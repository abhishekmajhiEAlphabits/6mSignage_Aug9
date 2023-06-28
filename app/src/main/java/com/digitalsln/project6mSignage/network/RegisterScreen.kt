package com.digitalsln.project6mSignage.network

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.digitalsln.project6mSignage.PlaylistNotBoundActivity
import com.digitalsln.project6mSignage.SlideShowActivity
import com.digitalsln.project6mSignage.model.PlaylistData
import com.digitalsln.project6mSignage.model.ScreenSize
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import com.digitalsln.project6mSignage.tvLauncher.utilities.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Singleton

@Singleton
class RegisterScreen(context: Context) {
    private val context = context
    private val TAG = "TvTimer"
    private val autoApiCallHandler = Handler(Looper.getMainLooper())

    /* registers screen to get screen code */
    fun registerScreen() {
        try {
            var screenSize = ScreenSize("2850x2315")
            ApiClient.client().create(ApiInterface::class.java)
                .registerScreen(screenSize).enqueue(object : Callback<String> {
                    override fun onResponse(call: Call<String>, response: Response<String>) {
                        if (response.isSuccessful && response.body() != null) {
                            Log.d(TAG, "${response.body()}")
                            var nativeScreenCode = response.body()
                            AppPreference(context).saveKeyValue(
                                nativeScreenCode!!,
                                Constants.nativeScreenCode
                            )
                            AppPreference(context).setScreenRegistered(true)
                            bindPlaylistForScreen()
                        }
                    }

                    override fun onFailure(call: Call<String>, t: Throwable) {
                        Toast.makeText(
                            context,
                            "Failed to get screen code! Please try again",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        Log.d(TAG, "$t")
                    }
                })

        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }
    }

    /* bind playlist to screen code for a screen */
    private fun bindPlaylistForScreen() {
        try {
            var nativeScreenCode =
                AppPreference(context).retrieveValueByKey(
                    Constants.nativeScreenCode,
                    Constants.defaultNativeScreenCode
                )
            ApiClient.client().create(ApiInterface::class.java)
                .getPlayList(nativeScreenCode).enqueue(object : Callback<List<PlaylistData>> {
                    override fun onResponse(
                        call: Call<List<PlaylistData>>,
                        response: Response<List<PlaylistData>>
                    ) {
                        if (response.isSuccessful) {

                            if (response.body() != null && response.code() == 200) {
                                Log.d(TAG, "${response.body()}")

                                if (response.body()!!.size != null) {
                                    AppPreference(context).setPlaylistBound(true)
                                    showActivity()
                                }
                            } else {
                                showActivity()
                                apiCronJob()
                            }
                        }
                    }

                    override fun onFailure(call: Call<List<PlaylistData>>, t: Throwable) {
                        Toast.makeText(
                            context,
                            "Failed to bind playlist! Please try again",
                            Toast.LENGTH_LONG
                        )
                            .show()
                        showActivity()
                        apiCronJob()
                        Log.d(TAG, "$t")
                    }
                })
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }

    }

    /* display activity based on playlist is bound or not */
    private fun showActivity() {
        try {
            var isScreenRegistered = AppPreference(context).isScreenRegistered()
            var isPlaylistBound = AppPreference(context).isPlaylistBound()
            var nativeScreenCode = AppPreference(context).retrieveValueByKey(
                Constants.nativeScreenCode,
                Constants.defaultNativeScreenCode
            )
            if (isScreenRegistered && isPlaylistBound) {
                var intent = Intent(context, SlideShowActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "inside slide")
            } else {
                var intent = Intent(context, PlaylistNotBoundActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Log.d(TAG, "inside not bound")
            }
        } catch (e: Exception) {
            Log.d(TAG, "$e")
        }

    }

    /* call api to bind playlist every 10 seconds */
    private fun apiCronJob() {
        autoApiCallHandler.postDelayed(Runnable {
            bindPlaylistForScreen()
            Log.d(TAG, "inside cron job")
        }, 10000L)
    }

}