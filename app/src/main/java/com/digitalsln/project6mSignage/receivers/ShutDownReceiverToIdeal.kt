package com.digitalsln.project6mSignage.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.appUtils.AppLogger
import com.digitalsln.project6mSignage.appUtils.TimerHelpers
import java.text.SimpleDateFormat
import java.util.*

/**
 * Broadcast Receiver class of AlarmManager to turn off
 * screen
 */
class ShutDownReceiverToIdeal : BroadcastReceiver() {
    private val TAG = "TvTimer"
    private lateinit var timerHelpers: TimerHelpers
    private lateinit var appLogger: AppLogger
    override fun onReceive(context: Context, intent: Intent) {
        timerHelpers = TimerHelpers(context)
        appLogger = AppLogger()
        setTimeOut(context)
    }

    /*set the default screen timeOut*/
    private fun setTimeOut(context: Context) {
        try {
            val calendar = Calendar.getInstance()
            val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var logTime = sdfData.format(calendar.time)
            var log = "$logTime screen was successfully turned off"
            Log.d(TAG, "$log")
            appLogger.appendLog(log)
            Log.d(TAG, "inside shutdown receiver toIdeal")
            /* if wakelock is acquired it is released to turn off screen at set time */
            if (MainActivity.wakeLock.isHeld) {
                MainActivity.wakeLock.release()
            }

            Settings.System.putString(
                context!!.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                "0"
            )  //setting screen_timeout to 10sec

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, TimeOutReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0)
            val futureDate: Calendar = Calendar.getInstance()
            val toIdealTime =
                timerHelpers.getApiToIdealTimePreferences(timerHelpers.getWeekDayInInt())
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toIdealTime) //give the toIdealTime here
            cal.time = date
            val apiTime =
                cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG, "$apiTime :: $systemCurrentTime"
            )
            futureDate.add(Calendar.SECOND, 20)
//            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
            val ac = AlarmManager.AlarmClockInfo(futureDate.time.time, pi)
            am.setAlarmClock(ac,pi)

        } catch (e: Exception) {
            val calendar = Calendar.getInstance()
            val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var logTime = sdfData.format(calendar.time)
            var log = "$logTime Failed to turn off screen. Error message: $e"
            appLogger.appendLog(log)
            Log.d(TAG, "$log")
            Log.d(TAG, "default timer alarm failed called from toIdeal")
        }
    }
}