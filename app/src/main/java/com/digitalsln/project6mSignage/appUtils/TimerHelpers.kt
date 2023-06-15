package com.digitalsln.project6mSignage.appUtils

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Singleton

@Singleton
class TimerHelpers(context: Context) {

    private val context = context
    private var TAG = "TvTimer"

    /* returns api param fromTime in Calendar format*/
    fun getApiFromTime(day: Int): Calendar {
        var dayName = getWeekDay(day)
        val fromTime =
            AppPreference(context).retrieveFromTime(
                "$dayName-${Constants.fromTime}",
                Constants.defaultFromTime
            )
        Log.d(TAG, "getApiFrom :: $fromTime")
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(fromTime) //give the fromTime here
        cal.time = date
        return cal
    }

    /* returns api param toIdealTime in Calendar format*/
    fun getApiToIdealTime(day: Int): Calendar {
        var dayName = getWeekDay(day)
        val toTime =
            AppPreference(context).retrieveToIdealTime(
                "$dayName-${Constants.toIdealTime}",
                Constants.defaultToIdealTime
            )
        Log.d(TAG, "getApiToIdeal :: $toTime")
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(toTime) //give the toTime here
        cal.time = date
        return cal
    }

    /* returns api param toLogicTime in Calendar format*/
    fun getApiToLogicTime(day: Int): Calendar {
        var dayName = getWeekDay(day)
        val toTime =
            AppPreference(context).retrieveToLogicTime(
                "$dayName-${Constants.toLogicTime}",
                Constants.defaultToLogicTime
            )
        Log.d(TAG, "getApiToLogic :: $toTime")
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
        val date: Date = sdf.parse(toTime) //give the toTime here
        cal.time = date
        return cal
    }

    /* returns fromTime preferences in String format*/
    fun getApiFromTimePreferences(day: Int): String {
        var dayName = getWeekDay(day)
        return AppPreference(context).retrieveFromTime(
            "$dayName-${Constants.fromTime}",
            Constants.defaultFromTime
        )
    }

    /* returns toLogicTime preferences in String format*/
    fun getApiToIdealTimePreferences(day: Int): String {
        var dayName = getWeekDay(day)
        return AppPreference(context).retrieveToIdealTime(
            "$dayName-${Constants.toIdealTime}",
            Constants.defaultToIdealTime
        )
    }

    /* returns toLogicTime preferences in String format*/
    fun getApiToLogicTimePreferences(day: Int): String {
        var dayName = getWeekDay(day)
        return AppPreference(context).retrieveToLogicTime(
            "$dayName-${Constants.toLogicTime}",
            Constants.defaultToLogicTime
        )
    }

    fun getWeekDayInInt(): Int {
        val calendar = Calendar.getInstance()
        return calendar.get(Calendar.DAY_OF_WEEK) - 1
    }

    fun getWeekDay(day: Int): String {
        when (day) {
            0 -> {
                return "Sunday"
            }
            1 -> {
                return "Monday"
            }
            2 -> {
                return "Tuesday"
            }
            3 -> {
                return "Wednesday"
            }
            4 -> {
                return "Thursday"
            }
            5 -> {
                return "Friday"
            }
            6 -> {
                return "Saturday"
            }
        }
        return "NA"
    }

    // Function to validate the
    // Traditional Time Formats (HH:MM:SS)
    fun validTime(str: String): Boolean {
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

    fun isTimeValidAndNotEmpty(str: String): Boolean {
        return str.isNotEmpty() && validTime(str) && str != Constants.defaultFromTime
    }
}