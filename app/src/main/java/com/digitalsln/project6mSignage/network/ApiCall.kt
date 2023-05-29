package com.digitalsln.project6mSignage.network

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.digitalsln.project6mSignage.DisplayOverlayReceiver
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.model.TimeData
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton

@Singleton
class ApiCall(context: Context) {
    val context = context
    fun callApi() {
        var localScreenCode = AppPreference(context).retrieveLocalScreenCode(
            "LOCAL_SCREEN_CODE",
            "LOCAL_CODE"
        )
        ApiClient.client().create(ApiInterface::class.java)
            .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                override fun onResponse(
                    call: Call<List<TimeData>>,
                    response: Response<List<TimeData>>
                ) {
                    if (response.isSuccessful) {
                        val calendar = Calendar.getInstance()
                        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
                        AppPreference(context).saveFromTime(
                            response.body()!![day].from,
                            "FROM_TIME"
                        )
                        AppPreference(context).saveToTime(
                            response.body()!![day].to,
                            "TO_TIME"
                        )
                        Log.d("TvTimer", "${response.body()}")
                        lockTV()
                    } else {
                        Log.d("TvTimer", "Failed")
                    }
                }

                override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                    Log.d("TvTimer", "$t")
                }
            })
    }

    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV() {
        try {
            if (MainActivity.wakeLock.isHeld) {
                Log.d("TvTimer", "${MainActivity.wakeLock.isHeld}")
                MainActivity.wakeLock.release()
            }
            MainActivity.isTimerSet = true

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(context, DisplayOverlayReceiver::class.java)
            i.action = "com.example.androidtvdemo.START_ALARM"
            val pi = PendingIntent.getBroadcast(context, 0, i, 0);

            val futureDate: Calendar = Calendar.getInstance()

            val fromTime =
                AppPreference(context).retrieveFromTime("FROM_TIME", "FROM_TIME")

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse(fromTime) //give the fromTime here
            cal.time = date

            Log.d(
                "TvTimer",
                "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
            )
            futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
            futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
            futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}