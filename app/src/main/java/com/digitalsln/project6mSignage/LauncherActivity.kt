package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.tvLauncher.dialogs.ConfirmDialog
import com.digitalsln.project6mSignage.tvLauncher.dialogs.SpinnerDialog
import com.digitalsln.project6mSignage.tvLauncher.utilities.AdbUtils
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.ConsoleBuffer
import com.digitalsln.project6mSignage.tvLauncher.utilities.DeviceConnection
import com.digitalsln.project6mSignage.tvLauncher.utilities.DeviceConnectionListener
import com.digitalsln.project6mSignage.tvLauncher.utilities.ShellService
import com.digitalsln.project6mSignage.tvLauncher.utilities.Utils

class LauncherActivity : FragmentActivity(), DeviceConnectionListener {

    private lateinit var hostIP: String
    private var connection: DeviceConnection? = null
    private var service: Intent? = null
    private var binder: ShellService.ShellServiceBinder? = null
    private var connectWaiting: SpinnerDialog? = null
    lateinit var ipAddress: TextView
    lateinit var connectBt: Button
    var isConnecting = true

    companion object {
        const val PORT = 5555
        const val connectCommand = "pm disable-user --user 0 com.google.android.tvlauncher\n"
        const val unConnectCommand =
            "pm enable --user 0 com.google.android.tvlauncher\n cmd package set-home-activity com.google.android.tvlauncher/com.google.android.tvlauncher.MainActivity\n"
    }

    var currentCommand = connectCommand

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        ipAddress = findViewById(R.id.ip_address)
        connectBt = findViewById(R.id.bt_connect)

        if (AppPreference(this).isAppDefaultLauncher()) {
            currentCommand = unConnectCommand
            connectBt.text = "Disconnect"
        } else {
            connectBt.text = "Connect"
            currentCommand = connectCommand
        }

        loadAdbCrypto()

        Handler().postDelayed({
            if (isDeveloperOptionEnabled()) {
                startConnect()
            } else {
                Toast.makeText(
                    this,
                    "You have to enable the Developer option from the Settings",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, 1500)
    }

    private fun loadAdbCrypto() {
        /* If we have old RSA keys, just use them */
        val crypto = AdbUtils.readCryptoConfig(filesDir)
        if (crypto == null) {
            Thread {
                AdbUtils.writeNewCryptoConfig(filesDir)
            }.start()
        }
    }

    private fun connectOrLookupConnection(host: String): DeviceConnection? {
        var conn: DeviceConnection? = binder?.findConnection(host, PORT)
        if (conn == null) {
            /* No existing connection, so start the connection process */
            conn = startConnection(host, PORT)
        } else {
            /* Add ourselves as a new listener of this connection */
            binder?.addListener(conn, this)
        }
        return conn
    }

    private fun startConnection(host: String, port: Int): DeviceConnection? {
        /* Display the connection progress spinner */
        connectWaiting = SpinnerDialog.displayDialog(
            this, "Connecting to $host:$port",
            """
            Please make sure the target device has network ADB enabled.

            You may need to accept a prompt on the target device if you are connecting to it for the first time from this device.
            """.trimIndent(), true
        )

        /* Create the connection object */
        connection = binder!!.createConnection(host, port)

        /* Add this activity as a connection listener */binder!!.addListener(connection, this)

        /* Begin the async connection process */connection?.startConnect()
        return connection
    }

    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        when (event!!.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (isConnecting) {
                    isConnecting = false
                    connection?.queueCommand(currentCommand)
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun startConnect() {
        service = Intent(this, ShellService::class.java)
        hostIP = Utils.getIpAddress(this)
        ipAddress.text ="IP: $hostIP"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service)
        } else {
            startService(service)
        }

        if (binder == null) {
            /* Bind the service if we're not bound already. After binding, the callback will
			 * perform the initial connection. */
            applicationContext.bindService(service, serviceConn, BIND_AUTO_CREATE)
        } else {

            /* We're already bound, so do the connect or lookup immediately */
            if (connection != null) {
                binder!!.removeListener(connection, this)
            }
            connection = connectOrLookupConnection(hostIP)
        }
    }

    private fun isDeveloperOptionEnabled(): Boolean {
        val devOptions = Settings.Secure.getInt(
            this.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        )
        return devOptions == 1
    }

    private val serviceConn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            binder = arg1 as ShellService.ShellServiceBinder
            if (connection != null) {
                binder?.removeListener(connection, this@LauncherActivity)
            }
            connection = connectOrLookupConnection(hostIP)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            binder = null
        }
    }


    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {
        /* Save the command history first */
        if (binder != null && connection != null) {
            /* Tell the service about our impending doom */
            binder?.notifyDestroyingActivity(connection!!)

            /* Dissociate our activity's listener */binder!!.removeListener(connection, this)
        }

        /* If the connection hasn't actually finished yet,
		 * close it before terminating */if (connectWaiting != null) {
            AdbUtils.safeAsyncClose(connection)
        }

        /* Unbind from the service since we're going away */if (binder != null) {
            applicationContext.unbindService(serviceConn)
        }
        ConfirmDialog.closeDialogs()
        SpinnerDialog.closeDialogs()
        super.onDestroy()
    }

    override fun onResume() {
        /* Tell the service about our UI state change */
        if (binder != null) {
            binder!!.notifyResumingActivity(connection!!)
        }
        isConnecting = true
        super.onResume()
    }

    override fun onPause() {
        /* Tell the service about our UI state change */
        if (binder != null) {
            binder!!.notifyPausingActivity(connection!!)
        }
        super.onPause()
    }

    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {
        runOnUiThread {
            connectWaiting?.dismiss()
            connectWaiting = null
            ipAddress.text ="IP: $hostIP"

            println("ADB SHELL==== Listener is updated")
        }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
        connectWaiting?.dismiss()
        connectWaiting = null

        ConfirmDialog.displayDialog(this, "Connection Failed", e!!.message, true)
    }

    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        ConfirmDialog.displayDialog(this, "Connection Terminated", e!!.message, true)
    }

    override fun notifyStreamClosed(devConn: DeviceConnection?) {
        ConfirmDialog.displayDialog(
            this,
            "Connection Closed",
            "The connection was gracefully closed.",
            true
        )
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? {
        Log.v("Launcher Activity====", "loadAdbCrypto")

        return AdbUtils.readCryptoConfig(filesDir)
    }

    override fun canReceiveData(): Boolean {
        /* We just handle console updates */return false
    }

    override fun receivedData(
        devConn: DeviceConnection?,
        data: ByteArray?,
        offset: Int,
        length: Int
    ) {
    }

    override fun isConsole(): Boolean {
        return true
    }

    override fun consoleUpdated(devConn: DeviceConnection?, console: ConsoleBuffer?) {
        runOnUiThread { /* We won't need an update again after this */
            /* Redraw the terminal */        console?.updateTextView(
                ipAddress,
                object : CommandSuccess {
                    override fun onSuccess() {
                        AppPreference(this@LauncherActivity).setAppDefaultLauncher(true)
                        startActivity(Intent(this@LauncherActivity, MainActivity::class.java))
                        finish()
                    }

                    override fun onHomePressed() {
                        AppPreference(this@LauncherActivity).setAppDefaultLauncher(false)

                        ConfirmDialog.displayDialog(
                            this@LauncherActivity,
                            "Back to android home",
                            "Press the Home button to got to the Android Home Launcher.",
                            true
                        )
                    }
                })
        }
    }
}

interface CommandSuccess {
    fun onSuccess()
    fun onHomePressed()
}