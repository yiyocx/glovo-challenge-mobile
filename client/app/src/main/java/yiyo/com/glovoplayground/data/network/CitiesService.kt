package yiyo.com.glovoplayground.data.network

import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.Path
import yiyo.com.glovoplayground.data.models.City
import yiyo.com.glovoplayground.data.models.CityLite

interface CitiesService {

    @GET("api/cities")
    fun getCities(): Observable<List<CityLite>>

    @GET("api/cities/{code}")
    fun getCity(@Path("code") code: String): Observable<City>
}