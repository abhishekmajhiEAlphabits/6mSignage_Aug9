package com.digitalsln.project6mSignage.network

import com.digitalsln.project6mSignage.model.TimeData
import retrofit2.Call
import retrofit2.http.GET

interface ApiInterface {

    @GET("/api/Signage/screen/ontimes/GBVVC123")
    fun getTime(): Call<List<TimeData>>

}