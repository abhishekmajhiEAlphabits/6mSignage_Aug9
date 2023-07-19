package com.digitalsln.project6mSignage.notificationManager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.PlaylistNotBoundActivity
import com.digitalsln.project6mSignage.R
import com.digitalsln.project6mSignage.network.PlaylistManager
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.qualifiers.ApplicationContext

class FirebaseMessageService : FirebaseMessagingService() {
    private val TAG = "TvTimer"


    private var playlistManager = PlaylistManager(this)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("tokens", "msg received: ${remoteMessage.notification?.title}")
        if (remoteMessage.notification != null) {
            playlistManager.deleteNDownloadData()
            AppPreference(this).sharedIntervalPreference?.edit()?.clear()?.apply()
            sendNotification(remoteMessage)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_MUTABLE
        )

        val channelId = getString(R.string.app_name)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.icon_app)
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText(remoteMessage.notification?.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setChannelId(channelId)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "6mSignage",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}