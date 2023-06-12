package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.content.Context
import android.content.SharedPreferences

class TimerPreferences(context: Context) {
    var sharedPreference: SharedPreferences? = null

    fun saveFromTime(value: String, key: String, day: Int) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveFromTime(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveToTime(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveToTime(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }
}