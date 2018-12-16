package yiyo.com.glovoplayground.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.PolyUtil
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.Section
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import yiyo.com.glovoplayground.data.models.CityLite
import yiyo.com.glovoplayground.data.models.Country
import yiyo.com.glovoplayground.data.repositories.CityRepository
import yiyo.com.glovoplayground.data.repositories.CountryRepository
import yiyo.com.glovoplayground.helpers.plusAssign
import yiyo.com.glovoplayground.ui.items.CityItem
import yiyo.com.glovoplayground.ui.items.ExpandableHeaderItem

class MapsViewModel : ViewModel() {

    private val countryRepository by lazy { CountryRepository() }
    private val cityRepository by lazy { CityRepository() }
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val cachedCities = mutableListOf<CityLite>()

    fun loadCities(onNext: Consumer<PolygonOptions>, onComplete: Action = Action {}) {
        compositeDisposable += cityRepository.getCities()
            .doOnNext { cities -> cachedCities.addAll(cities) }
            .flatMap { cities -> Observable.fromIterable(cities) }
            .flatMap { city -> Observable.fromIterable(city.workingArea) }
            .map { encodedPolygon -> PolygonOptions().addAll(PolyUtil.decode(encodedPolygon)) }
            .filter { polygonOptions -> polygonOptions.points.isNotEmpty() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, Consumer { Log.e(MapsViewModel::class.simpleName, it.message) }, onComplete)
    }

    fun loadFullCountries(action: Consumer<List<ExpandableGroup>>) {
        compositeDisposable += Observable.combineLatest(countryRepository.getCountries(),
            Observable.fromArray(cachedCities),
            BiFunction { countries: List<Country>, cities: List<CityLite> ->
                val citiesByCountry = cities.groupBy { it.countryCode }
                countries.map { Pair(it, citiesByCountry[it.code].orEmpty()) }
            })
            .map { data ->
                data.map { (country, cities) ->
                    val header = ExpandableHeaderItem(country.name)
                    val section = Section()
                    section.addAll(cities.map { CityItem(it) })

                    ExpandableGroup(header, true).apply {
                        add(section)
                    }
                }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(action)
    }
}
