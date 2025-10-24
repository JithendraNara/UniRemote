package com.example.uniremote.net

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> withMulticastLock(context: Context, block: suspend () -> T): T = withContext(Dispatchers.IO) {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    val lock = wifi?.createMulticastLock("uniremote-ssdp")
    lock?.setReferenceCounted(false)
    try {
        lock?.acquire()
        block()
    } finally {
        if (lock?.isHeld == true) lock.release()
    }
}
