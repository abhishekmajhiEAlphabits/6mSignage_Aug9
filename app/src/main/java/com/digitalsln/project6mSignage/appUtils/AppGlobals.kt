package com.digitalsln.project6mSignage.appUtils

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.receivers.*
import java.util.*


class AppGlobals : Application() {

    @Override
    override fun onCreate() {
        super.onCreate()
    }


    /* cancels all previously scheduled alarms */
    fun cancelMultipleAlarms() {
        val size = 7
        val alarmManagers = arrayOfNulls<AlarmManager>(size)
        val intents = arrayOf<Intent>(
            Intent(applicationContext, ShutDownReceiverToIdeal::class.java),
            Intent(applicationContext, ShutDownReceiverToLogic::class.java),
            Intent(applicationContext, TimeOutReceiver::class.java),
            Intent(applicationContext, WakeUpReceiver::class.java),
            Intent(applicationContext, ApiCallSchedulerInitReceiver::class.java),
            Intent(applicationContext, ScheduleApiTimerReceiverOne::class.java),
            Intent(applicationContext, ScheduleApiTimerReceiverTwo::class.java)
        )
        for (i in 0 until size) {
            alarmManagers[i] = getSystemService(ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0,
                intents[i]!!, PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManagers[i]!!.cancel(pendingIntent)
        }
    }


    /* alarm manager to initialize and call the api */
    fun scheduleApiCallTimer() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(applicationContext, ApiCallSchedulerInitReceiver::class.java)
        val pi = PendingIntent.getBroadcast(applicationContext, 0, i, 0)
        val futureDate: Calendar = Calendar.getInstance()
        futureDate.set(Calendar.HOUR_OF_DAY, 23)
        futureDate.set(Calendar.MINUTE, 59)
        futureDate.set(Calendar.SECOND, 0)
//        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
        val ac = AlarmManager.AlarmClockInfo(futureDate.time.time, pi)
        am.setAlarmClock(ac, pi)
    }

}