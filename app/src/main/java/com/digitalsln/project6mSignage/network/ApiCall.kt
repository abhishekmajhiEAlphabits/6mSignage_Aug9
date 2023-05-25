package com.digitalsln.project6mSignage.network

import android.content.Context
import android.util.Log
import com.digitalsln.project6mSignage.MainActivity
import com.digitalsln.project6mSignage.model.TimeData
import com.digitalsln.project6mSignage.tvLauncher.utilities.AppPreference
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import javax.inject.Singleton

@Singleton
class ApiCall(context: Context) {
    val context = context
     fun callApi() {
        ApiClient.client().create(ApiInterface::class.java)
            .getTime().enqueue(object : Callback<List<TimeData>> {
                override fun onResponse(
                    call: Call<List<TimeData>>,
                    response: Response<List<TimeData>>
                ) {
                    if (response.isSuccessful) {
                        val calendar = Calendar.getInstance()
                        val day = calendar.get(Calendar.DAY_OF_WEEK) - 1
                        AppPreference(context).saveFromTime(response.body()!![day].from, "FROM_TIME")
                        AppPreference(context).saveToTime(
                            response.body()!![day].to,
                            "TO_TIME"
                        )
                        Log.d("abhi", "${response.body()}")
                        Log.d("abhi", "$day")
                        Log.d("abhi", "${response.body()!![day].from}")
                        Log.d("abhi", "${response.body()!![day].to}")
                    } else {
                        Log.d("abhi", "Failed")
                    }
                }

                override fun onFailure(call: Call<List<TimeData>>, t: Throwable) {
                    Log.d("abhi", "$t")
                }
            })
    }
}