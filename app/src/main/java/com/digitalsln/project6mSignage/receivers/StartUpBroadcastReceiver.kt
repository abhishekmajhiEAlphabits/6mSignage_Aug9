package com.digitalsln.project6mSignage.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.digitalsln.project6mSignage.appUtils.AppGlobals
import com.digitalsln.project6mSignage.network.ApiCall

class StartUpBroadcastReceiver : BroadcastReceiver() {
    private lateinit var appGlobals: AppGlobals
    private lateinit var apiCall: ApiCall
    override fun onReceive(context: Context, intent: Intent) {

        Log.d("TvTimer", "we are in boot receiver")
        Toast.makeText(context,"Boot Receiver Called",Toast.LENGTH_LONG).show()
        var i = Intent(context, com.digitalsln.project6mSignage.MainActivity::class.java)
        i.putExtra("boot", "1")
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)

//        if (intent.action.equals(Intent.ACTION_BOOT_COMPLETED)) {
//        }
    }
}