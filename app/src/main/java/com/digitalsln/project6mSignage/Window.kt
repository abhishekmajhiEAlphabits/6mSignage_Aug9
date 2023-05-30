package com.digitalsln.project6mSignage

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.*
import android.widget.Toast
import com.digitalsln.project6mSignage.receivers.ShutDownReceiver
import java.util.*

/**
 * overlay screen to show on turn off countdown
 */
class Window(context: Context) {

    // declaring required variables
    private var context: Context? = null
    private var mView: View? = null
    private var mParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    private var layoutInflater: LayoutInflater? = null
    private var am: AlarmManager? = null
    private var pi: PendingIntent? = null

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
    }

    fun open() {
        try {
            // check if the view is already
            // inflated or present in the window
            if (mView!!.windowToken == null) {
                if (mView!!.parent == null) {
                    mWindowManager!!.addView(mView, mParams)
                    try {
                        am = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val i = Intent(context, ShutDownReceiver::class.java)
                        pi = PendingIntent.getBroadcast(context, 0, i, 0)
                        val futureDate = Calendar.getInstance()
                        futureDate.add(Calendar.SECOND, 10)
                        am!!.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to start display popup",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

    fun close() {
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
}