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

/**
 * receiver to call api after 24 hours of the ScheduleApiTimerReceiverOne receiver
 * so ScheduleApiTimerReceiverOne and ScheduleApiTimerReceiverTwo gets called in loop
 * after every 24 hours therefore calls the api everyday.
 */
class ScheduleApiTimerReceiverTwo : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = ApiCall(context)
        call.callApi()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(context, ScheduleApiTimerReceiverOne::class.java)
        val pi = PendingIntent.getBroadcast(context, 0, i, 0)
        val futureDate: Calendar = Calendar.getInstance()
        Log.d("TvTimer","inside api 2 :: ${futureDate.time}")
        futureDate.add(Calendar.HOUR_OF_DAY, 24)
        Log.d("TvTimer","${futureDate.time}")
//        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
        val ac = AlarmManager.AlarmClockInfo(futureDate.time.time, pi)
        am.setAlarmClock(ac,pi)
    }

}