package com.digitalsln.project6mSignage.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import java.util.*

/**
 * receiver to initialize scheduler for calling api
 */
class ApiCallSchedulerInitReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ScheduleApiTimerReceiverOne::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, 0)
        val futureDate: Calendar = Calendar.getInstance()
        Log.d("TvTimer","inside int :: ${futureDate.time}")
        futureDate.add(Calendar.MINUTE, 5)
        Log.d("TvTimer","${futureDate.time}")
        Toast.makeText(
            context,
            "starting api scheduler...",
            Toast.LENGTH_SHORT
        ).show()
        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
    }
}