package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter

object Utils {
    fun getIpAddress(context: Context): String {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
    }
}