package com.digitalsln.project6mSignage.network

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.digitalsln.project6mSignage.ForegroundService
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.model.TimeData
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton


@Singleton
class ApiCall(context: Context) {
    private val TAG = "TvTimer"
    val context = context

    fun callApi() {
//        if(isInternetConnected()){
//
//        }
//        else{
//            Toast.makeText(context, "No Internet Connection", Toast.LENGTH_LONG).show()
//            Log.d(TAG, "No internet")
//        }
        try {
            var localScreenCode = AppPreference(context).retrieveLocalScreenCode(
                Constants.localScreenCode,
                Constants.defaultLocalScreenCode
            )

            Toast.makeText(
                context,
                "Screen Code From App Preference :: $localScreenCode",
                Toast.LENGTH_LONG
            ).show()
            ApiClient.client().create(ApiInterface::class.java)
                .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                    @RequiresApi(Build.VERSION_CODES.O)
                    override fun onResponse(
                        call: Call<List<TimeData>>,
                        response: Response<List<TimeData>>
                    ) {
                        if (response.isSuccessful) {
                            Toast.makeText(context, "" + response, Toast.LENGTH_LONG).show()

                            if (response.body() != null) {
                                val calendar = Calendar.getInstance()
                                val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
                                var fromTime = "" //fromTime - screen on time
                                var toTime = ""  //toTime - screen off time

                                if (response.body()!![day] != null && response.body()!![day].from != null) {
                                    fromTime = response.body()!![day].from
                                }

                                if (response.body()!![day] != null && response.body()!![day].to != null) {
                                    toTime = response.body()!![day].to
                                }

                                if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                    AppPreference(context).saveFromTime(
                                        fromTime,
                                        Constants.fromTime
                                    )
                                    AppPreference(context).saveToTime(
                                        toTime,
                                        Constants.toTime
                                    )
                                    Toast.makeText(
                                        context,
                                        "fromTime : $fromTime & toTime : $toTime",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    Log.d(TAG, "${response.body()}")
                                    if (validTime(fromTime) && validTime(toTime)) {
                                        /* if api call is successful then alarm manager for screen off/on is called */
                                        lockTV()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Invalid Time",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                        } else {
                            Toast.makeText(
                                context,
                                "Failed api call",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "Failed")
                        }
                    }

                    override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                        Log.d(TAG, "$t")
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV() {
        try {
            MainActivity.isTimerSet = true
            /* initializes and schedules alarm manager for performing screen off and on */
//            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val i = Intent(context, DisplayOverlayReceiver::class.java)
//            val pi = PendingIntent.getBroadcast(context, 0, i, 0);
            val futureDate: Calendar = Calendar.getInstance()

            var apiFromTime = getApiFromTime()
            var apiToTime = getApiToTime()

            /* gets the times from api and compares it with system current time */
            val calApiFromTime =
                apiFromTime[Calendar.HOUR_OF_DAY] * 3600 + apiFromTime[Calendar.MINUTE] * 60 + apiFromTime[Calendar.SECOND]
            val calApiToTime =
                apiToTime[Calendar.HOUR_OF_DAY] * 3600 + apiToTime[Calendar.MINUTE] * 60 + apiToTime[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG, "$calApiFromTime :: $calApiToTime :: $systemCurrentTime"
            )

            var hrs = futureDate[Calendar.HOUR_OF_DAY]
            var mins = futureDate[Calendar.MINUTE]
            var secs = futureDate[Calendar.SECOND]
            Toast.makeText(
                context,
                "Current Hours :: $hrs && Current Mins : $mins && Current secs : $secs",
                Toast.LENGTH_LONG
            ).show()

            /* if times from api is greater than system time then only schedules the alarm */
            if (calApiFromTime > systemCurrentTime || calApiToTime > systemCurrentTime) {
                var cal: Calendar? = null
                if (calApiFromTime > systemCurrentTime) {
                    cal = apiFromTime
                } else if (calApiToTime > systemCurrentTime) {
                    cal = apiToTime
                } else {
                    cal = apiFromTime
                }
                Log.d(
                    TAG,
                    "${cal!!.get(Calendar.HOUR_OF_DAY)} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                )

                /* starts service to initiate handle screen on/off*/
                startService()

//                futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
//                futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
//                futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])
//                am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
            } else {
                if (!MainActivity.wakeLock.isHeld) {
                    MainActivity.wakeLock.acquire()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Lock failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Function to validate the
    // Traditional Time Formats (HH:MM:SS)
    private fun validTime(str: String): Boolean {
        // Regex to check valid
        // Traditional Time Formats
        // (HH:MM:SS  or HH:MM).
        var pattern = "^(?:[01]\\d|2[0123]):(?:[012345]\\d):(?:[012345]\\d)$"

        // If the str
        // is empty return false
        if (str.isEmpty()) {
            return false
        }

        // Return true if the str
        // matched the ReGex
        return str.matches(pattern.toRegex())
    }

    private fun isInternetConnected() : Boolean{
        val cm = context.getSystemService(AppCompatActivity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val currentnetwork = cm.activeNetwork
        if (currentnetwork != null) {
            return cm.getNetworkCapabilities(currentnetwork)!!
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && cm.getNetworkCapabilities(
                currentnetwork
            )!!
                .hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            return false
        }
    }

    /* returns api param fromTime in Calendar format*/
    private fun getApiFromTime(): Calendar {
        val fromTime =
            AppPreference(context).retrieveFromTime(
                Constants.fromTime,
                Constants.defaultFromTime
            )
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(fromTime) //give the fromTime here
        cal.time = date
        return cal
    }

    /* returns api param toTime in Calendar format*/
    private fun getApiToTime(): Calendar {
        val toTime =
            AppPreference(context).retrieveFromTime(
                Constants.toTime,
                Constants.defaultToTime
            )
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(toTime) //give the toTime here
        cal.time = date
        return cal
    }

    /*service to start alarm manager for screen on/off*/
    private fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // check if the user has already granted
            // the Draw over other apps permission
            if (Settings.canDrawOverlays(context)) {
                // start the service based on the android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(
                        Intent(
                            context,
                            ForegroundService::class.java
                        )
                    )
                } else {
                    context.startService(Intent(context, ForegroundService::class.java))
                }
            }
        } else {
            context.startService(Intent(context, ForegroundService::class.java))
        }
    }
}