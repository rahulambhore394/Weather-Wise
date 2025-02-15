package com.rahul.weatherwise.Utilities

import com.rahul.weatherwise.POJO.ModelClass
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface APIInterface {

    @GET("weather")
    fun getCurrentWeatherData(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("APPID") api_key: String
    ): Call<ModelClass>

    @GET("weather")
    fun getCityWeatherData(
        @Query("q") cityName: String,
        @Query("APPID") api_key: String
    ): Call<ModelClass>
}
