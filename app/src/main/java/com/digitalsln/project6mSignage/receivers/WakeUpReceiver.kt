package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference

/**
 * receiver to wakeup screen on scheduled time
 */
class WakeUpReceiver : BroadcastReceiver() {
    private val TAG = "TvTimer"
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "inside wakeup receiver")

        if (!MainActivity.wakeLock.isHeld) {
            MainActivity.wakeLock.acquire()
        }
//        wakeLock.release()
    }
}