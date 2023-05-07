package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class AppPreference(context: Context) {

    var sharedPreference: SharedPreferences? = null

    companion object {
        const val PREF_NAME = "tvApp"
        const val DEFAULT_LAUNCHER = "launcher"
    }

    init {
        if (sharedPreference == null)
            sharedPreference = context.getSharedPreferences(PREF_NAME,MODE_PRIVATE)
    }

    fun isAppDefaultLauncher(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean(DEFAULT_LAUNCHER, false)
    }

    fun setAppDefaultLauncher(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean(DEFAULT_LAUNCHER, flag).apply()
    }
}