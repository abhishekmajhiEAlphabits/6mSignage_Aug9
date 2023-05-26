package com.digitalsln.project6mSignage.network

import com.digitalsln.project6mSignage.model.TimeData
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiInterface {

    //        @GET("/api/Signage/screen/ontimes/GBVVC123")
    @GET("/api/Signage/screen/ontimes/{screen_code}")
    fun getTime(@Path(value = "screen_code", encoded = true) code: String): Call<List<TimeData>>

}