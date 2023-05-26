package com.digitalsln.project6mSignage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference

class TimeOutReceiver : BroadcastReceiver() {
    private val TAG = "6mSignage"
    override fun onReceive(context: Context, intent: Intent) {
        val value = AppPreference(context).retrieveDefaultTimeOut("TIME_OUT","300000")
        if ((context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
            Log.d("abhi", "inside timeout receiver interactive :$value")
            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        } else {
            Log.d("abhi", "inside timeout receiver NOT interactive :$value")
            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        }
    }
}