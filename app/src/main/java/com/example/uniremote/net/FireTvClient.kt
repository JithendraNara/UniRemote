package com.example.uniremote.net

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
// import removed: readSettings was unused

/**
 * Thin wrapper for Amazon Fling SDK with graceful fallback when the SDK JAR
 * is not present at compile/runtime. Uses reflection to avoid a hard dependency
 * so the app can still build without libs/amazon-fling.jar.
 */
class FireTvClient(private val context: Context) {
    enum class Playback { Idle, Playing, Paused }

    private val _playback = kotlinx.coroutines.flow.MutableStateFlow(Playback.Idle)
    val playback: kotlinx.coroutines.flow.StateFlow<Playback> = _playback

    // Whether the reflective Fling implementation is available at runtime
    val isAvailable: Boolean get() = impl !is NoopImpl
    private val impl: IFireTvClient = try {
        // If DiscoveryController class exists, use the real (reflective) implementation
        Class.forName("com.amazon.whisperplay.fling.media.controller.DiscoveryController")
        FlingImpl(context)
    } catch (t: Throwable) {
        Log.w("FireTvClient", "Fling SDK not found; using Noop implementation: ${t.message}")
        NoopImpl
    }

    fun startDiscovery(sid: String? = null, onFound: (Receiver) -> Unit, onLost: (Receiver) -> Unit) =
        impl.startDiscovery(sid, onFound, onLost)

    fun stopDiscovery() = impl.stopDiscovery()

    fun connect(receiver: Receiver) {
        impl.connect(receiver)
        _playback.value = Playback.Idle
    }

    suspend fun play(url: String) {
        impl.play(url)
        _playback.value = Playback.Playing
    }

    suspend fun pause() {
        impl.pause()
        _playback.value = Playback.Paused
    }

    suspend fun stop() {
        impl.stop()
        _playback.value = Playback.Idle
    }

    /** Represents a Fire TV receiver discovered on the LAN. */
    data class Receiver(val id: String, val friendlyName: String, val ip: String)
}

private interface IFireTvClient {
    // Note: avoid default args in interface to prevent edge-case override mismatches on some Kotlin toolchains
    fun startDiscovery(sid: String?, onFound: (FireTvClient.Receiver) -> Unit, onLost: (FireTvClient.Receiver) -> Unit)
    fun stopDiscovery()
    fun connect(receiver: FireTvClient.Receiver)
    suspend fun play(url: String)
    suspend fun pause()
    suspend fun stop()
}

/** No-op impl when Fling SDK is absent. */
private object NoopImpl : IFireTvClient {
    override fun startDiscovery(sid: String?, onFound: (FireTvClient.Receiver) -> Unit, onLost: (FireTvClient.Receiver) -> Unit) { /* no-op */ }
    override fun stopDiscovery() { /* no-op */ }
    override fun connect(receiver: FireTvClient.Receiver) { /* no-op */ }
    override suspend fun play(url: String) { /* no-op */ }
    override suspend fun pause() { /* no-op */ }
    override suspend fun stop() { /* no-op */ }
}

/**
 * Reflective implementation. This purposefully avoids compile-time references
 * to the Fling SDK types so that the app builds even if the jar is missing.
 *
 * Notes:
 * - Navigation (Home/Back/D-Pad) is not supported by the Fling media controller;
 *   those key events typically require ADB. This client focuses on media control
 *   (play URL, pause, stop) once connected to a receiver.
 */
private class FlingImpl(private val context: Context) : IFireTvClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Cached reflective members (Amazon Fling SDK)
    private val controllerClass = Class.forName("com.amazon.whisperplay.fling.media.controller.DiscoveryController")
    private val listenerClass = Class.forName("com.amazon.whisperplay.fling.media.controller.DiscoveryController\$IDiscoveryListener")
    private val rmpClass = Class.forName("com.amazon.whisperplay.fling.media.controller.RemoteMediaPlayer")
    // Optional install discovery fallback
    private val installControllerClass = try { Class.forName("com.amazon.whisperplay.install.InstallDiscoveryController") } catch (_: Throwable) { null }
    private val installListenerClass = try { Class.forName("com.amazon.whisperplay.install.InstallDiscoveryController\$IInstallDiscoveryListener") } catch (_: Throwable) { null }

    private var controller: Any? = null
    private var currentPlayer: Any? = null
    private val playersById = mutableMapOf<String, Any>()
    private var multicastLock: WifiManager.MulticastLock? = null
    // MediaRouter fallback members
    private var mediaRouter: Any? = null
    private var mediaRouterCallback: Any? = null
    private var mediaRouteSelector: Any? = null
    private var mediaRouteProvider: Any? = null
    private var mediaRouterClasses: MediaRouterReflect? = null

    @Volatile private var connectedId: String? = null

    override fun startDiscovery(sid: String?, onFound: (FireTvClient.Receiver) -> Unit, onLost: (FireTvClient.Receiver) -> Unit) {
        try {
            // Acquire multicast lock for SSDP/MDNS traffic during discovery
            try {
                val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                multicastLock = wm?.createMulticastLock("UniRemote-Fling").apply {
                    this?.setReferenceCounted(false)
                    this?.acquire()
                }
                Log.d("FireTvClient", "Multicast lock acquired for Fling discovery")
            } catch (e: Throwable) {
                Log.w("FireTvClient", "Could not acquire multicast lock: ${e.message}")
            }
            Log.d("FireTvClient", "Initializing Fling discovery... (SDK classes resolved)")
            // controller = DiscoveryController(context)
            val ctor = controllerClass.getConstructor(Context::class.java)
            controller = ctor.newInstance(context)
            Log.d("FireTvClient", "DiscoveryController instantiated")
            playersById.clear()
            currentPlayer = null

            // Debug: log listener method signatures once
            try {
                val methods = listenerClass.methods.joinToString { it.name }
                Log.d("FireTvClient", "IDiscoveryListener methods: $methods")
            } catch (_: Throwable) {}

            val handler = java.lang.reflect.InvocationHandler { _, method, args ->
                try {
                    val name = method?.name ?: ""
                    val player = args?.getOrNull(0)
                    when {
                        name.contains("Discovered", ignoreCase = true) -> {
                            val rx = toReceiverFromPlayer(player)
                            if (rx != null) {
                                playersById[rx.id] = player!!
                                onFound(rx)
                            }
                        }
                        name.contains("Lost", ignoreCase = true) || name.contains("Removed", ignoreCase = true) -> {
                            val id = getPlayerId(player)
                            if (id != null) {
                                val rx = FireTvClient.Receiver(id, getPlayerName(player) ?: "Fire TV", ip = "")
                                playersById.remove(id)
                                onLost(rx)
                            }
                        }
                        else -> {
                            // Log once in debug builds if unknown callback occurs
                            Log.d("FireTvClient", "Discovery callback: $name")
                        }
                    }
                } catch (e: Throwable) {
                    Log.e("FireTvClient", "Discovery listener error: ${e.message}")
                }
                null
            }
            val listener = java.lang.reflect.Proxy.newProxyInstance(
                listenerClass.classLoader, arrayOf(listenerClass), handler
            )

            // Per Amazon docs: start requires a Service ID (SID) matching the receiver.
            val startWithSid = controllerClass.methods.firstOrNull { it.name == "start" && it.parameterTypes.size == 2 }
            val desiredSid = (sid ?: "").ifBlank {
                // Prefer the SDK's default player service id if available; else use known fallback
                try {
                    val providerClass = Class.forName("com.amazon.whisperplay.fling.provider.FlingMediaRouteProvider")
                    val field = providerClass.getDeclaredField("DEFAULT_PLAYER_SERVICE_ID")
                    field.isAccessible = true
                    (field.get(null) as? String) ?: "amzn.thin.pl"
                } catch (_: Throwable) {
                    "amzn.thin.pl"
                }
            }
            var started = false
            try {
                Log.d("FireTvClient", "Starting discovery via start(\"$desiredSid\", listener)")
                startWithSid?.invoke(controller, desiredSid, listener)
                started = true
                Log.d("FireTvClient", "DiscoveryController.start succeeded with SID=$desiredSid")
            } catch (e: Throwable) {
                Log.w("FireTvClient", "start($desiredSid, listener) failed: ${e.message}")
            }
            if (!started) {
                // Conservative fallbacks in case OEM builds require different SID strings
                val fallbacks = listOf("com.amazon.whisperplay.fling.media", "com.amazon.whisperplay", "*", "")
                for (fb in fallbacks) {
                    try {
                        Log.d("FireTvClient", "Retry discovery via start(\"$fb\", listener)")
                        startWithSid?.invoke(controller, fb, listener)
                        started = true
                        Log.d("FireTvClient", "DiscoveryController.start succeeded with fallback SID=$fb")
                        break
                    } catch (e: Throwable) {
                        Log.w("FireTvClient", "start($fb, listener) failed: ${e.message}")
                    }
                }
            }
            if (!started) {
                // Last resort: try a no-arg overload if present (older SDKs)
                val startNoSid = controllerClass.methods.firstOrNull { it.name == "start" && it.parameterTypes.size == 1 }
                if (startNoSid != null) {
                    try {
                        Log.d("FireTvClient", "Fallback: start(listener)")
                        startNoSid.invoke(controller, listener)
                        started = true
                        Log.d("FireTvClient", "DiscoveryController.start(listener) succeeded")
                    } catch (e: Throwable) {
                        Log.w("FireTvClient", "start(listener) failed: ${e.message}")
                    }
                }
            }
            if (!started) Log.e("FireTvClient", "DiscoveryController.start could not be invoked with any signature")

            // MediaRouter fallback discovery (routes) — helpful for built-in receiver
            try {
                mediaRouterClasses = MediaRouterReflect.load()
                mediaRouterClasses?.let { M ->
                    val getInstance = M.routerClass.getMethod("getInstance", Context::class.java)
                    mediaRouter = getInstance.invoke(null, context)

                    // Instantiate FlingMediaRouteProvider(context, sid)
                    val providerClass = Class.forName("com.amazon.whisperplay.fling.provider.FlingMediaRouteProvider")
                    val providerCtor = try { providerClass.getConstructor(Context::class.java, String::class.java) } catch (_: Throwable) { null }
                    mediaRouteProvider = if (providerCtor != null) providerCtor.newInstance(context, desiredSid) else null

                    // router.addProvider(provider)
                    if (mediaRouteProvider != null) {
                        val mediaRouteProviderBase = try { Class.forName("androidx.mediarouter.media.MediaRouteProvider") } catch (_: Throwable) { null }
                        val addProvider = if (mediaRouteProviderBase != null)
                            M.routerClass.getMethod("addProvider", mediaRouteProviderBase)
                        else
                            M.routerClass.methods.firstOrNull { it.name == "addProvider" }
                        addProvider?.invoke(mediaRouter, mediaRouteProvider)
                    }

                    // Build selector: CATEGORY_REMOTE_PLAYBACK
                    val builder = M.selectorBuilderCtor.newInstance()
                    val cat = M.categoryRemotePlayback
                    M.selectorAddCategory.invoke(builder, cat)
                    mediaRouteSelector = M.selectorBuild.invoke(builder)

                    // Create callback proxy
                    val cbHandler = java.lang.reflect.InvocationHandler { _, method, args ->
                        try {
                            when (method?.name) {
                                "onRouteAdded" -> {
                                    val route = args?.getOrNull(1)
                                    val rx = toReceiverFromRoute(route, M)
                                    if (rx != null) onFound(rx)
                                }
                                "onRouteRemoved" -> {
                                    val route = args?.getOrNull(1)
                                    val id = M.routeGetId.invoke(route) as? String
                                    val name = (M.routeGetName.invoke(route) as? CharSequence)?.toString() ?: "Fire TV"
                                    if (id != null) onLost(FireTvClient.Receiver(id, name, ""))
                                }
                            }
                        } catch (t: Throwable) {
                            Log.w("FireTvClient", "MediaRouter callback error: ${t.message}")
                        }
                        null
                    }
                    mediaRouterCallback = java.lang.reflect.Proxy.newProxyInstance(
                        M.callbackClass.classLoader, arrayOf(M.callbackClass), cbHandler
                    )

                    // router.addCallback(selector, callback, flags)
                    val addCallback = M.routerClass.getMethod("addCallback", M.selectorClass, M.callbackClass, Int::class.javaPrimitiveType)
                    addCallback.invoke(mediaRouter, mediaRouteSelector, mediaRouterCallback, M.flagPerformActiveScan)

                    Log.d("FireTvClient", "MediaRouter discovery started with SID=$desiredSid")
                }
            } catch (mr: Throwable) {
                Log.w("FireTvClient", "MediaRouter fallback unavailable: ${mr.message}")
            }

            // Start optional Install discovery fallback in parallel
            try {
                if (installControllerClass != null && installListenerClass != null) {
                    val installCtor = installControllerClass.getConstructor(Context::class.java)
                    val installController = installCtor.newInstance(context)
                    val installHandler = java.lang.reflect.InvocationHandler { _, m, a ->
                        try {
                            val methodName = m?.name ?: ""
                            val obj = a?.getOrNull(0)
                            when {
                                methodName.contains("Discovered", true) || methodName.contains("Found", true) -> {
                                    val rx = toReceiverFromGeneric(obj)
                                    if (rx != null && !playersById.containsKey(rx.id)) {
                                        // We don't have a RemoteMediaPlayer here, but still surface a candidate device for the user
                                        onFound(rx)
                                    }
                                }
                                methodName.contains("Lost", true) || methodName.contains("Removed", true) -> {
                                    val id = extractId(obj)
                                    if (id != null) {
                                        onLost(FireTvClient.Receiver(id, extractName(obj) ?: "Fire TV", ip = extractIp(obj) ?: ""))
                                    }
                                }
                                methodName.contains("Failure", true) -> {
                                    Log.w("FireTvClient", "Install discovery reported failure")
                                }
                            }
                        } catch (e: Throwable) {
                            Log.w("FireTvClient", "Install discovery listener error: ${e.message}")
                        }
                        null
                    }
                    val installListener = java.lang.reflect.Proxy.newProxyInstance(
                        installListenerClass.classLoader, arrayOf(installListenerClass), installHandler
                    )
                    val startInstall = installControllerClass.methods.firstOrNull { it.name == "start" && it.parameterTypes.size == 1 }
                    startInstall?.invoke(installController, installListener)
                    Log.d("FireTvClient", "Install discovery fallback started")
                }
            } catch (e: Throwable) {
                Log.w("FireTvClient", "Install discovery not available: ${e.message}")
            }
        } catch (e: Throwable) {
            Log.e("FireTvClient", "startDiscovery failed: ${e.message}")
        }
    }

    override fun stopDiscovery() {
        try {
            val stopMethod = controllerClass.methods.firstOrNull { it.name.equals("stop", true) }
            if (controller != null && stopMethod != null) {
                stopMethod.invoke(controller)
                Log.d("FireTvClient", "DiscoveryController.stop invoked")
            } else {
                Log.d("FireTvClient", "DiscoveryController.stop skipped (controller not initialized)")
            }
        } catch (e: Throwable) {
            Log.e("FireTvClient", "stopDiscovery failed: ${e.message}")
        }
        // Tear down MediaRouter callback/provider
        try {
            mediaRouterClasses?.let { M ->
                if (mediaRouter != null && mediaRouterCallback != null) {
                    val removeCallback = M.routerClass.getMethod("removeCallback", M.callbackClass)
                    removeCallback.invoke(mediaRouter, mediaRouterCallback)
                }
                if (mediaRouter != null && mediaRouteProvider != null) {
                    val mediaRouteProviderBase = try { Class.forName("androidx.mediarouter.media.MediaRouteProvider") } catch (_: Throwable) { null }
                    val removeProvider = if (mediaRouteProviderBase != null)
                        M.routerClass.getMethod("removeProvider", mediaRouteProviderBase)
                    else
                        M.routerClass.methods.firstOrNull { it.name == "removeProvider" }
                    removeProvider?.invoke(mediaRouter, mediaRouteProvider)
                }
            }
        } catch (t: Throwable) {
            Log.w("FireTvClient", "MediaRouter teardown failed: ${t.message}")
        } finally {
            mediaRouter = null
            mediaRouterCallback = null
            mediaRouteSelector = null
            mediaRouteProvider = null
        }
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
            Log.d("FireTvClient", "Multicast lock released")
        } catch (_: Throwable) {}
    }

    override fun connect(receiver: FireTvClient.Receiver) {
        try {
            currentPlayer = playersById[receiver.id]
            connectedId = receiver.id
        } catch (e: Throwable) {
            Log.e("FireTvClient", "connect failed: ${e.message}")
        }
    }

    override suspend fun play(url: String) {
        withContext(Dispatchers.IO) {
            try {
                val play = rmpClass.methods.firstOrNull { it.name == "play" && it.parameterTypes.isEmpty() }
                if (url.isNotBlank()) {
                    val setMediaSource = rmpClass.methods.firstOrNull { it.name == "setMediaSource" && it.parameterTypes.size == 4 }
                    setMediaSource?.invoke(currentPlayer, url, "UniRemote", true, false)
                }
                play?.invoke(currentPlayer)
            } catch (e: Throwable) {
                Log.e("FireTvClient", "play failed: ${e.message}")
            }
        }
    }

    override suspend fun pause() {
        withContext(Dispatchers.IO) {
            try {
                val pauseMethod = rmpClass.methods.firstOrNull { it.name.equals("pause", true) }
                pauseMethod?.invoke(currentPlayer)
            } catch (e: Throwable) {
                Log.e("FireTvClient", "pause failed: ${e.message}")
            }
        }
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            try {
                val stopMethod = rmpClass.methods.firstOrNull { it.name.equals("stop", true) }
                stopMethod?.invoke(currentPlayer)
            } catch (e: Throwable) {
                Log.e("FireTvClient", "stop failed: ${e.message}")
            }
        }
    }

    private fun toReceiverFromPlayer(player: Any?): FireTvClient.Receiver? {
        if (player == null) return null
        return try {
            val id = getPlayerId(player) ?: return null
            val name = getPlayerName(player) ?: "Fire TV"
            FireTvClient.Receiver(id = id, friendlyName = name, ip = "")
        } catch (e: Throwable) {
            Log.w("FireTvClient", "toReceiverFromPlayer failed: ${e.message}")
            null
        }
    }

    private fun getPlayerId(player: Any?): String? {
        return try {
            rmpClass.methods.firstOrNull { it.name == "getUniqueIdentifier" }?.invoke(player) as? String
        } catch (e: Throwable) {
            null
        }
    }

    private fun getPlayerName(player: Any?): String? {
        return try {
            rmpClass.methods.firstOrNull { it.name == "getName" }?.invoke(player) as? String
        } catch (e: Throwable) {
            null
        }
    }

    // Generic helpers for Install discovery objects (best-effort by reflection)
    private fun extractId(o: Any?): String? {
        return try {
            if (o == null) null else {
                val m = o::class.java.methods.firstOrNull {
                    it.name.equals("getUniqueIdentifier", true) ||
                    it.name.equals("getUniqueId", true) ||
                    it.name.equals("getId", true)
                }
                m?.invoke(o) as? String
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractName(o: Any?): String? {
        return try {
            if (o == null) null else {
                val m = o::class.java.methods.firstOrNull {
                    it.name.equals("getName", true) || it.name.equals("getFriendlyName", true)
                }
                (m?.invoke(o) as? CharSequence)?.toString()
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun extractIp(o: Any?): String? {
        return try {
            if (o == null) null else {
                val m = o::class.java.methods.firstOrNull {
                    it.name.contains("Ip", true) || it.name.contains("Address", true) || it.name.contains("Host", true)
                }
                val v = m?.invoke(o)
                when (v) {
                    is String -> v
                    else -> v?.toString()
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun toReceiverFromGeneric(o: Any?): FireTvClient.Receiver? {
        val id = extractId(o) ?: return null
        val name = extractName(o) ?: "Fire TV"
        val ip = extractIp(o) ?: ""
        return FireTvClient.Receiver(id, name, ip)
    }

    private fun toReceiverFromRoute(route: Any?, M: MediaRouterReflect): FireTvClient.Receiver? {
        return try {
            val id = M.routeGetId.invoke(route) as? String ?: return null
            val name = (M.routeGetName.invoke(route) as? CharSequence)?.toString() ?: "Fire TV"
            FireTvClient.Receiver(id, name, "")
        } catch (_: Throwable) { null }
    }
}

// Reflection helper for AndroidX MediaRouter
private class MediaRouterReflect(
    val routerClass: Class<*>,
    val selectorClass: Class<*>,
    val callbackClass: Class<*>,
    val routeInfoClass: Class<*>,
    val selectorBuilderCtor: java.lang.reflect.Constructor<*>,
    val selectorAddCategory: java.lang.reflect.Method,
    val selectorBuild: java.lang.reflect.Method,
    val categoryRemotePlayback: String,
    val routeGetId: java.lang.reflect.Method,
    val routeGetName: java.lang.reflect.Method,
    val flagPerformActiveScan: Int
) {
    companion object {
        fun load(): MediaRouterReflect? = try {
            val router = Class.forName("androidx.mediarouter.media.MediaRouter")
            val selector = Class.forName("androidx.mediarouter.media.MediaRouteSelector")
            val callback = Class.forName("androidx.mediarouter.media.MediaRouter\$Callback")
            val routeInfo = Class.forName("androidx.mediarouter.media.MediaRouter\$RouteInfo")
            val builder = Class.forName("androidx.mediarouter.media.MediaRouteSelector\$Builder").getConstructor()
            val addCat = Class.forName("androidx.mediarouter.media.MediaRouteSelector\$Builder").getMethod("addControlCategory", String::class.java)
            val build = Class.forName("androidx.mediarouter.media.MediaRouteSelector\$Builder").getMethod("build")
            val mci = Class.forName("androidx.mediarouter.media.MediaControlIntent")
            val cat = mci.getField("CATEGORY_REMOTE_PLAYBACK").get(null) as String
            val routeGetId = routeInfo.getMethod("getId")
            val routeGetName = routeInfo.getMethod("getName")
            val flag = router.getField("CALLBACK_FLAG_PERFORM_ACTIVE_SCAN").getInt(null)
            MediaRouterReflect(router, selector, callback, routeInfo, builder, addCat, build, cat, routeGetId, routeGetName, flag)
        } catch (_: Throwable) {
            null
        }
    }
}
