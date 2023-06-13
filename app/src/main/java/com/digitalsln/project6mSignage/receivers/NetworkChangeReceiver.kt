package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast

class NetworkChangeReceiver : BroadcastReceiver() {
    private var TAG= "TvTimer"
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (isOnline(context)) {
                Toast.makeText(context, "Internet Connected", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Online Connected Internet ")
            } else {
                Toast.makeText(context, "Internet Connection Lost", Toast.LENGTH_LONG).show()
                Log.d(TAG, "Connectivity Failure !!! ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isOnline(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo = cm.activeNetworkInfo;
            //should check null because in airplane mode it will be null
            return (netInfo != null && netInfo.isConnected())
        } catch (e: Exception) {
            e.printStackTrace()
            return false;
        }
    }
}