package yiyo.com.glovoplayground.data.network

import io.reactivex.Observable
import retrofit2.http.GET
import yiyo.com.glovoplayground.data.models.Country

interface CountriesService {

    @GET("api/countries")
    fun getCountries(): Observable<List<Country>>
}