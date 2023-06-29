package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.Toast
import com.digitalsln.project6mSignage.appUtils.AppLogger
import com.digitalsln.project6mSignage.appUtils.TimerHelpers
import com.digitalsln.project6mSignage.receivers.ShutDownReceiverToIdeal
import com.digitalsln.project6mSignage.receivers.ShutDownReceiverToLogic
import com.digitalsln.project6mSignage.receivers.WakeUpReceiver
import java.text.SimpleDateFormat
import java.util.*

/**
 * overlay screen to show on turn off countdown
 * and also handle screen on
 */
class Window(context: Context) {

    // declaring required variables
    private var context: Context? = null
    private var mView: View? = null
    private var mParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var layoutInflater: LayoutInflater? = null
    private lateinit var timerHelpers: TimerHelpers
    private val TAG = "TvTimer"
    private lateinit var appLogger: AppLogger

    init {
        this.context = context
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // set the layout parameters of the window
            mParams = WindowManager.LayoutParams( // Shrink the window to wrap the content rather
                // than filling the screen
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,  // Display it on top of other application windows
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // Don't let it grab the input focus
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,  // Make the underlying application window visible
                // through any transparent parts
                PixelFormat.TRANSLUCENT
            )
        }
        // getting a LayoutInflater
        layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        // inflating the view with the custom layout we created
        mView = layoutInflater!!.inflate(R.layout.overlay_screen, null)

        // Define the position of the
        // window within the screen
        mParams!!.gravity = Gravity.CENTER
        mWindowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        timerHelpers = TimerHelpers(context)
        appLogger = AppLogger()
    }

    fun open() {
        try {
            // check if the view is already
            // inflated or present in the window
            if (mView!!.windowToken == null) {
                if (mView!!.parent == null) {
                    mWindowManager!!.addView(mView, mParams)
                    // call to turn off screen for toIdeal case
                    turnOffScreenIdealCase()

                    // call to turn off screen for toLogic case
                    turnOffScreenLogicCase()

                    //call to turn on screen
                    turnOnScreen()
                }
            }
            val timerObj = Timer()
            val timerTaskObj: TimerTask = object : TimerTask() {
                override fun run() {
                    //perform your action here
                    close()
                }
            }
            timerObj.schedule(timerTaskObj, 0, 10)
        } catch (e: Exception) {
            Log.d("Error1", e.toString())
        }
    }

    private fun close() {
        try {
            // remove the view from the window
            (context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(mView)
            // invalidate the view
            mView!!.invalidate()
            // remove all views
            (mView!!.parent as ViewGroup).removeAllViews()

            // the above steps are necessary when you are adding and removing
            // the view simultaneously, it might give some exceptions
        } catch (e: Exception) {
            Log.d("Error2", e.toString())
        }
    }

    /*turns screen off at set time for toIdeal time case*/
    private fun turnOffScreenIdealCase() {
        try {
            val toIdealTime =
                timerHelpers.getApiToIdealTimePreferences(timerHelpers.getWeekDayInInt())
            if (timerHelpers.isTimeValidAndNotEmpty(toIdealTime)) {
                val am = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val i = Intent(context, ShutDownReceiverToIdeal::class.java)
                val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_MUTABLE)
                val futureDate: Calendar = Calendar.getInstance()
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("HH:mm:ss")
                val date: Date = sdf.parse(toIdealTime) //give the toIdealTime here
                cal.time = date
                val apiTime =
                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                val systemCurrentTime =
                    futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
                Log.d(
                    TAG, "$apiTime :: $systemCurrentTime"
                )
                if (apiTime > systemCurrentTime) {
                    Log.d(
                        TAG,
                        "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                    )
                    futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
                    futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
                    futureDate.set(Calendar.SECOND, cal[Calendar.SECOND] - 10)
//                    am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)

                    val ac = AlarmClockInfo(futureDate.time.time,pi)
                    am.setAlarmClock(ac,pi)

                    val calendar = Calendar.getInstance()
                    val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    var logTime = sdfData.format(calendar.time)
                    var log = "$logTime Going to turn off screen at $toIdealTime"
                    appLogger.appendLog(log)
                    Log.d(TAG, "$log")
                }
            } else {
                Log.d(TAG, "Invalid Prefs toIdealTime")
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to start display popup",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*turns screen off at set time for toLogic time case*/
    private fun turnOffScreenLogicCase() {
        try {
            val toLogicTime =
                timerHelpers.getApiToLogicTimePreferences(timerHelpers.getWeekDayInInt())
            if (timerHelpers.isTimeValidAndNotEmpty(toLogicTime)) {
                val am = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val i = Intent(context, ShutDownReceiverToLogic::class.java)
                val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_MUTABLE)
                val futureDate: Calendar = Calendar.getInstance()
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("HH:mm:ss")
                val date: Date = sdf.parse(toLogicTime) //give the toLogicTime here
                cal.time = date
                val apiTime =
                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                val systemCurrentTime =
                    futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
                Log.d(
                    TAG, "$apiTime :: $systemCurrentTime"
                )
                if (apiTime > systemCurrentTime) {
                    Log.d(
                        TAG,
                        "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                    )
                    futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
                    futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
                    futureDate.set(Calendar.SECOND, cal[Calendar.SECOND] - 10)
//                    am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)

                    val ac = AlarmClockInfo(futureDate.time.time,pi)
                    am.setAlarmClock(ac,pi)

                    val calendar = Calendar.getInstance()
                    val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    var logTime = sdfData.format(calendar.time)
                    var log = "$logTime Going to turn off screen at $toLogicTime"
                    appLogger.appendLog(log)
                    Log.d(TAG, "$log")
                }
            } else {
                Log.d(TAG, "Invalid Prefs toLogicTime")
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Failed to start display popup",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /*turns screen on at set time*/
    private fun turnOnScreen() {
        try {
            val fromTime = timerHelpers.getApiFromTimePreferences(timerHelpers.getWeekDayInInt())
            if (timerHelpers.isTimeValidAndNotEmpty(fromTime)) {
                val am = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val i = Intent(context, WakeUpReceiver::class.java)
                val pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_MUTABLE)
                val futureDate: Calendar = Calendar.getInstance()
                val cal = Calendar.getInstance()
                val sdf = SimpleDateFormat("HH:mm:ss")
                val date: Date = sdf.parse(fromTime) //give the fromTime here
                cal.time = date
                val apiTime =
                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                val systemCurrentTime =
                    futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
                Log.d(
                    TAG, "$apiTime :: $systemCurrentTime"
                )
                if (apiTime > systemCurrentTime) {
                    Log.d(
                        TAG,
                        "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
                    )
                    futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
                    futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
                    futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])
//                    am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)

                    val ac = AlarmClockInfo(futureDate.time.time,pi)
                    am.setAlarmClock(ac,pi)

                    val calendar = Calendar.getInstance()
                    val sdfData = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                    var logTime = sdfData.format(calendar.time)
                    var log = "$logTime Going to turn on screen at $fromTime"
                    appLogger.appendLog(log)
                    Log.d(TAG, "$log")
                }
            } else {
                Log.d(TAG, "Invalid Prefs fromTime")
            }
        } catch (e: Exception) {
            Log.d(TAG, "wake up alarm failed")
        }
    }
}