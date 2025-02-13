package com.rahul.weatherwise.POJO

import com.google.gson.annotations.SerializedName

data class Sys(

    @SerializedName("type") val type:Int,
    @SerializedName("id") val id:Int,
    @SerializedName("country") val country:String,
    @SerializedName("sunrise") val sunrise:String,
    @SerializedName("sunset") val sunset:String,
)
