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
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import java.text.SimpleDateFormat
import java.util.*

class ShutDownReceiverToLogic : BroadcastReceiver() {
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
            Log.d(TAG, "inside shutdown receiver toLogic")
            /* if wakelock is acquired it is released to turn off screen at set time */
            if (MainActivity.wakeLock.isHeld) {
                MainActivity.wakeLock.release()
            }

            var isTimeOutSet = Settings.System.putString(
                context!!.applicationContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                "0"
            )  //setting screen_timeout to 10sec

            AppPreference(context).setShutTimerSet(true)

            if (isTimeOutSet) {
                var timeOutLog = "$logTime screen timeout set successfully"
                appLogger.appendLog(timeOutLog)
                Log.d(TAG, "$timeOutLog")
            } else {
                var timeOutLog = "$logTime Failed to set screen timeout."
                appLogger.appendLog(timeOutLog)
                Log.d(TAG, "$timeOutLog")
            }

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, TimeOutReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_MUTABLE)
            val futureDate: Calendar = Calendar.getInstance()
            val toLogicTime =
                timerHelpers.getApiToLogicTimePreferences(timerHelpers.getWeekDayInInt())
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toLogicTime) //give the toLogicTime here
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
            am.setAlarmClock(ac, pi)

        } catch (e: Exception) {
            val calendar = Calendar.getInstance()
            val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var logTime = sdfData.format(calendar.time)
            var log = "$logTime Failed to turn off screen. Error message: $e"
            appLogger.appendLog(log)
            Log.d(TAG, "$log")
            Log.d(TAG, "default timer alarm failed called from toLogic")
        }
    }
}