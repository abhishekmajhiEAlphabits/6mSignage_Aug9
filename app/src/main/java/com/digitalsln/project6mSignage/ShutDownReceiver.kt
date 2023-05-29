package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import java.text.SimpleDateFormat
import java.util.*

class ShutDownReceiver : BroadcastReceiver() {
    private val TAG = "6mSignage"
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TvTimer", "inside shutdown receiver")

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

            val toTime = AppPreference(context).retrieveToTime("TO_TIME", "TO_TIME")

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toTime) //give the toTime here
            cal.time = date

            Log.d(
                "TvTimer",
                "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
            )
            futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
            futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
            futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
            Log.d("TvTimer", "inside wake up alarm manager")
        } catch (e: Exception) {
            Log.d("TvTimer", "wake up alarm manager failed")
        }


        try {

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, TimeOutReceiver::class.java)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0);

            val futureDate: Calendar = Calendar.getInstance()

            val toTime = AppPreference(context).retrieveToTime("TO_TIME", "TO_TIME")

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(toTime) //give the toTime here
            cal.time = date

            Log.d(
                "TvTimerTO",
                "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
            )
            futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
            futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
            futureDate.set(Calendar.SECOND, cal[Calendar.SECOND] - 10)

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
            Log.d("TvTimer", "inside set default timer alarm manager")
        } catch (e: Exception) {
            Log.d("TvTimer", "inside set default timer alarm manager failed")
        }
    }
}