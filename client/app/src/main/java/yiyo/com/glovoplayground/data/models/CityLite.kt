package yiyo.com.glovoplayground.data.models

import com.google.gson.annotations.SerializedName

data class CityLite(
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("country_code") val countryCode: String,
    @SerializedName("working_area") val workingArea: List<String>
)