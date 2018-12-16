package yiyo.com.glovoplayground.data.repositories

import io.reactivex.Observable
import yiyo.com.glovoplayground.data.models.Country
import yiyo.com.glovoplayground.data.network.CountriesService
import yiyo.com.glovoplayground.data.network.RetrofitBuilder

class CountryRepository {

    private val service = RetrofitBuilder.createService(CountriesService::class.java)

    fun getCountries(): Observable<List<Country>> {
        return service.getCountries()
    }
}