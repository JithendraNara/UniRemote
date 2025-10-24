package com.example.uniremote.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.uniremote.MainActivity
import com.jithendranara.uniremote.R
import com.example.uniremote.data.readSettings
import com.example.uniremote.net.sendRoku
import com.example.uniremote.net.sendRokuVolumeDown
import com.example.uniremote.net.sendRokuVolumeUp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class FloatingAssistService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var panelView: View? = null
    private var panelParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var fadeRunnable: Runnable? = null
    private val fadeDelayMs = 2500L

    private val notifId = 4242
    private val channelId = "assist_overlay"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        // Clear any zombie views
        removeViews()
        
        ensureChannel()
        startForeground(
            notifId,
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Assistive dot is running")
                .setOngoing(true)
                .setContentIntent(PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_IMMUTABLE else 0)
                ))
                .build()
        )

        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        showBubble()
    }

    override fun onDestroy() {
        super.onDestroy()
        removeViews()
        serviceScope.cancel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Assist Overlay", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showBubble() {
        if (bubbleView != null) return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_bubble, null)
        val size = resources.getDimensionPixelSize(R.dimen.assist_bubble_size)
        val params = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 40
        params.y = 200

        // Drag handling with snap-to-edge, idle fade, and long-press to open app
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var initialPanelX = 0
        var initialPanelY = 0
        var isDragging = false
        val displayMetrics = resources.displayMetrics

        fun scheduleFade() {
            fadeRunnable?.let { view.removeCallbacks(it) }
            fadeRunnable = Runnable {
                bubbleView?.animate()?.alpha(0.5f)?.setDuration(250)?.start()
            }
            view.postDelayed(fadeRunnable!!, fadeDelayMs)
        }

        fun cancelFadeReset() {
            fadeRunnable?.let { view.removeCallbacks(it) }
            bubbleView?.animate()?.alpha(1f)?.setDuration(120)?.start()
        }

        fun snapToEdge() {
            val wm = windowManager
            val screenW = displayMetrics.widthPixels
            val targetX = if (params.x + params.width / 2 < screenW / 2) 0 else screenW - params.width
            val startX = params.x
            val dx = targetX - startX
            if (dx == 0) {
                scheduleFade()
                return
            }
            val animator = android.animation.ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 180
            animator.addUpdateListener {
                val f = it.animatedFraction
                params.x = startX + (dx * f).toInt()
                try { wm.updateViewLayout(view, params) } catch (_: Exception) {}
                panelParams?.let { p ->
                    p.x = params.x + params.width + 12
                    panelView?.let { v -> try { wm.updateViewLayout(v, p) } catch (_: Exception) {} }
                }
            }
            animator.start()
            scheduleFade()
        }

        var longPressHandler: Runnable? = null
        
        view.setOnTouchListener { v, event ->
            android.util.Log.d("FloatingAssist", "Touch event: ${event.action} at (${event.rawX}, ${event.rawY})")
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    android.util.Log.d("FloatingAssist", "ACTION_DOWN detected")
                    cancelFadeReset()
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    // Snapshot panel position at drag start (if open)
                    panelParams?.let { p ->
                        initialPanelX = p.x
                        initialPanelY = p.y
                    }
                    isDragging = false
                    
                    // Start long-press timer
                    longPressHandler = Runnable {
                        if (!isDragging) {
                            try {
                                val intent = Intent(this@FloatingAssistService, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    }
                    view.postDelayed(longPressHandler!!, android.view.ViewConfiguration.getLongPressTimeout().toLong())
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                        // Cancel long-press if we start dragging
                        longPressHandler?.let { view.removeCallbacks(it) }
                        
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager.updateViewLayout(view, params)
                        // Move panel along with bubble if it's open
                        panelParams?.let { p ->
                            p.x = initialPanelX + dx
                            p.y = initialPanelY + dy
                            panelView?.let { windowManager.updateViewLayout(it, p) }
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Cancel long-press timer
                    longPressHandler?.let { view.removeCallbacks(it) }
                    
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    
                    if (isDragging) {
                        snapToEdge()
                    } else if (dx < 10 && dy < 10) {
                        // It's a tap - toggle panel
                        togglePanel(params)
                    }
                    scheduleFade()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    // Cancel long-press on cancel event
                    longPressHandler?.let { view.removeCallbacks(it) }
                    if (isDragging) {
                        snapToEdge()
                    }
                    scheduleFade()
                    true
                }
                else -> false
            }
        }

        windowManager.addView(view, params)
        bubbleView = view
        // Initial fade schedule
        scheduleFade()
    }

    private fun togglePanel(anchorParams: WindowManager.LayoutParams) {
        if (panelView == null) showPanelNear(anchorParams) else removePanel()
    }

    private fun showPanelNear(anchorParams: WindowManager.LayoutParams) {
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_panel_radial, null) as android.widget.FrameLayout
        
        // Make panel compact to save screen space
        val panelSize = 280.dpToPx()
        val wrap = WindowManager.LayoutParams(
            panelSize,
            panelSize,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        wrap.gravity = Gravity.TOP or Gravity.START
        
        // Center the panel on the bubble
        val bubbleSize = resources.getDimensionPixelSize(R.dimen.assist_bubble_size)
        wrap.x = anchorParams.x - (panelSize - bubbleSize) / 2
        wrap.y = anchorParams.y - (panelSize - bubbleSize) / 2

        // Build a radial/arc menu around the panel center
        val screenW = resources.displayMetrics.widthPixels
        val bubbleCenterX = anchorParams.x + bubbleSize / 2
        val fromLeft = bubbleCenterX < screenW / 2
        val radius = 85.dpToPx()
        val centerX = panelSize / 2
        val centerY = panelSize / 2
        val angles = if (fromLeft) listOf(-60, -20, 20, 60, 100) else listOf(240, 200, 160, 120, 80)

        fun makeBtn(iconRes: Int, cd: String, color: Int, onClick: () -> Unit): android.widget.ImageView {
            val iv = android.widget.ImageView(this).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(52.dpToPx(), 52.dpToPx())
                setImageResource(iconRes)
                contentDescription = cd
                scaleType = android.widget.ImageView.ScaleType.CENTER
                setPadding(10.dpToPx(), 10.dpToPx(), 10.dpToPx(), 10.dpToPx())
                elevation = 6.dpToPx().toFloat()
                
                // Create circular gradient background programmatically
                val gd = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    colors = intArrayOf(color, adjustBrightness(color, 0.7f))
                    gradientType = android.graphics.drawable.GradientDrawable.RADIAL_GRADIENT
                    gradientRadius = 26.dpToPx().toFloat()
                }
                background = gd
                
                alpha = 0f
                scaleX = 0.3f
                scaleY = 0.3f
                // Don't auto-close panel on button click - let user tap outside to dismiss
                setOnClickListener { onClick() }
            }
            view.addView(iv)
            return iv
        }

        val btns = listOf(
            makeBtn(R.drawable.ic_volume_up, "Volume up", 0xFF4CAF50.toInt()) { performTvVolume(VolumeAction.UP) },
            makeBtn(R.drawable.ic_volume_mute, "Mute toggle", 0xFFFF9800.toInt()) { performTvVolume(VolumeAction.MUTE_TOGGLE) },
            makeBtn(R.drawable.ic_volume_down, "Volume down", 0xFF2196F3.toInt()) { performTvVolume(VolumeAction.DOWN) },
            makeBtn(R.drawable.ic_power, "Power", 0xFFF44336.toInt()) { serviceScope.launch { performPower() } },
            makeBtn(R.drawable.ic_open_app, "Open app", 0xFF9C27B0.toInt()) {
                try { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        )

        // Position buttons in a radial arc with smooth fan-out animation
        btns.forEachIndexed { i, btn ->
            val angle = Math.toRadians(angles[i].toDouble())
            val btnSize = (btn.layoutParams as FrameLayout.LayoutParams).width
            val finalX = (centerX + radius * Math.cos(angle)).toInt() - btnSize / 2
            val finalY = (centerY + radius * Math.sin(angle)).toInt() - btnSize / 2
            
            // Start from center
            btn.x = (centerX - btnSize / 2).toFloat()
            btn.y = (centerY - btnSize / 2).toFloat()
            
            // Smooth bounce animation
            btn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .x(finalX.toFloat())
                .y(finalY.toFloat())
                .setDuration(300)
                .setStartDelay((i * 40).toLong())
                .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
                .start()
        }

        // Dismiss when touched outside
        view.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_OUTSIDE) {
                removePanel()
                true
            } else {
                false
            }
        }

        windowManager.addView(view, wrap)
        panelView = view
        panelParams = wrap

        // Animate panel reveal
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.alpha = 0f
        view.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(160).start()
        // Ensure bubble is fully visible while panel is open
        bubbleView?.animate()?.alpha(1f)?.setDuration(120)?.start()
    }

    private fun removePanel() {
        panelView?.let { v ->
            try {
                v.animate().alpha(0f).scaleX(0.9f).scaleY(0.9f).setDuration(120).withEndAction {
                    try { windowManager.removeView(v) } catch (_: Exception) {}
                }.start()
            } catch (_: Exception) {
                try { windowManager.removeView(v) } catch (_: Exception) {}
            }
        }
        panelView = null
        panelParams = null
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).roundToInt()

    private suspend fun performPower() {
        // Fire off Roku power toggle (uses the same endpoint as main UI for PowerOff)
        val ip = try { applicationContext.readSettings().rokuIp } catch (_: Exception) { "" }
        if (ip.isBlank()) {
            Toast.makeText(this, "Set Roku IP in Settings", Toast.LENGTH_SHORT).show()
            return
        }
        val result = com.example.uniremote.net.sendRoku(ip, "PowerOff")
        result.onFailure {
            Toast.makeText(this, it.message ?: "Roku didn't respond", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeViews() {
        removePanel()
        bubbleView?.let { windowManager.removeView(it) }
        bubbleView = null
    }

    private enum class VolumeAction { UP, DOWN, MUTE_TOGGLE }

    private fun performTvVolume(action: VolumeAction) {
        serviceScope.launch {
            val ip = try {
                applicationContext.readSettings().rokuIp
            } catch (_: Exception) {
                ""
            }
            if (ip.isBlank()) {
                Toast.makeText(
                    this@FloatingAssistService,
                    "Set Roku IP in Settings to control TV volume",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            val result = when (action) {
                VolumeAction.UP -> sendRokuVolumeUp(ip)
                VolumeAction.DOWN -> sendRokuVolumeDown(ip)
                VolumeAction.MUTE_TOGGLE -> sendRoku(ip, "VolumeMute")
            }
            result.onFailure {
                Toast.makeText(
                    this@FloatingAssistService,
                    it.message ?: "Roku didn't respond",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    
    private fun adjustBrightness(color: Int, factor: Float): Int {
        val a = (color shr 24) and 0xff
        val r = ((color shr 16) and 0xff) * factor
        val g = ((color shr 8) and 0xff) * factor
        val b = (color and 0xff) * factor
        return (a shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()
    }
    
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).roundToInt()
}
