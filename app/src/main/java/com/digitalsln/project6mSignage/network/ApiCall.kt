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
import com.digitalsln.project6mSignage.appUtils.TimerHelpers
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
    private lateinit var timerHelpers: TimerHelpers
    private var context = context


    init {
        timerHelpers = TimerHelpers(context)
    }

    fun callApi() {
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
                                for (i in 0..6) {
                                    var fromTime = "" //fromTime - screen on time
                                    var toTime = ""  //toTime - screen off time
                                    if (response.body()!![i].day != null) {
                                        var dayName =
                                            timerHelpers.getWeekDay(response.body()!![i].day)
                                        if (response.body()!![i] != null && response.body()!![i].from != null) {
                                            fromTime = response.body()!![i].from
                                        }

                                        if (response.body()!![i] != null && response.body()!![i].to != null) {
                                            toTime = response.body()!![i].to
                                        }
                                        if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                            if (timerHelpers.validTime(fromTime) && timerHelpers.validTime(
                                                    toTime
                                                )
                                            ) {
                                                AppPreference(context).saveFromTime(
                                                    fromTime,
                                                    "$dayName-${Constants.fromTime}"
                                                )
                                                AppPreference(context).saveToTime(
                                                    toTime,
                                                    "$dayName-${Constants.toTime}"
                                                )
                                                Toast.makeText(
                                                    context,
                                                    "fromTime : $fromTime & toTime : $toTime",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                Log.d(TAG, "${response.body()}")
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Invalid Time",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
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
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api with
                        the stored data if there is any */
                        lockTvDayBase()
                    }

                    override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                        Log.d(TAG, "$t")
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api with
                        the stored data if there is any */
                        lockTvDayBase()
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    private fun lockTvDayBase() {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
        var fromTimePrefs = timerHelpers.getApiFromTimePreferences(day)
        var toTimePrefs = timerHelpers.getApiToTimePreferences(day)
        if (fromTimePrefs.isNotEmpty() && toTimePrefs.isNotEmpty()) {
            if (timerHelpers.validTime(fromTimePrefs) && timerHelpers.validTime(toTimePrefs)) {
                /* if prefs has valid values then alarm manager for screen off/on is called */
                lockTV(day)
            }
        }
    }


    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV(day: Int) {
        try {
            MainActivity.isTimerSet = true
            /* initializes and schedules alarm manager for performing screen off and on */
//            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//            val i = Intent(context, DisplayOverlayReceiver::class.java)
//            val pi = PendingIntent.getBroadcast(context, 0, i, 0);
            val futureDate: Calendar = Calendar.getInstance()

            var apiFromTime = timerHelpers.getApiFromTime(day)
            var apiToTime = timerHelpers.getApiToTime(day)

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
                    "${cal!![Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                )

                /* starts service to initiate handle screen on/off*/
                startService()

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