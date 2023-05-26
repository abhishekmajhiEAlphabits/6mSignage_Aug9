package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitalsln.project6mSignage.network.ApiCall
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import java.util.*

class ScheduleApiTimerReceiverTwo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("abhi", "inside schedule api receiver 2")

        val call = ApiCall(context)
        call.callApi()

        val toTime = AppPreference(context).retrieveFromTime("TO_TIME","TO_TIME")
        Log.d("abhi", "$toTime")

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ScheduleApiTimerReceiverOne::class.java)
        i.action = "com.example.androidtvdemo.START_ALARM"
        val pi = PendingIntent.getBroadcast(context, 0, i, 0);

        val futureDate: Calendar = Calendar.getInstance()
//        futureDate.add(Calendar.SECOND, 10)
//        futureDate.set(Calendar.HOUR_OF_DAY, 17)
//        futureDate.set(Calendar.MINUTE, 58)
        futureDate.add(Calendar.SECOND, 5)

//        val ac = AlarmClockInfo(
//            futureDate.timeInMillis,
//            pi
//        )
//        am.setAlarmClock(ac, pi)
        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
    }

}