package com.digitalsln.project6mSignage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast

class DisplayOverlayReceiver : BroadcastReceiver() {
    private val TAG = "6mSignage"
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d("TvTimer","inside overlay ")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // check if the user has already granted
                // the Draw over other apps permission
                if (Settings.canDrawOverlays(context)) {
                    // start the service based on the android version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(
                            Intent(
                                context,
                                ForegroundService::class.java
                            )
                        )
                    } else {
                        context.startService(Intent(context, ForegroundService::class.java))
                    }
                }
            } else {
                context.startService(Intent(context, ForegroundService::class.java))
            }


        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Modify System Settings permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}