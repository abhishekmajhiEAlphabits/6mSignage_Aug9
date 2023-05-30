package com.digitalsln.project6mSignage.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants
import java.text.SimpleDateFormat
import java.util.*

/**
 * Broadcast Receiver class of AlarmManager to turn off
 * screen and wake up
 */
class ShutDownReceiver : BroadcastReceiver() {
    private val TAG = "TvTimer"
    override fun onReceive(context: Context, intent: Intent) {

        try {
            Settings.System.putString(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                "0"
            )  //setting screen_timeout to 10sec
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, WakeUpReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0);
            val futureDate: Calendar = Calendar.getInstance()
            val toTime = AppPreference(context).retrieveToTime(Constants.toTime, Constants.defaultToTime)
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toTime) //give the toTime here
            cal.time = date
            val apiTime =
                cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG, "$apiTime :: $systemCurrentTime"
            )
            if (apiTime > systemCurrentTime) {
                Log.d(
                    TAG,
                    "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                )
                futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
                futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
                futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])
                am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
            }
        } catch (e: Exception) {
            Log.d(TAG, "wake up alarm failed")
        }


        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, TimeOutReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0);
            val futureDate: Calendar = Calendar.getInstance()
            val toTime = AppPreference(context).retrieveToTime(Constants.toTime, Constants.defaultToTime)
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toTime) //give the toTime here
            cal.time = date
            val apiTime =
                cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG, "$apiTime :: $systemCurrentTime"
            )
            if (apiTime > systemCurrentTime) {
                Log.d(
                    TAG,
                    "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                )
                futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
                futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
                futureDate.set(Calendar.SECOND, cal[Calendar.SECOND] - 10)
                am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
            }
        } catch (e: Exception) {
            Log.d(TAG, "default timer alarm failed")
        }
    }
}