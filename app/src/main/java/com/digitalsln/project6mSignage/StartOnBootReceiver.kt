package com.digitalsln.project6mSignage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class StartOnBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Consts.isAppStartedFromBroadcast = true
            Thread.sleep(5000)
            val activityIntent = Intent(context, MainActivity::class.java)
            activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(activityIntent)
        }
    }

    companion object {
        private const val TAG = "StartOnBootReceiver"
    }
}