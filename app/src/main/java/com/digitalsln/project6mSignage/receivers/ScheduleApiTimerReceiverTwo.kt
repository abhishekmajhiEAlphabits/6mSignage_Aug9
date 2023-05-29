package com.digitalsln.project6mSignage.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.network.ApiCall
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import java.util.*

class ScheduleApiTimerReceiverTwo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TvTimer", "inside schedule api receiver 2")

        val call = ApiCall(context)
        call.callApi()

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ScheduleApiTimerReceiverOne::class.java)
        i.action = "com.example.androidtvdemo.START_ALARM"
        val pi = PendingIntent.getBroadcast(context, 0, i, 0);

        val futureDate: Calendar = Calendar.getInstance()
        futureDate.add(Calendar.HOUR_OF_DAY, 24)

        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
    }

}