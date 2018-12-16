package yiyo.com.glovoplayground.viewModels

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.PolyUtil
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import yiyo.com.glovoplayground.data.models.CityLite
import yiyo.com.glovoplayground.data.models.Country
import yiyo.com.glovoplayground.data.repositories.CityRepository
import yiyo.com.glovoplayground.data.repositories.CountryRepository
import yiyo.com.glovoplayground.helpers.plusAssign

class MapsViewModel : ViewModel() {

    private val countryRepository by lazy { CountryRepository() }
    private val cityRepository by lazy { CityRepository() }
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val cachedCities = mutableListOf<CityLite>()

    fun loadCities(action: Consumer<PolygonOptions>) {
        compositeDisposable += cityRepository.getCities()
            .doOnNext { cities -> cachedCities.addAll(cities) }
            .flatMap { cities -> Observable.fromIterable(cities) }
            .flatMap { city -> Observable.fromIterable(city.workingArea) }
            .map { encodedPolygon -> PolygonOptions().addAll(PolyUtil.decode(encodedPolygon)) }
            .filter { polygonOptions -> polygonOptions.points.isNotEmpty() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(action)
    }

    fun loadFullCountries(action: Consumer<List<Pair<Country, List<CityLite>>>>) {
        compositeDisposable += Observable.combineLatest(countryRepository.getCountries(),
            Observable.fromArray(cachedCities),
            BiFunction { countries: List<Country>, cities: List<CityLite> ->
                val citiesByCountry = cities.groupBy { it.countryCode }
                countries.map { Pair(it, citiesByCountry[it.code].orEmpty()) }
            })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(action)
    }
}
