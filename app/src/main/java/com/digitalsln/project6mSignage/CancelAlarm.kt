package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.digitalsln.project6mSignage.network.ApiCall
import javax.inject.Singleton

@Singleton
class CancelAlarm(context: Context) {

    val context = context
    fun cancelAlarms(){
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, DisplayOverlayReceiver::class.java)
        i.action = "com.example.androidtvdemo.START_ALARM"
        val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        am.cancel(pi)

        getApiCall()

    }

    private fun getApiCall(){
        val call = ApiCall(context)
        call.callApi()
    }
}