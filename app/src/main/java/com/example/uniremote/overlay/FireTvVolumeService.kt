package com.example.uniremote.overlay

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.*
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class FireTvVolumeService : Service() {
    private lateinit var windowManager: WindowManager
    private var handleView: View? = null
    private var volumeView: View? = null
    private var isExpanded = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collapseJob: kotlinx.coroutines.Job? = null
    
    private val notifId = 4243
    private val channelId = "firetv_volume_overlay"
    
    companion object {
        private const val FIRETV_PACKAGE = "com.amazon.storm.lightning.client.aosp"
        private var isRunning = false
        
        fun start(context: Context) {
            if (!isRunning) {
                val intent = Intent(context, FireTvVolumeService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, FireTvVolumeService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        ensureChannel()
        startForeground(
            notifId,
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Fire TV Volume Control")
                .setContentText("Volume controls are visible. Tap to open app.")
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
        
        // Show the collapsed handle initially
        showHandle()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        removeHandle()
        removeVolumePanel()
        serviceScope.cancel()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Fire TV Volume Overlay", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannel(channel)
        }
    }

    private fun showHandle() {
        if (handleView != null) return
        
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_firetv_handle, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        // Position on the right edge
        params.gravity = Gravity.TOP or Gravity.END
        params.x = 0
        params.y = 360
        
        // Make it draggable and expandable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(initialTouchX - event.rawX)
                    val deltaY = Math.abs(initialTouchY - event.rawY)
                    if (deltaX > 10 || deltaY > 10) {
                        isDragging = true
                        params.x = initialX + (initialTouchX - event.rawX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tap to expand
                        expandPanel()
                    }
                    true
                }
                else -> false
            }
        }
        
        windowManager.addView(view, params)
        handleView = view
    }

    private fun removeHandle() {
        handleView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            handleView = null
        }
    }

    private fun expandPanel() {
        if (isExpanded) return
        isExpanded = true
        
        // Animate the handle fading out
        handleView?.animate()
            ?.alpha(0f)
            ?.setDuration(150)
            ?.withEndAction {
                handleView?.visibility = View.GONE
                handleView?.alpha = 1f
            }
            ?.start()
        
        // Show the volume panel with animation
        showVolumePanel()
    }

    private fun collapsePanel() {
        if (!isExpanded) return
        isExpanded = false
        
        // Cancel auto-collapse
        collapseJob?.cancel()
        
        // Animate volume panel fading out
        volumeView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(200)
            ?.withEndAction {
                removeVolumePanel()
            }
            ?.start()
        
        // Show the handle again with animation
        handleView?.apply {
            visibility = View.VISIBLE
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        }
    }

    private fun scheduleAutoCollapse() {
        collapseJob?.cancel()
        collapseJob = serviceScope.launch {
            delay(5000) // Auto-collapse after 5 seconds
            collapsePanel()
        }
    }

    private fun showVolumePanel() {
        if (volumeView != null) return
        
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.overlay_firetv_volume, null)
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        // Position at same location as handle
        handleView?.let { handle ->
            val handleParams = handle.layoutParams as WindowManager.LayoutParams
            params.gravity = handleParams.gravity
            params.x = handleParams.x
            params.y = handleParams.y
        }
        
        // Detect tap outside to collapse
        view.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                collapsePanel()
                true
            } else {
                // Reset auto-collapse timer on interaction
                scheduleAutoCollapse()
                false
            }
        }
        
        // Setup button click listeners
        view.findViewById<View>(R.id.btn_vol_up).setOnClickListener {
            performTvVolume(VolumeAction.UP)
            scheduleAutoCollapse() // Reset timer
        }
        view.findViewById<View>(R.id.btn_vol_down).setOnClickListener {
            performTvVolume(VolumeAction.DOWN)
            scheduleAutoCollapse() // Reset timer
        }
        view.findViewById<View>(R.id.btn_vol_mute).setOnClickListener {
            performTvVolume(VolumeAction.MUTE_TOGGLE)
            scheduleAutoCollapse() // Reset timer
        }
        
        windowManager.addView(view, params)
        volumeView = view
        
        // Animate panel appearing
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f
        view.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(200)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.2f))
            .withEndAction {
                // Start auto-collapse timer after animation
                scheduleAutoCollapse()
            }
            .start()
    }

    private fun removeVolumePanel() {
        volumeView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
            volumeView = null
        }
    }

    private enum class VolumeAction { UP, DOWN, MUTE_TOGGLE }

    private fun performTvVolume(action: VolumeAction) {
        serviceScope.launch(Dispatchers.IO) {
            val ip = this@FireTvVolumeService.readSettings().rokuIp
            if (ip.isBlank()) {
                Toast.makeText(
                    this@FireTvVolumeService,
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
                    this@FireTvVolumeService,
                    it.message ?: "Roku didn't respond",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
