package com.digitalsln.project6mSignage

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.digitalsln.project6mSignage.databinding.ActivityMainBinding
import com.digitalsln.project6mSignage.databinding.HandMadeStartAppDialogBinding
import com.digitalsln.project6mSignage.databinding.PlayModeDialogBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val backgroundWebView by lazy { WebView(this) }
    private val sharedPref by lazy { getSharedPreferences("preferences", Context.MODE_PRIVATE) }
    private val REQUEST_OVERLAY_PERMISSION = 10101

    private val loadCodesState = MutableStateFlow(LoadCodesState())
    private var playModeDialog: Dialog? = null
    private var numberOfAttempts = 0

    // values for non-sleeping
    private val powerManager: PowerManager by lazy {
        getSystemService(Context.POWER_SERVICE) as PowerManager
    }

    private val wakeLock: PowerManager.WakeLock by lazy {
        powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "myapp:mywakelocktag")
    }

    private val binding get() = _binding!!
    private var dialogMain: Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        try {
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !Settings.canDrawOverlays(this)) {
//                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
//                startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
//            }
            preventFromSleeping()
            binding.webView.initWebView()
            setCashSettings()
//            if (!Consts.isAppStartedFromBroadcast) {
//                showHandMadeStartAppDialog()
//            } else
            binding.webView.loadUrl(sharedPref.getString(LAST_WEB_URL, URL)!!)

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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setCashSettings() {
        binding.webView.settings.allowFileAccess = true
        binding.webView.settings.cacheMode = WebSettings.LOAD_DEFAULT

        if (!isNetworkAvailable())  //offline
            binding.webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Activity.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event?.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            // Handle trackpad button click event
            // Replace this with your desired action
            showHandMadeStartAppDialog()
            Toast.makeText(this, "Upper arrow key pressed", Toast.LENGTH_SHORT).show()
            return true
        }
        //Toast.makeText(this, "Dispatch arrow key pressed" + event?.action, Toast.LENGTH_SHORT).show()
        return super.dispatchKeyEvent(event)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            loadCodesState.collect { handleLoadCodesState(it) }
        }
    }

    // handle state for load codes and show play mode dialog
    private fun handleLoadCodesState(state: LoadCodesState) {
        state.run {
            Log.d(TAG, "realCode $realCode testCode $testCode")
            if (testCode.isNotEmpty() && realCode.isEmpty()) tryToParseCode(URL)
            if (showDialog) {
                if (playModeDialog == null) showPlayModeDialog()
                if (testCode.isNotEmpty() && realCode.isNotEmpty()) showPlayModeButtons(
                    realCode,
                    testCode
                )
            } else playModeDialog = null
        }
    }

    override fun onResume() {
        super.onResume()
        binding.webView.reload()
    }

    private fun WebView.initWebView() {
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.domStorageEnabled = true
        settings.allowContentAccess = true
        webViewClient = WebViewClient()
        setInitialScale(100)
        activateJS(this)

        Log.d(TAG, "webview user agent ${settings.userAgentString}")
    }

    private fun preventFromSleeping() {
        wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
    }

    private fun showHandMadeStartAppDialog() {
        val dialogBinding = HandMadeStartAppDialogBinding.inflate(layoutInflater)

        val dialog = Dialog(this)
        dialogMain = dialog
        dialog.setContentView(dialogBinding.root)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogBinding.run {
            playButton.requestFocus()

            playButton.setOnClickListener {
                dialog.dismiss()
                sharedPref.edit().putString(LAST_WEB_URL, "$URL/1").apply()
                binding.webView.loadUrl("$URL/1")
            }

            playModeButton.setOnClickListener {
                loadCodesState.value = loadCodesState.value.copy(showDialog = true)
            }

            resetAllSettingsButton.setOnClickListener {
                showResetSettingsDialog()
            }
        }

        backgroundWebView.initWebView()
        dialog.show()
        tryToParseCode(TEST_URL)
    }

    private fun showPlayModeDialog() {

        val dialogBinding = PlayModeDialogBinding.inflate(layoutInflater)

        val choice = loadPlayModePreferences(getPreferences(Context.MODE_PRIVATE))

        playModeDialog = Dialog(this).apply {
            setContentView(dialogBinding.root)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setOnCancelListener {
                loadCodesState.value = loadCodesState.value.copy(showDialog = false)
            }
        }

        dialogBinding.run {

            when (choice) {
                PlayModeDialogChoice.REAL -> {
                    realButton.requestFocus()
                }
                PlayModeDialogChoice.TEST -> {
                    testButton.requestFocus()
                }
            }

            realButton.setOnClickListener {
                playModeDialog!!.dismiss()
                savePlayModePreferences(
                    getPreferences(Context.MODE_PRIVATE),
                    PlayModeDialogChoice.REAL
                )
                sharedPref.edit().putString(LAST_WEB_URL, URL).apply()
                binding.webView.loadUrl(URL)
                dialogMain?.dismiss()
            }

            testButton.setOnClickListener {
                playModeDialog!!.dismiss()
                savePlayModePreferences(
                    getPreferences(Context.MODE_PRIVATE),
                    PlayModeDialogChoice.TEST
                )
                sharedPref.edit().putString(LAST_WEB_URL, TEST_URL).apply()
                binding.webView.loadUrl(TEST_URL)
                dialogMain?.dismiss()
            }
        }

        playModeDialog?.show()
    }

    // hide loader and show buttons in play mode dialog
    private fun showPlayModeButtons(realCodeText: String, testCodeText: String) {
        Log.d(TAG, "show playmode buttons")
        playModeDialog?.findViewById<ViewGroup>(R.id.rootLayout)?.let {
            PlayModeDialogBinding.bind(it).run {
                realCode.text = realCodeText
                testCode.text = testCodeText
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

    // try to parse code from real and test url
    private fun tryToParseCode(url: String) {
        Log.i("WebView", "tryToParseCode $url")
        numberOfAttempts = 0
        with(backgroundWebView) {
            loadUrl(url)
            if (url == URL) {
                val realCode = sharedPref.getString(REAL_SCREEN_CODE, null)
                if (realCode.isNullOrEmpty()) tryToGetRealCodeWithDelay()
                else {
                    Log.d(TAG, "from pref realCode $realCode")
                    loadCodesState.value = loadCodesState.value.copy(realCode = realCode)
                }
            } else {
                val testCode = sharedPref.getString(TEST_SCREEN_CODE, null)
                if (testCode.isNullOrEmpty()) tryToGetTestCodeWithDelay()
                else {
                    Log.d(TAG, "from pref testCode $testCode")
                    loadCodesState.value = loadCodesState.value.copy(testCode = testCode)
                }
            }
        }
    }

    // make request to real site
    private fun WebView.tryToGetRealCodeWithDelay(delay: Long = 1000) {
        numberOfAttempts++
        Handler(mainLooper).postDelayed({
            evaluateJavascript(
                "(function() { return document.getElementsByClassName(\"screen-code subtitle\")[0].textContent; })()"
            ) { result ->
                if (result == "null") {
                    Log.i("WebView", "page is loading")
                    if (numberOfAttempts < DOWNLOAD_CODE_NUMBER) tryToGetRealCodeWithDelay()
                    else loadCodesState.value =
                        loadCodesState.value.copy(realCode = "Nothing found")
                } else {
                    Log.d(TAG, "class(\"screen-code\") text: $result (${result.length})")
                    val realCode = result.replace("\"", "").trim()
                    loadCodesState.value = loadCodesState.value.copy(realCode = realCode)
                    sharedPref.edit().putString(REAL_SCREEN_CODE, realCode).apply()
                }
            }
        }, delay)
    }

    // make request to test site
    private fun WebView.tryToGetTestCodeWithDelay(delay: Long = 1000) {
        numberOfAttempts++
        Handler(mainLooper).postDelayed({
            evaluateJavascript(
                "(function() { return document.getElementsByClassName(\"screen-code\")[0].textContent; })()"
            ) { result ->
                if (result == "null") {
                    Log.i("WebView", "page is loading")
                    if (numberOfAttempts < DOWNLOAD_CODE_NUMBER) tryToGetTestCodeWithDelay()
                    else loadCodesState.value =
                        loadCodesState.value.copy(testCode = "Nothing found")
                } else {
                    Log.d(TAG, "class(\"screen-code\") text: $result (${result.length})")
                    val testCode = result.replace("\"", "").trim()
                    loadCodesState.value = loadCodesState.value.copy(testCode = testCode)
                    sharedPref.edit().putString(TEST_SCREEN_CODE, testCode).apply()
                }
            }
        }, delay)
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

    override fun onDestroy() {
        wakeLock.release()
        super.onDestroy()
    }

    private fun activateJS(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
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

            sharedPref.edit().putString(REAL_SCREEN_CODE, null).apply()
            sharedPref.edit().putString(TEST_SCREEN_CODE, null).apply()
            loadCodesState.value = LoadCodesState()

            tryToParseCode(TEST_URL)
        }
    }

    private fun savePlayModePreferences(
        sharedPref: SharedPreferences,
        choice: PlayModeDialogChoice
    ) {
        val editor = sharedPref.edit()
        editor.putInt(PLAY_MODE_CHOICE_CODE, choice.code)
        editor.apply()
    }

    private fun loadPlayModePreferences(sharedPref: SharedPreferences): PlayModeDialogChoice {
        val choice = sharedPref.getInt(PLAY_MODE_CHOICE_CODE, 0)
        return PlayModeDialogChoice.getChoice(choice)
    }

    companion object {
        const val MAGICAL_NUMBER = 3
        const val DOWNLOAD_CODE_NUMBER = 5
        const val URL = "https://6lb.menu/signage"
        const val TEST_URL = "https://test.6lb.menu/signage"

        const val PLAY_MODE_CHOICE_CODE = "8"
        private const val REAL_SCREEN_CODE = "screen.code.real"
        private const val TEST_SCREEN_CODE = "screen.code.test"
        private const val LAST_WEB_URL = "web.url.last"

        private const val TAG = "MainActivity"
    }
}