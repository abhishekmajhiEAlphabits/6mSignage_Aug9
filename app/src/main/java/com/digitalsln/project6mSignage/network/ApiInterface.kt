package com.digitalsln.project6mSignage.network

import com.digitalsln.project6mSignage.model.PlaylistData
import com.digitalsln.project6mSignage.model.ScreenSize
import com.digitalsln.project6mSignage.model.TimeData
import retrofit2.Call
import retrofit2.http.*

interface ApiInterface {

    @GET("/api/Signage/screen/ontimes/{screen_code}")
    fun getTime(@Path(value = "screen_code", encoded = true) code: String): Call<List<TimeData>>

    @GET("/api/signage/screen/{screen_code}/playlist")
    fun getPlayList(@Path(value = "screen_code", encoded = true) code: String): Call<List<PlaylistData>>

    @POST("/api/ScreenRegistration/Register")
    @Headers(
        "Accept: application/json",
        "Content-Type: application/json")
    fun registerScreen(@Body screenSize: ScreenSize): Call<String>
}