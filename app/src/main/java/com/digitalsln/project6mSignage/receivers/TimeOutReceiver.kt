package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants

/**
 * receiver to set default time out value in native settings
 */
class TimeOutReceiver : BroadcastReceiver() {
    private val TAG = "TvTimer"
    override fun onReceive(context: Context, intent: Intent) {
        val value = AppPreference(context).retrieveDefaultTimeOut(Constants.timeOut,Constants.defaultTimeOut)
        if ((context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
            Log.d(TAG, "inside timeout receiver is interactive:$value")
            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        } else {
            Log.d(TAG, "inside timeout receiver NOT interactive :$value")
            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        }
    }
}