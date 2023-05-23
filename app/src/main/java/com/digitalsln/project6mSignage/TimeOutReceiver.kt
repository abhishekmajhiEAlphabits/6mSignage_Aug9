package com.digitalsln.project6mSignage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

class TimeOutReceiver : BroadcastReceiver() {
    private val TAG = "6mSignage"
    override fun onReceive(context: Context, intent: Intent) {
        val value = intent.getStringExtra("timeOutValue")
        if ((context.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {

            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        } else {

            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                value
            )
        }
    }
}