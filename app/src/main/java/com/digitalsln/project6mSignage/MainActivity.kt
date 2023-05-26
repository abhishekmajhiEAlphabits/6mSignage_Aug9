package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.databinding.ActivityMainBinding
import com.digitalsln.project6mSignage.databinding.HandMadeStartAppDialogBinding
import com.digitalsln.project6mSignage.databinding.PlayModeDialogBinding
import com.digitalsln.project6mSignage.model.TimeData
import com.digitalsln.project6mSignage.network.ApiClient
import com.digitalsln.project6mSignage.network.ApiInterface
import com.digitalsln.project6mSignage.tvLauncher.dialogs.ConfirmDialog
import com.digitalsln.project6mSignage.tvLauncher.dialogs.SpinnerDialog
import com.digitalsln.project6mSignage.tvLauncher.utilities.*
import com.digitalsln.project6mSignage.tvLauncher.utilities.Utils.isNetworkAvailable
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*


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
    private var isTimerSet = false
    private var defaultValue: String? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var powerManager: PowerManager;

    @RequiresApi(Build.VERSION_CODES.M)
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

        try {
            checkOverlayPermission();
            checkWritePermission()

            defaultValue =
                Settings.System.getString(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)

            AppPreference(this@MainActivity).saveDefaultTimeOut(defaultValue!!,"TIME_OUT")

            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE, "appname::WakeLock"
            )

            wakeLock.acquire()

            binding.webView.setWebChromeClient(object : WebChromeClient() {
                override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                    binding.webView.evaluateJavascript("javascript:window.localStorage.getItem('signageScreenCode')",
                        ValueCallback<String?> { s ->
                            Log.e("abhiCode", s!!)
                            AppPreference(this@MainActivity).saveExternalScreenCode(
                                s,
                                "EXTERNAL_SCREEN_CODE"
                            )
                        })
                    super.onConsoleMessage(message, lineNumber, sourceID)
                }
            })

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
            Log.d("abhi", "error : $e")
        }

        showHandMadeStartAppDialog()
    }

    /* To ask user to grant the Overlay permission
     */
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // send user to the device settings
                val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(myIntent)
            }
        }
    }

    /* To ask user to grant the Write System Settings permission
    */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkWritePermission() {
        if (!Settings.System.canWrite(this)) {
            // send user to the device settings
            val myIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            startActivity(myIntent)
        }

    }

    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            isTimerSet = true

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(applicationContext, DisplayOverlayReceiver::class.java)
            i.action = "com.example.androidtvdemo.START_ALARM"
            val pi = PendingIntent.getBroadcast(applicationContext, 0, i, 0);

            val futureDate: Calendar = Calendar.getInstance()
//            futureDate.add(Calendar.SECOND, 10)  //timerValue - the user selected timerValue

            val fromTime =
                AppPreference(this@MainActivity).retrieveFromTime("FROM_TIME", "FROM_TIME")
            val time = futureDate.timeInMillis
            Log.d(
                "abhi", "$time")

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse("19:06:00") //give the fromTime here
            cal.time = date

            Log.d(
                "abhi",
                "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
            )
            futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
            futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
            futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV2() {
        try {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            isTimerSet = true

            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val i = Intent(applicationContext, DisplayOverlayReceiver::class.java)
            i.action = "com.example.androidtvdemo.START_ALARM"
            val pi = PendingIntent.getBroadcast(applicationContext, 0, i, 0);

            val futureDate: Calendar = Calendar.getInstance()
//            futureDate.add(Calendar.SECOND, 10)  //timerValue - the user selected timerValue

            val fromTime =
                AppPreference(this@MainActivity).retrieveFromTime("FROM_TIME", "FROM_TIME")
            val time = futureDate.timeInMillis
            Log.d(
                "abhi", "$time")

            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm:ss")
            val date: Date = sdf.parse("19:31:00") //give the fromTime here
            cal.time = date

            Log.d(
                "abhi",
                "${cal[Calendar.HOUR_OF_DAY]} :: ${cal[Calendar.MINUTE]}:: ${cal[Calendar.SECOND]}"
            )
            futureDate.set(Calendar.HOUR_OF_DAY, cal[Calendar.HOUR_OF_DAY])
            futureDate.set(Calendar.MINUTE, cal[Calendar.MINUTE])
            futureDate.set(Calendar.SECOND, cal[Calendar.SECOND])

            am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun startScheduler() {
        val localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
            "LOCAL_SCREEN_CODE",
            "LOCAL_CODE"
        )
        val externalCode = AppPreference(this@MainActivity).retrieveExternalScreenCode(
            "EXTERNAL_SCREEN_CODE",
            "EXTERNAL_CODE"
        )
        if (localScreenCode != externalCode) {
            AppPreference(this@MainActivity).setAlarmCancelled(true)
            callApi()
            Log.d("abhi", "inside compare : $localScreenCode : $externalCode")

            //here give the alarm manager for everyday api hit timer after every 24hrs
//            scheduleApiCallTimer()
            AppPreference(this@MainActivity).saveLocalScreenCode("LOCAL_SCREEN_CODE", externalCode)
        }
    }

    private fun scheduleApiCallTimer() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(applicationContext, ScheduleApiTimerReceiverOne::class.java)
        i.action = "com.example.androidtvdemo.START_ALARM"
        val pi = PendingIntent.getBroadcast(applicationContext, 0, i, 0);

        val futureDate: Calendar = Calendar.getInstance()
        futureDate.add(Calendar.SECOND, 10)
//        futureDate.set(Calendar.HOUR_OF_DAY, 17)
//        futureDate.set(Calendar.MINUTE, 58)
//        futureDate.set(Calendar.SECOND, 0)

        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi);
    }

    private fun callApi() {
        var localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
            "LOCAL_SCREEN_CODE",
            "LOCAL_CODE"
        )
        ApiClient.client().create(ApiInterface::class.java)
            .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                override fun onResponse(
                    call: Call<List<TimeData>>,
                    response: Response<List<TimeData>>
                ) {
                    if (response.isSuccessful) {
                        val calendar = Calendar.getInstance()
                        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
                        AppPreference(this@MainActivity).saveFromTime(
                            response.body()!![day].from,
                            "FROM_TIME"
                        )
                        AppPreference(this@MainActivity).saveToTime(
                            response.body()!![day].to,
                            "TO_TIME"
                        )
                        Log.d("abhi", "${response.body()}")
                        Log.d("abhi", "$day")
                        Log.d("abhi", "${response.body()!![day].from}")
                        Log.d("abhi", "${response.body()!![day].to}")
                        var alarmCancelStatus = AppPreference(this@MainActivity).isAlarmCancelled()
//                        if (alarmCancelStatus) {
//                            cancelMultipleAlarms()
//                            lockTV() //new timer set after cancelling old alarms
//                            Log.d("abhi", "inside if")
//                        } else {
//                            lockTV()
//                            Log.d("abhi", "inside else")
//                        }
                        lockTV()
                        AppPreference(this@MainActivity).setAlarmCancelled(false)
                    } else {
                        Log.d("abhi", "Failed")
                    }
                }

                override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                    Log.d("abhi", "$t")
                }
            })
    }

    private fun callApi2() {
        var localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
            "LOCAL_SCREEN_CODE",
            "LOCAL_CODE"
        )
        ApiClient.client().create(ApiInterface::class.java)
            .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                override fun onResponse(
                    call: Call<List<TimeData>>,
                    response: Response<List<TimeData>>
                ) {
                    if (response.isSuccessful) {
                        val calendar = Calendar.getInstance()
                        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
                        AppPreference(this@MainActivity).saveFromTime(
                            response.body()!![day].from,
                            "FROM_TIME"
                        )
                        AppPreference(this@MainActivity).saveToTime(
                            response.body()!![day].to,
                            "TO_TIME"
                        )
                        Log.d("abhi", "${response.body()}")
                        Log.d("abhi", "$day")
                        Log.d("abhi", "${response.body()!![day].from}")
                        Log.d("abhi", "${response.body()!![day].to}")
                        var alarmCancelStatus = AppPreference(this@MainActivity).isAlarmCancelled()
//                        if (alarmCancelStatus) {
////                            cancelMultipleAlarms()
//                            lockTV() //new timer set after cancelling old alarms
//                            Log.d("abhi", "inside if")
//                        } else {
//                            lockTV()
//                            Log.d("abhi", "inside else")
//                        }
                        lockTV2()
                        AppPreference(this@MainActivity).setAlarmCancelled(false)
                    } else {
                        Log.d("abhi", "Failed")
                    }
                }

                override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                    Log.d("abhi", "$t")
                }
            })
    }

//    private fun cancelAlarms() {
//        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val i = Intent(applicationContext, DisplayOverlayReceiver::class.java)
//        i.action = "com.example.androidtvdemo.START_ALARM"
//        val pi =
//            PendingIntent.getBroadcast(applicationContext, 0, i, 0);
//        am.cancel(pi)
//
//        Log.d("abhi", "inside cancel")
//    }

    fun cancelMultipleAlarms() {
        //if alarm is set in past time then it will execute even after cancel alarm
        val size = 4
        val alarmManagers = arrayOfNulls<AlarmManager>(size)
        val intents = arrayOf<Intent>(Intent(applicationContext, DisplayOverlayReceiver::class.java),
            Intent(applicationContext, ShutDownReceiver::class.java),
            Intent(applicationContext, TimeOutReceiver::class.java),
            Intent(applicationContext, WakeUpReceiver::class.java))
        for (i in 0 until size) {
            alarmManagers[i] = getSystemService(ALARM_SERVICE) as AlarmManager
//            intents[i] = Intent(applicationContext, AlarmReceiver::class.java)
            //we don't need to any flags here so it is zero (0)
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0,
                intents[i]!!, PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManagers[i]!!.cancel(pendingIntent)
            Log.d("abhi", "inside cancel")
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
                startScheduler()
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

            btMakeAppDefault.requestFocus()
            //btMakeAppDefault.text = currentButtonLbl
            btMakeAppDefault.setOnClickListener {
                if (connection == null || connection?.isClosed() == true) {
                    initConnectButtonAction()
                } else {
                    connection?.startConnect()
                }
            }

            btOpenTvSettings.setOnClickListener {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }

            btRefresh.setOnClickListener {
                AppPreference(this@MainActivity).setAlarmCancelled(true)
                cancelMultipleAlarms()
                callApi2()//dummy api
                //here lockTv() is called to schedule lock after cancel all alarms on refresh button
                //for the current day because for the next day api will be called and lockTv also
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
                    //dialogBinding.btMakeAppDefault.text = currentButtonLbl

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
                //btPlay.callOnClick()
                dialog.dismiss()
                AppPreference(this@MainActivity).saveKeyValue(LAST_WEB_URL, REAL_URL)
                binding.webView.loadUrl(REAL_URL)
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        /* Tell the service about our UI state change */
        hostIP = Utils.getIpAddress(this)

        binding.webView.reload()

        if (binder != null) {
            binder!!.notifyResumingActivity(connection!!)
        }
        super.onResume()

        try {
            checkOverlayPermission();
            checkWritePermission()

            if (isTimerSet) {
                Log.d(TAG, "Timer set..onResume")
                if (!wakeLock.isHeld) {
                    wakeLock.acquire()
                    Log.d(TAG, "Timer set..onResume..wakelock acquire")
                }
            } else {
                Settings.System.putString(
                    contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    defaultValue
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onPause() {
        /* Tell the service about our UI state change */
        if (binder != null) {
            binder!!.notifyPausingActivity(connection!!)
        }
        super.onPause()

        try {
            checkOverlayPermission();
            checkWritePermission()

            if (isTimerSet) {
                Log.d(TAG, "Timer set..onPause")
            } else {
                Settings.System.putString(
                    contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    defaultValue
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Permissions not granted",
                Toast.LENGTH_SHORT
            ).show()
        }
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