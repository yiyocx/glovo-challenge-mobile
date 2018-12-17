package yiyo.com.glovoplayground.data.repositories

import io.reactivex.Observable
import yiyo.com.glovoplayground.data.models.City
import yiyo.com.glovoplayground.data.models.CityLite
import yiyo.com.glovoplayground.data.network.CitiesService
import yiyo.com.glovoplayground.data.network.RetrofitBuilder

class CityRepository {

    private val service = RetrofitBuilder.createService(CitiesService::class.java)

    fun getCities(): Observable<List<CityLite>> {
        return service.getCities()
    }

    fun getCityDetail(cityCode: String): Observable<City> {
        return service.getCity(cityCode)
    }
}