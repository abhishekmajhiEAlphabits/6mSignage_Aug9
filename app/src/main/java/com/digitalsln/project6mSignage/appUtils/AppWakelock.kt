package com.digitalsln.project6mSignage.appUtils

import android.app.Application
import android.content.Context
import android.os.PowerManager
import javax.inject.Singleton


class AppWakelock : Application() {

//    private lateinit var powerManager: PowerManager
//    lateinit var wakeLock: PowerManager.WakeLock
//    private lateinit var appInstance: Context

    @Override
    override fun onCreate() {
        super.onCreate()
//        powerManager = applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
//        wakeLock = powerManager.newWakeLock(
//            PowerManager.FULL_WAKE_LOCK or
//                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
//                    PowerManager.ON_AFTER_RELEASE, "appname::WakeLock"
//        )

//        wakeLock.acquire()

    }

//    private fun getInstance(): Context {
//        return appInstance
//    }

}