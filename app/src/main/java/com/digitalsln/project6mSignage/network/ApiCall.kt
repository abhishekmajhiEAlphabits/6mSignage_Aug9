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
import com.digitalsln.project6mSignage.appUtils.AppLogger
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
    private lateinit var appLogger: AppLogger


    init {
        timerHelpers = TimerHelpers(context)
        appLogger = AppLogger()
    }

    /*calls api and stores the fromTime and toTime in the preferences(fromTime,toIdeal,toLogic)
    accordingly
     */
    fun callApi() {
        try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var logTime = sdf.format(cal.time)
            var log = "$logTime Calling backend API to get timings"
            Log.d(TAG, "$log")
            appLogger.appendLog(log)
            var localScreenCode = AppPreference(context).retrieveLocalScreenCode(
                Constants.localScreenCode,
                Constants.defaultLocalScreenCode
            )

            ApiClient.client().create(ApiInterface::class.java)
                .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                    override fun onResponse(
                        call: Call<List<TimeData>>,
                        response: Response<List<TimeData>>
                    ) {
                        if (response.isSuccessful) {
//                            Toast.makeText(context, "" + response, Toast.LENGTH_LONG)
//                                .show()

                            if (response.body() != null) {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                var logTime = sdf.format(cal.time)
                                var log = "$logTime API returned response successfully"
                                Log.d(TAG, "$log")
                                appLogger.appendLog(log)

                                for (i in 0..6) {
                                    var fromTime = "" //fromTime - screen on time
                                    var toTime = ""  //toTime - screen off time
                                    var fromTimeSeconds = 0
                                    var toTimeSeconds = 0
                                    if (i == 6) {
                                        if (response.body()!![i].day != null && response.body()!![0].day != null) {
                                            var dayName =
                                                timerHelpers.getWeekDay(response.body()!![i].day)
                                            if (response.body()!![i] != null && response.body()!![i].from != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].from) //give the fromTime here
                                                cal.time = date
                                                val apiFromTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                fromTimeSeconds = apiFromTimeSeconds
                                                fromTime = response.body()!![i].from
                                            }

                                            if (response.body()!![i] != null && response.body()!![i].to != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].to) //give the toTime here
                                                cal.time = date
                                                val apiToTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                toTimeSeconds = apiToTimeSeconds
                                                toTime = response.body()!![i].to
                                            }
                                            if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                                if (timerHelpers.validTime(fromTime) && timerHelpers.validTime(
                                                        toTime
                                                    )
                                                ) {
                                                    if (fromTimeSeconds > toTimeSeconds) {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![0].day)
                                                        AppPreference(context).saveToLogicTime(
                                                            toTime,
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(context).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(context).saveToIdealTime(
                                                            "00:00:00",
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    } else {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![0].day)
                                                        AppPreference(context).saveToLogicTime(
                                                            "00:00:00",
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(context).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(context).saveToIdealTime(
                                                            toTime,
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    }
                                                    val cal = Calendar.getInstance()
                                                    val sdf =
                                                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                                    var logTime = sdf.format(cal.time)
                                                    var log = "$logTime timings was stored locally"
                                                    Log.d(TAG, "$log")
                                                    appLogger.appendLog(log)
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
                                    } else {
                                        if (response.body()!![i].day != null && response.body()!![i + 1].day != null) {
                                            var dayName =
                                                timerHelpers.getWeekDay(response.body()!![i].day)
                                            if (response.body()!![i] != null && response.body()!![i].from != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].from) //give the fromTime here
                                                cal.time = date
                                                val apiFromTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                fromTimeSeconds = apiFromTimeSeconds
                                                fromTime = response.body()!![i].from
                                            }

                                            if (response.body()!![i] != null && response.body()!![i].to != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].to) //give the toTime here
                                                cal.time = date
                                                val apiToTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                toTimeSeconds = apiToTimeSeconds
                                                toTime = response.body()!![i].to
                                            }
                                            if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                                if (timerHelpers.validTime(fromTime) && timerHelpers.validTime(
                                                        toTime
                                                    )
                                                ) {
                                                    if (fromTimeSeconds > toTimeSeconds) {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![i + 1].day)
                                                        AppPreference(context).saveToLogicTime(
                                                            toTime,
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(context).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(context).saveToIdealTime(
                                                            "00:00:00",
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    } else {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![i + 1].day)
                                                        AppPreference(context).saveToLogicTime(
                                                            "00:00:00",
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(context).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(context).saveToIdealTime(
                                                            toTime,
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    }
                                                    val cal = Calendar.getInstance()
                                                    val sdf =
                                                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                                    var logTime = sdf.format(cal.time)
                                                    var log = "$logTime timings was stored locally"
                                                    Log.d(TAG, "$log")
                                                    appLogger.appendLog(log)
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
                            }

                        } else {
                            Toast.makeText(
                                context,
                                "Failed to refresh! Please try again",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG, "Failed")
                        }
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api
                        with the stored data if there is any */
                        lockTvDayBase()
                    }

                    override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                        Toast.makeText(
                            context,
                            "Failed to refresh! Please try again",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d(TAG, "$t")
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api
                        with the stored data if there is any */
                        lockTvDayBase()
                    }
                })
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to refresh! Please try again",
                Toast.LENGTH_SHORT
            ).show()
            Log.d(TAG, "$e")
        }
    }

    private fun lockTvDayBase() {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
        lockTV(day)
//        Log.d(TAG2,"another day :: fromTime -- ${timerHelpers.getApiFromTimePreferences(6)} :: toTime -- ${timerHelpers.getApiToTimePreferences(6)}")
    }


    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV(day: Int) {
        try {
            MainActivity.isTimerSet = true

            val futureDate: Calendar = Calendar.getInstance()

            /* gets the times from preferences and compares it with system current time */
            var apiFromTime = timerHelpers.getApiFromTime(day)
            var apiToIdealTime = timerHelpers.getApiToIdealTime(day)
            var apiToLogicTime = timerHelpers.getApiToLogicTime(day)
            val calApiFromTime =
                apiFromTime[Calendar.HOUR_OF_DAY] * 3600 + apiFromTime[Calendar.MINUTE] * 60 + apiFromTime[Calendar.SECOND]
            val calApiToIdealTime =
                apiToIdealTime[Calendar.HOUR_OF_DAY] * 3600 + apiToIdealTime[Calendar.MINUTE] * 60 + apiToIdealTime[Calendar.SECOND]
            val calApiToLogicTime =
                apiToLogicTime[Calendar.HOUR_OF_DAY] * 3600 + apiToLogicTime[Calendar.MINUTE] * 60 + apiToLogicTime[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG,
                "$calApiFromTime :: $calApiToIdealTime :: $calApiToIdealTime :: $systemCurrentTime"
            )

            var hrs = futureDate[Calendar.HOUR_OF_DAY]
            var mins = futureDate[Calendar.MINUTE]
            var secs = futureDate[Calendar.SECOND]

            /* starts service to initiate handle screen on/off*/
            startService()

            if (!MainActivity.wakeLock.isHeld) {
                MainActivity.wakeLock.acquire()
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