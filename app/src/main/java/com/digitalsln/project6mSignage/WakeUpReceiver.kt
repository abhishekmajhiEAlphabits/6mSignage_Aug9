package com.digitalsln.project6mSignage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference

class WakeUpReceiver : BroadcastReceiver() {

    private val TAG = "6mSignage"
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager;

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("abhi", "inside wakeup receiver")

        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "appname::WakeLock"
        )

        wakeLock.acquire()
        wakeLock.release()
    }
}