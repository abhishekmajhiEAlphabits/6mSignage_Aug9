package com.digitalsln.project6mSignage.tvLauncher.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.R
import com.digitalsln.project6mSignage.MainActivity

class ShellService : Service(), DeviceConnectionListener {

    private val binder = ShellServiceBinder()
    val listener = ShellListener(this)

    private val currentConnectionMap: HashMap<String, DeviceConnection?> =
        HashMap()

    private var wlanLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val FOREGROUND_PLACEHOLDER_ID = 1
    private val CONN_BASE = 12131
    private val FAILED_BASE = 12111
    private val CHANNEL_ID = "connectionInfo"

    private var foregroundId = 0

    inner class ShellServiceBinder : Binder() {
        fun createConnection(host: String?, port: Int): DeviceConnection {
            val conn = DeviceConnection(listener, host, port)
            listener.addListener(conn, this@ShellService)
            return conn
        }

        fun findConnection(host: String, port: Int): DeviceConnection? {
            val connStr = "$host:$port"
            return currentConnectionMap[connStr]
        }

        fun notifyPausingActivity(devConn: DeviceConnection) {
            devConn.setForeground(false)
        }

        fun notifyResumingActivity(devConn: DeviceConnection) {
            devConn.setForeground(true)
        }

        @RequiresApi(Build.VERSION_CODES.S)
        fun notifyDestroyingActivity(devConn: DeviceConnection) {
            /* If we're pausing before destruction after the connection is closed, remove the failure
			 * notification */
            if (devConn.isClosed()) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(getFailedNotificationId(devConn))
            }
        }

        fun addListener(conn: DeviceConnection?, ls: DeviceConnectionListener?) {
             listener.addListener(conn, ls)
        }

        fun removeListener(conn: DeviceConnection?, ls: DeviceConnectionListener?) {
            listener.removeListener(conn, ls)
        }
    }

    override fun onBind(arg0: Intent?): IBinder {
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        /* Stop the the service if no connections remain */
        if (currentConnectionMap.isEmpty()) {
            stopSelf()
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (foregroundId == 0) {
            // If we're not already running in the foreground, use a placeholder
            // notification until a real connection is established. After connection
            // establishment, the real notification will replace this one.
            startForeground(FOREGROUND_PLACEHOLDER_ID, createForegroundPlaceholderNotification())
        }

        // Don't restart if we've been killed. We will have already lost our connections
        // when we died, so we'll just be running doing nothing if the OS restarted us.
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wlanLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL, "RemoteADBShell:ShellService")
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteADBShell:ShellService")
    }

    override fun onDestroy() {
        if (wlanLock?.isHeld == true) {
            wlanLock?.release()
        }
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getFailedNotificationId(devConn: DeviceConnection?): Int {
        return FAILED_BASE + getConnectionString(devConn).hashCode()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getConnectedNotificationId(devConn: DeviceConnection?): Int {
        return CONN_BASE + getConnectionString(devConn).hashCode()
    }

    private fun createPendingIntentForConnection(devConn: DeviceConnection?): PendingIntent? {
        val appContext: Context = applicationContext
        val i = Intent(appContext, MainActivity::class.java)
        i.putExtra("IP", devConn?.getHost())
        i.putExtra("Port", devConn?.getPort())
        i.action = getConnectionString(devConn)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(appContext, 0, i, flags)
    }

    private fun createPendingIntentToLaunchShellActivity(): PendingIntent? {
        val appContext: Context = applicationContext
        val i = Intent(appContext, MainActivity::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        return PendingIntent.getActivity(appContext, 0, i, flags)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createForegroundPlaceholderNotification(): Notification {
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentTitle("Remote ADB Shell")
            .setContentText("Connecting...")
            .setContentIntent(createPendingIntentToLaunchShellActivity())
//            .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun createConnectionNotification(
        devConn: DeviceConnection?,
        connected: Boolean
    ): Notification {
        val ticker: String
        val message: String
        if (connected) {
            ticker = "Connection Established"
            message = "Connected to " + getConnectionString(devConn)
        } else {
            ticker = "Connection Terminated"
            message = "Connection to " + getConnectionString(devConn) + " failed"
        }
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setTicker("Remote ADB Shell - $ticker")
            .setSmallIcon(R.drawable.ic_launcher)
            .setOnlyAlertOnce(true)
            .setOngoing(connected)
            .setAutoCancel(!connected)

            .setContentTitle("Remote ADB Shell")
            .setContentText(message)
            .setContentIntent(createPendingIntentForConnection(devConn))
           // .setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun updateNotification(devConn: DeviceConnection?, connected: Boolean) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        removeNotification(devConn)
        if (connected) {
            if (foregroundId != 0) {
                /* There's already a foreground notification, so use the normal notification framework */
                nm.notify(
                    getConnectedNotificationId(devConn),
                    createConnectionNotification(devConn, connected)
                )
            } else {
                /* This is the first notification so make it the foreground one */
                foregroundId = getConnectedNotificationId(devConn)
                startForeground(foregroundId, createConnectionNotification(devConn, connected))
            }
        } else if (!devConn?.isForeground()!!) {
            nm.notify(
                getFailedNotificationId(devConn),
                createConnectionNotification(devConn, connected)
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun removeNotification(devConn: DeviceConnection?) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        /* Removing failure notifications is easy */nm.cancel(getFailedNotificationId(devConn))

        /* Connected notifications is a bit more complex */if (getConnectedNotificationId(devConn) == foregroundId) {
            /* We're the foreground notification, so we need to switch in another
			 * notification to take our place */

            /* Search for a new device connection to promote */
            var newConn: DeviceConnection? = null
            for (conn in currentConnectionMap.values) {
                if (devConn === conn) {
                    continue
                } else {
                    newConn = conn
                    break
                }
            }
            if (newConn == null) {
                /* None found, so we're done in foreground */
                stopForeground(true)
                foregroundId = 0
            } else {
                /* Found one, so cancel this guy's original notification
				 * and start it as foreground */
                foregroundId = getConnectedNotificationId(newConn)
                nm.cancel(foregroundId)
                startForeground(foregroundId, createConnectionNotification(newConn, true))
            }
        } else {
            /* This just a normal connected notification */
            nm.cancel(getConnectedNotificationId(devConn))
        }
    }

    private fun getConnectionString(devConn: DeviceConnection?): String {
        return devConn?.getHost() + ":" + devConn?.getPort()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    private fun addNewConnection(devConn: DeviceConnection) {
//        if (currentConnectionMap.isEmpty()) {
//            wakeLock?.acquire(10*60*1000L /*10 minutes*/)
//            wlanLock?.acquire()
//        }
        currentConnectionMap[getConnectionString(devConn)] = devConn
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Synchronized
    private fun removeConnection(devConn: DeviceConnection?) {
        currentConnectionMap.remove(getConnectionString(devConn))
        /* Stop the the service if no connections remain */if (currentConnectionMap.isEmpty()) {
            stopSelf()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {
        devConn?.let {addNewConnection(it)  }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
        /* No notification is displaying here */
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        updateNotification(devConn, false)
        removeConnection(devConn)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun notifyStreamClosed(devConn: DeviceConnection?) {
                updateNotification(devConn, false)
        removeConnection(devConn)
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? {
        Log.v("Shell Service====", "loadAdbCrypto")

        return null
    }

    override fun receivedData(
        devConn: DeviceConnection?, data: ByteArray?, offset: Int,
        length: Int
    ) {
    }

    override fun canReceiveData(): Boolean {
        return false
    }

    override fun isConsole(): Boolean {
        return false
    }

    override fun consoleUpdated(
        devConn: DeviceConnection?,
        console: ConsoleBuffer?
    ) {
    }
}