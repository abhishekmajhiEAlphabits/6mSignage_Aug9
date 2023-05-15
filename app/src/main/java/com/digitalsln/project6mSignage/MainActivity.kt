package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.databinding.ActivityMainBinding
import com.digitalsln.project6mSignage.databinding.HandMadeStartAppDialogBinding
import com.digitalsln.project6mSignage.databinding.PlayModeDialogBinding
import com.digitalsln.project6mSignage.tvLauncher.dialogs.ConfirmDialog
import com.digitalsln.project6mSignage.tvLauncher.dialogs.SpinnerDialog
import com.digitalsln.project6mSignage.tvLauncher.utilities.*
import com.digitalsln.project6mSignage.tvLauncher.utilities.Utils.isNetworkAvailable

class MainActivity : AppCompatActivity(), DeviceConnectionListener {

    private lateinit var dialog: Dialog
    private lateinit var dialogBinding: HandMadeStartAppDialogBinding
    private var _binding: ActivityMainBinding? = null
    private var playModeDialog: Dialog? = null
    private var hostIP: String? = null
    private var connection: DeviceConnection? = null
    private var service: Intent? = null
    private var binder: ShellService.ShellServiceBinder? = null
    private var connectWaiting: SpinnerDialog? = null
    var currentCommand = connectCommand
    var currentButtonLbl: String = ConnectionType.CONNECTED.value
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setCacheSettings()

            loadAdbCrypto()

        } catch (e: Exception) {
            AlertDialog.Builder(this)
                .setMessage(e.message)
                .setPositiveButton("copy") { _, _ ->
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val data = ClipData.newPlainText("error", e.message)
                    clipboard.setPrimaryClip(data)
                    Toast.makeText(this, "data's copied", Toast.LENGTH_LONG).show()
                }
                .setNegativeButton("close", null)
                .show()
        }
    }

    private fun startConnecting() {
        if (isDeveloperOptionEnabled()) {
            initServiceConnection()
        } else {
            Toast.makeText(
                this,
                "You have to enable the Developer option from the Settings",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun isDeveloperOptionEnabled(): Boolean {
        val devOptions = Settings.Secure.getInt(
            this.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
        )
        return devOptions == 1
    }

    private fun initServiceConnection() {
        service = Intent(this, ShellService::class.java)
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
            connection = hostIP?.let { connectOrLookupConnection(it) }
        }
    }

    private val serviceConn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            binder = arg1 as ShellService.ShellServiceBinder
            if (connection != null) {
                binder?.removeListener(connection, this@MainActivity)
            }
            connection = hostIP?.let { connectOrLookupConnection(it) }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            binder = null
        }
    }

    private fun connectOrLookupConnection(host: String): DeviceConnection? {
        var conn: DeviceConnection? = binder?.findConnection(host, PORT)
        if (conn == null) {
            /* No existing connection, so start the connection process */
            conn = startAdbConnection(host, PORT)
        } else {
            /* Add ourselves as a new listener of this connection */
            binder?.removeListener(conn, this)
            binder?.addListener(conn, this)
        }
        return conn
    }

    private fun startAdbConnection(host: String, port: Int): DeviceConnection? {
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

    private fun loadAdbCrypto() {
        /* If we have old RSA keys, just use them */
        val crypto = AdbUtils.readCryptoConfig(filesDir)
        if (crypto == null) {
            Thread {
                AdbUtils.writeNewCryptoConfig(filesDir)
            }.start()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setCacheSettings() {
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (!isNetworkAvailable(this))  //offline
            binding.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        binding.webView.loadUrl(
            AppPreference(this@MainActivity).retrieveValueByKey(
                LAST_WEB_URL,
                REAL_URL
            )
        )
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_UP) {
            showHandMadeStartAppDialog()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun showHandMadeStartAppDialog() {
        dialogBinding = HandMadeStartAppDialogBinding.inflate(layoutInflater)
        dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        setupDialogUi()
        dialog.show()
    }

    private fun setupDialogUi() {
        if (AppPreference(this).isAppDefaultLauncher()) {
            currentCommand = unConnectCommand
            currentButtonLbl = ConnectionType.DIS_CONNECTED.value
        } else {
            currentCommand = connectCommand
            currentButtonLbl = ConnectionType.CONNECTED.value
        }

        dialogBinding.run {
            hostIP?.let {
                tvIpAddress.text = "IP: $it"
            }

            btPlay.setOnClickListener {
                dialog.dismiss()
                AppPreference(this@MainActivity).saveKeyValue(LAST_WEB_URL, REAL_URL)
                binding.webView.loadUrl(REAL_URL)
            }

            btPlayMode.setOnClickListener {
                if (playModeDialog == null) {
                    showPlayModeDialog()
                }
                showPlayModeButtons()
            }

            btResetSettings.setOnClickListener {
                showResetSettingsDialog()
            }

            btConnect.requestFocus()
            btConnect.text = currentButtonLbl
            btConnect.setOnClickListener {
                if (connection == null || connection?.isClosed() == true) {
                    initConnectButtonAction()
                } else {
                    connection?.startConnect()
                }
            }
        }
    }

    private fun initConnectButtonAction() {
        if (isNetworkAvailable(this)) {
            startConnecting()
        } else {
            AlertDialog.Builder(this@MainActivity)
                .setMessage("You have to enable the network connection first")
                .setPositiveButton("Settings") { _, _ ->
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
                .setNegativeButton("close", null)
                .show()
        }
    }

    override fun notifyConnectionEstablished(devConn: DeviceConnection?) {
        connection?.queueCommand(currentCommand)
        runOnUiThread {
            connectWaiting?.dismiss()
            connectWaiting = null
        }
    }

    override fun notifyConnectionFailed(devConn: DeviceConnection?, e: Exception?) {
        connectWaiting?.dismiss()
        connectWaiting = null

        ConfirmDialog.displayDialog(this, "Connection Failed", e!!.message, true)
    }

    override fun notifyStreamFailed(devConn: DeviceConnection?, e: Exception?) {
        Log.v("notifyStreamFailed", e?.localizedMessage ?: "")
        ConfirmDialog.displayDialog(this, "Connection Terminated", e!!.message, true)
    }

    override fun consoleUpdated(devConn: DeviceConnection?, console: ConsoleBuffer?) {
        runOnUiThread { /* We won't need an update again after this */
            /* Redraw the terminal */        console?.updateTextView(object : CommandSuccess {
                override fun onSuccess(type: ConnectionType) {
                    AppPreference(this@MainActivity).setAppDefaultLauncher(type == ConnectionType.CONNECTED)
                    currentButtonLbl = type.value
                    when (type) {
                        ConnectionType.CONNECTED -> {
                            currentCommand = unConnectCommand
                            currentButtonLbl = ConnectionType.DIS_CONNECTED.value
                        }
                        else -> {
                            currentCommand = connectCommand
                            currentButtonLbl = ConnectionType.CONNECTED.value
                        }
                    }
                    dialogBinding.btConnect.text = currentButtonLbl

                }
            })
        }
    }

    private fun showPlayModeDialog() {

        val dialogBinding = PlayModeDialogBinding.inflate(layoutInflater)
//        val choice = loadPlayModePreferences(getPreferences(Context.MODE_PRIVATE))

        playModeDialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnCancelListener {
            }
        }

        dialogBinding.run {
            realButton.setOnClickListener {
                playModeDialog!!.dismiss()
                AppPreference(this@MainActivity).saveKeyValue(REAL_URL, LAST_WEB_URL)
                binding.webView.loadUrl(REAL_URL)
                dialog.dismiss()
            }

            testButton.setOnClickListener {
                playModeDialog!!.dismiss()
                AppPreference(this@MainActivity).saveKeyValue(TEST_URL, LAST_WEB_URL)
                binding.webView.loadUrl(TEST_URL)
                dialog.dismiss()
            }
        }
        playModeDialog?.show()
    }

    // hide loader and show buttons in play mode dialog
    private fun showPlayModeButtons() {
        playModeDialog?.findViewById<ViewGroup>(R.id.rootLayout)?.let {
            PlayModeDialogBinding.bind(it).run {
                loader.isVisible = false
                realCode.isVisible = true
                realButton.isVisible = true
                testCode.isVisible = true
                testButton.isVisible = true
                testButton.isVisible = true
                title.isVisible = true
            }
        }
        Log.d(TAG, "codes is shown")
    }

    private fun showResetSettingsDialog() {
        AlertDialog.Builder(this)
            .setMessage(R.string.dialog_exit_do_you_want_to_close_app)
            .setPositiveButton(R.string.dialog_exit_yes) { _, _ ->
                resetAllSettings()
                //restartApp()
            }.setNegativeButton(R.string.dialog_exit_no) { _, _ ->
                showHandMadeStartAppDialog()
            }
            .create()
            .show()
    }
    private fun resetAllSettings() {
        WebStorage.getInstance().deleteAllData()
        binding.webView.run {
            clearCache(true)
            clearHistory()
            clearFormData()
            clearMatches()
            clearSslPreferences()
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onDestroy() {

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
        hostIP = Utils.getIpAddress(this)

        binding.webView.reload()

        if (binder != null) {
            binder!!.notifyResumingActivity(connection!!)
        }
        super.onResume()
    }

    override fun onPause() {
        /* Tell the service about our UI state change */
        if (binder != null) {
            binder!!.notifyPausingActivity(connection!!)
        }
        super.onPause()
    }

    override fun notifyStreamClosed(devConn: DeviceConnection?) {
        Log.v("notifyStreamClosed", "notifyStreamClosed")

        ConfirmDialog.displayDialog(
            this,
            "Connection Closed",
            "The connection was gracefully closed.",
            true
        )
    }

    override fun loadAdbCrypto(devConn: DeviceConnection?): AdbCrypto? {
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

    companion object {
        const val REAL_URL = "https://6lb.menu/signage"
        const val TEST_URL = "https://test.6lb.menu/signage"
        private const val LAST_WEB_URL = "web.url.last"
        private const val TAG = "MainActivity"
        const val PORT = 5555
        const val connectCommand = "pm disable-user --user 0 com.google.android.tvlauncher\n"
        const val unConnectCommand =
            "pm enable --user 0 com.google.android.tvlauncher\n cmd package set-home-activity com.google.android.tvlauncher/com.google.android.tvlauncher.MainActivity\n"
    }

    enum class ConnectionType(val value: String) {
        CONNECTED("Connect"), DIS_CONNECTED("Disconnect")
    }

    interface CommandSuccess {
        fun onSuccess(type: ConnectionType)
    }
}