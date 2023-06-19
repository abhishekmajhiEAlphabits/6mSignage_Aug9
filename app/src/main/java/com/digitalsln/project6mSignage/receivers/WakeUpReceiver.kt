package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.appUtils.AppLogger
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import java.text.SimpleDateFormat
import java.util.*

/**
 * receiver to wakeup screen on scheduled time
 */
class WakeUpReceiver : BroadcastReceiver() {
    private val TAG = "TvTimer"
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager
    private lateinit var appLogger: AppLogger

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "inside wakeup receiver")
        appLogger = AppLogger()

        powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE, "appname::WakeLock"
        )
        wakeLock.acquire()

        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        var logTime = sdf.format(cal.time)
        var log = "$logTime screen was successfully turned on"
        appLogger.appendLog(log)
        Log.d(TAG, "$log")
        Log.d(TAG, "now acquire command")
    }
}