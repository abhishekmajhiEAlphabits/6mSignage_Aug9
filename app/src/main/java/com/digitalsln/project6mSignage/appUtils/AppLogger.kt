package com.digitalsln.project6mSignage.appUtils

import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import javax.inject.Singleton

@Singleton
class AppLogger {
    private var TAG = "TvTimer"

    fun appendLog(text: String?) {
        try {
            val logFile = File("/sdcard/Pictures/log.txt")
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile()
                } catch (e: IOException) {
                    Log.d(TAG, "File log create error : $e")
                    e.printStackTrace()
                }
            }
            try {
                //BufferedWriter for performance, true to set append to file flag
                val buf = BufferedWriter(FileWriter(logFile, true))
                buf.append(text)
                buf.newLine()
                buf.close()
            } catch (e: Exception) {
                Log.d(TAG, "File log write error : $e")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.d(TAG, "File log append error : $e")
        }

    }
}