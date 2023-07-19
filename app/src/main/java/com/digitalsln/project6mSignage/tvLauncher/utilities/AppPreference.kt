package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class AppPreference(context: Context) {

    var sharedPreference: SharedPreferences? = null
    var sharedIntervalPreference: SharedPreferences? = null

    companion object {
        const val PREF_NAME = "tvApp"
        const val INTERVAL_PREF_NAME = "intervalPref"
        const val DEFAULT_LAUNCHER = "launcher"
    }

    init {
        if (sharedPreference == null)
            sharedPreference = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE)

        if (sharedIntervalPreference == null)
            sharedIntervalPreference = context.getSharedPreferences(INTERVAL_PREF_NAME, MODE_PRIVATE)
    }

    fun isAppDefaultLauncher(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean(DEFAULT_LAUNCHER, false)
    }

    fun setAppDefaultLauncher(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean(DEFAULT_LAUNCHER, flag).apply()
    }

    fun saveKeyValue(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveValueByKey(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveIntervalKeyValue(value: String, key: String) {
        if (sharedIntervalPreference == null) return
        sharedIntervalPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveIntervalValueByKey(key: String, default: String): String {
        return sharedIntervalPreference?.getString(key, default)!!
    }

    fun saveLocalScreenCode(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveLocalScreenCode(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveExternalScreenCode(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveExternalScreenCode(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveFromTime(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveFromTime(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveToIdealTime(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveToIdealTime(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveToLogicTime(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveToLogicTime(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun saveDefaultTimeOut(value: String, key: String) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putString(key, value).apply()
    }

    fun retrieveDefaultTimeOut(key: String, default: String): String {
        return sharedPreference?.getString(key, default)!!
    }

    fun isFirstTimeRun(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean("FIRST_RUN", true)
    }

    fun setFirstTimeRun(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean("FIRST_RUN", flag).apply()
    }

    fun isScreenRegistered(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean("SCREEN_REGISTERED", false)
    }

    fun setScreenRegistered(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean("SCREEN_REGISTERED", flag).apply()
    }

    fun isPlaylistBound(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean("PLAYLIST_BOUND", false)
    }

    fun setPlaylistBound(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean("PLAYLIST_BOUND", flag).apply()
    }

    fun isFirstTimeRunSettings(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean("FIRST_SETTINGS", true)
    }

    fun setFirstTimeRunSettings(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean("FIRST_SETTINGS", flag).apply()
    }

    fun isShutTimerSet(): Boolean {
        if (sharedPreference == null) return false
        return sharedPreference!!.getBoolean("SHUT_TIMER", false)
    }

    fun setShutTimerSet(flag: Boolean) {
        if (sharedPreference == null) return
        sharedPreference!!.edit().putBoolean("SHUT_TIMER", flag).apply()
    }
}