package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import java.util.*

class ShutDownReceiver : BroadcastReceiver() {
    private val TAG = "6mSignage"
    override fun onReceive(context: Context, intent: Intent) {
        val defaultTimeOut =
            Settings.System.getString(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
        Log.d(TAG, "inside shutdown receiver")

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
            futureDate.add(Calendar.SECOND, 20)

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
//            Log.d(TAG, "inside wake up alarm manager")
        } catch (e: Exception) {
            Log.d(TAG, "wake up alarm manager failed")
        }


        try {

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, TimeOutReceiver::class.java)
            i.putExtra("timeOutValue", defaultTimeOut)
            val pi = PendingIntent.getBroadcast(context, 0, i, 0);

            val futureDate: Calendar = Calendar.getInstance()
            futureDate.add(Calendar.SECOND, 15)

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
//            Log.d(TAG, "inside set default timer alarm manager")
        } catch (e: Exception) {
            Log.d(TAG, "inside set default timer alarm manager failed")
        }
    }
}