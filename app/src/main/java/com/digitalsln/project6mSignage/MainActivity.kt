package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.*
import android.widget.RadioButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.cgutman.adblib.AdbCrypto
import com.digitalsln.project6mSignage.appUtils.AppLogger
import com.digitalsln.project6mSignage.appUtils.TimerHelpers
import com.digitalsln.project6mSignage.databinding.ActivityMainBinding
import com.digitalsln.project6mSignage.databinding.HandMadeStartAppDialogBinding
import com.digitalsln.project6mSignage.databinding.PlayModeDialogBinding
import com.digitalsln.project6mSignage.databinding.PlaySettingsDialogBinding
import com.digitalsln.project6mSignage.model.TimeData
import com.digitalsln.project6mSignage.network.ApiClient
import com.digitalsln.project6mSignage.network.ApiInterface
import com.digitalsln.project6mSignage.receivers.*
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
    private var playSettingsDialog: Dialog? = null
    private var hostIP: String? = null
    private var connection: DeviceConnection? = null
    private var service: Intent? = null
    private var binder: ShellService.ShellServiceBinder? = null
    private var connectWaiting: SpinnerDialog? = null
    var currentCommand = connectCommand
    var currentButtonLbl: String = ConnectionType.CONNECTED.value
    private val binding get() = _binding!!
    private var defaultValue: String? = null
    private lateinit var powerManager: PowerManager
    private lateinit var timerHelpers: TimerHelpers
    private lateinit var networkChangeReceiver: NetworkChangeReceiver
    private lateinit var appLogger: AppLogger

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
            checkOverlayPermission()
            checkWritePermission()
            getStoragePermission()

            /* Fetch and sets the default screen timeOut value in preferences */
            defaultValue =
                Settings.System.getString(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
            AppPreference(this@MainActivity).saveDefaultTimeOut(defaultValue!!, Constants.timeOut)

            /* creates wakelock and acquires it to keep screen on */
            powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE, "appname::WakeLock"
            )
            wakeLock.acquire()

            appLogger = AppLogger()
            timerHelpers = TimerHelpers(applicationContext)
            networkChangeReceiver = NetworkChangeReceiver()
            registerNetworkBroadcastForNougat()


            /* Fetches the screen code from the browser localStorage and stores in preferences */
            binding.webView.setWebChromeClient(object : WebChromeClient() {
                override fun onConsoleMessage(message: String, lineNumber: Int, sourceID: String) {
                    binding.webView.evaluateJavascript("javascript:window.localStorage.getItem('signageScreenCode')",
                        ValueCallback<String?> { s ->
                            var s = s.replace("\"", "")
                            AppPreference(this@MainActivity).saveExternalScreenCode(
                                s,
                                Constants.externalScreenCode
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
            Log.d(TAG2, "error : $e")
        }

        showHandMadeStartAppDialog()
        syncTimer()
    }

    /**
     * Below function is used to sync the timer in case when app opens
     * -> This scenario will be help when device reboots & off when call api at 12:00 am and due to OFF it was not called.
     */
    private fun syncTimer()
    {
        refreshButtonCall()
        scheduleApiCallTimer()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getStoragePermission(): Boolean {
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Log.d("abhi", "Permission is granted")
            //File write logic here
            return true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1234
            )
            return false
        }
    }

    /* To ask user to grant the Overlay permission */
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // send user to the device settings
                val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                startActivity(myIntent)
            }
        }
    }

    /* To ask user to grant the Write System Settings permission */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun checkWritePermission() {
        if (!Settings.System.canWrite(this)) {
            // send user to the device settings
            val myIntent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            startActivity(myIntent)
        }

    }

    @SuppressLint("InvalidWakeLockTag", "ShortAlarm")
    private fun lockTV(day: Int) {
        try {
            isTimerSet = true

            val futureDate: Calendar = Calendar.getInstance()

            /* gets the times from preferences and compares it with system current time */
            var apiFromTime = timerHelpers.getApiFromTime(day)
            var apiToIdealTime = timerHelpers.getApiToIdealTime(day)
            var apiToLogicTime = timerHelpers.getApiToLogicTime(day)
            val calApiFromTime =
                apiFromTime[Calendar.HOUR_OF_DAY] * 3600 + apiFromTime[Calendar.MINUTE] * 60 + apiFromTime[Calendar.SECOND]
            val calApiToIdealTime =
                apiToIdealTime[Calendar.HOUR_OF_DAY] * 3600 + apiToIdealTime[Calendar.MINUTE] * 60 + apiToIdealTime[Calendar.SECOND]
            val calApiToLogicTime =
                apiToLogicTime[Calendar.HOUR_OF_DAY] * 3600 + apiToLogicTime[Calendar.MINUTE] * 60 + apiToLogicTime[Calendar.SECOND]
            val systemCurrentTime =
                futureDate[Calendar.HOUR_OF_DAY] * 3600 + futureDate[Calendar.MINUTE] * 60 + futureDate[Calendar.SECOND]
            Log.d(
                TAG2,
                "$calApiFromTime :: $calApiToIdealTime :: $calApiToLogicTime :: $systemCurrentTime"
            )

            var hrs = futureDate[Calendar.HOUR_OF_DAY]
            var mins = futureDate[Calendar.MINUTE]
            var secs = futureDate[Calendar.SECOND]


            /* starts service to initiate handle screen on/off*/
            startService()

            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }


        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Lock failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    /*service to start alarm manager for screen on/off*/
    private fun startService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // check if the user has already granted
            // the Draw over other apps permission
            if (Settings.canDrawOverlays(this)) {
                // start the service based on the android version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(
                        Intent(
                            this,
                            ForegroundService::class.java
                        )
                    )
                } else {
                    startService(Intent(this, ForegroundService::class.java))
                }
            }
        } else {
            startService(Intent(this, ForegroundService::class.java))
        }
    }

    /* starts scheduler for calling the api everyday at scheduled time */
    private fun startScheduler() {
        try {
            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }

            /* gets the local screen code and screen code from browser */
            val localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
                Constants.localScreenCode,
                Constants.defaultLocalScreenCode
            )
            val externalCode = AppPreference(this@MainActivity).retrieveExternalScreenCode(
                Constants.externalScreenCode,
                Constants.defaultExternalScreenCode
            )

            /* checks if the app is run first time */
            val isFirstRun = AppPreference(this@MainActivity).isFirstTimeRun()
            if (isFirstRun) {
                /* runs on first run of the app after install and starts the function to call the api everyday */
                AppPreference(this@MainActivity).saveLocalScreenCode(
                    externalCode,
                    Constants.localScreenCode
                )

                //alarm manager for everyday api hit timer after every 24hrs
                scheduleApiCallTimer()
                callApi()
                AppPreference(this@MainActivity).setFirstTimeRun(false)
            } else {
                if (localScreenCode != externalCode) {
                    AppPreference(this@MainActivity).saveLocalScreenCode(
                        externalCode,
                        Constants.localScreenCode
                    )

                    /* if saved screen code is not same to the code from the browser then cancels all the alarms and call api again */
                    cancelMultipleAlarms()
                    callApi()
                } else {
                    Log.d(TAG2, "equal")
                }
                Log.d(TAG2, "!first")
            }
        } catch (e: Exception) {
            Log.d(TAG2, "$e")
        }
    }

    /* alarm manager to initialize and call the api */
    private fun scheduleApiCallTimer() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(applicationContext, ApiCallSchedulerInitReceiver::class.java)
        val pi = PendingIntent.getBroadcast(applicationContext, 0, i, 0)
        val futureDate: Calendar = Calendar.getInstance()
        futureDate.set(Calendar.HOUR_OF_DAY, 23)
        futureDate.set(Calendar.MINUTE, 59)
        futureDate.set(Calendar.SECOND, 0)
//        am.setExact(AlarmManager.RTC_WAKEUP, futureDate.time.time, pi)
        val ac = AlarmManager.AlarmClockInfo(futureDate.time.time, pi)
        am.setAlarmClock(ac, pi)
    }


    /*calls api and stores the fromTime and toTime in the preferences(fromTime,toIdeal,toLogic)
    accordingly
     */
    private fun callApi() {
        try {
            val cal = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
            var logTime = sdf.format(cal.time)
            var log = "$logTime Calling backend API to get timings"
            Log.d(TAG2, "$log")
            appLogger.appendLog(log)
            var localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
                Constants.localScreenCode,
                Constants.defaultLocalScreenCode
            )

            ApiClient.client().create(ApiInterface::class.java)
                .getTime(localScreenCode).enqueue(object : Callback<List<TimeData>> {
                    override fun onResponse(
                        call: Call<List<TimeData>>,
                        response: Response<List<TimeData>>
                    ) {
                        if (response.isSuccessful) {
//                            Toast.makeText(applicationContext, "" + response, Toast.LENGTH_LONG)
//                                .show()
                            Toast.makeText(
                                applicationContext,
                                "Refreshed Successfully!",
                                Toast.LENGTH_LONG
                            )
                                .show()

                            if (response.body() != null) {
                                val cal = Calendar.getInstance()
                                val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                var logTime = sdf.format(cal.time)
                                var log = "$logTime API returned response successfully"
                                Log.d(TAG2, "$log")
                                appLogger.appendLog(log)

                                for (i in 0..6) {
                                    var fromTime = "" //fromTime - screen on time
                                    var toTime = "" //toTime - screen off time
                                    var fromTimeSeconds = 0
                                    var toTimeSeconds = 0
                                    if (i == 6) {
                                        if (response.body()!![i].day != null && response.body()!![0].day != null) {
                                            var dayName =
                                                timerHelpers.getWeekDay(response.body()!![i].day)
                                            if (response.body()!![i] != null && response.body()!![i].from != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].from) //give the fromTime here
                                                cal.time = date
                                                val apiFromTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                fromTimeSeconds = apiFromTimeSeconds
                                                fromTime = response.body()!![i].from
                                            }

                                            if (response.body()!![i] != null && response.body()!![i].to != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].to) //give the toTime here
                                                cal.time = date
                                                val apiToTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                toTimeSeconds = apiToTimeSeconds
                                                toTime = response.body()!![i].to
                                            }
                                            if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                                if (timerHelpers.validTime(fromTime) && timerHelpers.validTime(
                                                        toTime
                                                    )
                                                ) {
                                                    if (fromTimeSeconds > toTimeSeconds) {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![0].day)
                                                        AppPreference(this@MainActivity).saveToLogicTime(
                                                            toTime,
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveToIdealTime(
                                                            "00:00:00",
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    } else {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![0].day)
                                                        AppPreference(this@MainActivity).saveToLogicTime(
                                                            "00:00:00",
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveToIdealTime(
                                                            toTime,
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    }
                                                    val cal = Calendar.getInstance()
                                                    val sdf =
                                                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                                    var logTime = sdf.format(cal.time)
                                                    var log = "$logTime timings was stored locally"
                                                    Log.d(TAG2, "$log")
                                                    appLogger.appendLog(log)
                                                    Log.d(TAG2, "${response.body()}")
                                                } else {
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Invalid Time",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        if (response.body()!![i].day != null && response.body()!![i + 1].day != null) {
                                            var dayName =
                                                timerHelpers.getWeekDay(response.body()!![i].day)
                                            if (response.body()!![i] != null && response.body()!![i].from != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].from) //give the fromTime here
                                                cal.time = date
                                                val apiFromTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                fromTimeSeconds = apiFromTimeSeconds
                                                fromTime = response.body()!![i].from
                                            }

                                            if (response.body()!![i] != null && response.body()!![i].to != null) {
                                                val cal = Calendar.getInstance()
                                                val sdf = SimpleDateFormat("HH:mm:ss")
                                                val date: Date =
                                                    sdf.parse(response.body()!![i].to) //give the toTime here
                                                cal.time = date
                                                val apiToTimeSeconds =
                                                    cal[Calendar.HOUR_OF_DAY] * 3600 + cal[Calendar.MINUTE] * 60 + cal[Calendar.SECOND]
                                                toTimeSeconds = apiToTimeSeconds
                                                toTime = response.body()!![i].to
                                            }
                                            if (fromTime.isNotEmpty() && toTime.isNotEmpty()) {
                                                if (timerHelpers.validTime(fromTime) && timerHelpers.validTime(
                                                        toTime
                                                    )
                                                ) {
                                                    if (fromTimeSeconds > toTimeSeconds) {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![i + 1].day)
                                                        AppPreference(this@MainActivity).saveToLogicTime(
                                                            toTime,
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveToIdealTime(
                                                            "00:00:00",
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    } else {
                                                        var nextDayName =
                                                            timerHelpers.getWeekDay(response.body()!![i + 1].day)
                                                        AppPreference(this@MainActivity).saveToLogicTime(
                                                            "00:00:00",
                                                            "$nextDayName-${Constants.toLogicTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveFromTime(
                                                            fromTime,
                                                            "$dayName-${Constants.fromTime}"
                                                        )
                                                        AppPreference(this@MainActivity).saveToIdealTime(
                                                            toTime,
                                                            "$dayName-${Constants.toIdealTime}"
                                                        )
                                                    }
                                                    val cal = Calendar.getInstance()
                                                    val sdf =
                                                        SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
                                                    var logTime = sdf.format(cal.time)
                                                    var log = "$logTime timings was stored locally"
                                                    Log.d(TAG2, "$log")
                                                    appLogger.appendLog(log)
                                                    Log.d(TAG2, "${response.body()}")
                                                } else {
                                                    Toast.makeText(
                                                        applicationContext,
                                                        "Invalid Time",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }

                                }
                            }

                        } else {
                            Toast.makeText(
                                applicationContext,
                                "Failed to refresh! Please try again",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d(TAG2, "Failed")
                        }
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api
                        with the stored data if there is any */
                        lockTvDayBase()
                    }

                    override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                        Toast.makeText(
                            applicationContext,
                            "Failed to refresh! Please try again",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.d(TAG2, "$t")
                        /* call api to set alarm manager for screen off/on is called even if there is no internet or failed api
                        with the stored data if there is any */
                        lockTvDayBase()
                    }
                })
        } catch (e: Exception) {
            Toast.makeText(
                applicationContext,
                "Failed to refresh! Please try again",
                Toast.LENGTH_LONG
            ).show()
            Log.d(TAG2, "$e")
        }
    }

    private fun lockTvDayBase() {
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
        lockTV(day)
        Log.d(
            TAG2,
            "another day :: fromTime -- ${timerHelpers.getApiFromTimePreferences(4)} :: toIdealTime -- ${
                timerHelpers.getApiToIdealTimePreferences(5)
            } :: toLogicTime -- ${timerHelpers.getApiToLogicTimePreferences(4)}"
        )
    }

    private fun registerNetworkBroadcastForNougat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(
                networkChangeReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            registerReceiver(
                networkChangeReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }


    /* cancels all previously scheduled alarms */
    private fun cancelMultipleAlarms() {
        val size = 4
        val alarmManagers = arrayOfNulls<AlarmManager>(size)
        val intents = arrayOf<Intent>(
            Intent(applicationContext, ShutDownReceiverToIdeal::class.java),
            Intent(applicationContext, ShutDownReceiverToLogic::class.java),
            Intent(applicationContext, TimeOutReceiver::class.java),
            Intent(applicationContext, WakeUpReceiver::class.java)
        )
        for (i in 0 until size) {
            alarmManagers[i] = getSystemService(ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext, 0,
                intents[i]!!, PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmManagers[i]!!.cancel(pendingIntent)
            Log.d(TAG2, "cancelled")
        }
    }

    /*calls the api when refresh button is clicked*/
    private fun refreshButtonCall() {
        try {
            /* gets the local screen code and screen code from browser */
            val localScreenCode = AppPreference(this@MainActivity).retrieveLocalScreenCode(
                Constants.localScreenCode,
                Constants.defaultLocalScreenCode
            )
            val externalCode = AppPreference(this@MainActivity).retrieveExternalScreenCode(
                Constants.externalScreenCode,
                Constants.defaultExternalScreenCode
            )

            /*if local screen code is not same to the external code then save the new code in preferences*/
            if (localScreenCode != externalCode) {
                AppPreference(this@MainActivity).saveLocalScreenCode(
                    externalCode,
                    Constants.localScreenCode
                )
            }

            cancelMultipleAlarms() //cancels all alarms
            callApi()//recall api to get new times after refresh
            if (!wakeLock.isHeld) {
                wakeLock.acquire()
            }

            /* checks if the app is run first time */
            val isFirstRun = AppPreference(this@MainActivity).isFirstTimeRun()
            if (isFirstRun) {
                /* runs on first run of the app after install and starts the function to call the api everyday */
                AppPreference(this@MainActivity).saveLocalScreenCode(
                    externalCode,
                    Constants.localScreenCode
                )

                //alarm manager for everyday api hit timer after every 24hrs
                scheduleApiCallTimer()
                AppPreference(this@MainActivity).setFirstTimeRun(false)
                Log.d(TAG2, "is first")
            } else {
                Log.d(TAG2, "!first")
            }

        } catch (e: Exception) {
            Log.d(TAG2, "$e")
        }

    }

    private fun showPlaySettingsDialog() {
        val dialogBinding = PlaySettingsDialogBinding.inflate(layoutInflater)

        playSettingsDialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnCancelListener {
            }
        }

        dialogBinding.run {
            btnWeb.setOnClickListener {
                playSettingsDialog!!.dismiss()
                AppPreference(this@MainActivity).saveKeyValue(LAST_WEB_URL, REAL_URL)
                binding.webView.loadUrl(REAL_URL)
                dialog.dismiss()
            }

            btnNative.setOnClickListener {
                playSettingsDialog!!.dismiss()

                dialog.dismiss()
            }

            radioGroup.setOnCheckedChangeListener { group, checkedId ->
                // Get the selected Radio Button
                val radioButton = group
                    .findViewById(checkedId) as RadioButton

                // on below line we are setting
                // text for our status text view.
                AppPreference(this@MainActivity).saveKeyValue(
                    checkedId.toString(),
                    "PLAY_SETTINGS_MODE"
                )
                Toast.makeText(applicationContext, "${radioButton.text}", Toast.LENGTH_LONG)
                    .show()
                Log.d(TAG2, "${radioButton.text}")
            }
        }
        playSettingsDialog?.show()
    }

    private fun showPlaySettingsButtons() {
        playSettingsDialog?.findViewById<ViewGroup>(R.id.rootLayouts)?.let {
            PlaySettingsDialogBinding.bind(it).run {
                btnWeb.isVisible = true
                btnNative.isVisible = true
                var savedPlaySetting = AppPreference(this@MainActivity).retrieveValueByKey(
                    "PLAY_SETTINGS_MODE",
                    "-1"
                ).toInt()
                if (savedPlaySetting != null && savedPlaySetting != -1) {
                    radioGroup.check(savedPlaySetting)
                }

            }
        }
        Log.d(TAG, "codes is shown")
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
        binding.webView.settings.databaseEnabled = true
//        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

//        if (!isNetworkAvailable(this))  //offline
//            binding.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

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
                startScheduler()
                showPlaySettingsDialog()
                showPlaySettingsButtons()
            }

            btPlayMode.setOnClickListener {
                if (playModeDialog == null || !playModeDialog!!.isShowing) {
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
                refreshButtonCall()
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
        unregisterReceiver(networkChangeReceiver);
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
            /* when app in foreground state then acquires wakelock */
            checkOverlayPermission()
            checkWritePermission()
            if (isTimerSet) {
                Log.d(TAG2, "onResume")
                if (!wakeLock.isHeld) {
                    wakeLock.acquire()
                    Log.d(TAG2, "wakelock acquired")
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
            /* when app in background state then resets the default timeOut */
            checkOverlayPermission()
            checkWritePermission()
            if (isTimerSet) {
                Log.d(TAG2, "onPause")
            } else {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
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
        var isTimerSet = false
        lateinit var wakeLock: PowerManager.WakeLock
        const val REAL_URL = "https://6lb.menu/signage"
        const val TEST_URL = "https://test.6lb.menu/signage"
        private const val LAST_WEB_URL = "web.url.last"
        private const val TAG = "MainActivity"
        private const val TAG2 = "TvTimer"
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