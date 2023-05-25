package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitalsln.project6mSignage.network.ApiCall
import java.util.*

class ScheduleApiTimerReceiverOne : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("abhi", "inside api receiver 1 ")

        val call = ApiCall(context)
        call.callApi()

        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ScheduleApiTimerReceiverTwo::class.java)
        i.action = "com.example.androidtvdemo.START_ALARM"
        val pi = PendingIntent.getBroadcast(context, 0, i, 0);

        val futureDate: Calendar = Calendar.getInstance()
//        futureDate.add(Calendar.SECOND, 10)
//        futureDate.set(Calendar.HOUR_OF_DAY, 17)
//        futureDate.set(Calendar.MINUTE, 58)
        futureDate.add(Calendar.SECOND, 5)

        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
    }

}