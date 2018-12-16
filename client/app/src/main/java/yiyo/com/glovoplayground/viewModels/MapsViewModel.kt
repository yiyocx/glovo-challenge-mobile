package yiyo.com.glovoplayground.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
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
import io.reactivex.subjects.PublishSubject
import yiyo.com.glovoplayground.data.models.ActionsUiModel
import yiyo.com.glovoplayground.data.models.ActionsUiModel.MoveToPosition
import yiyo.com.glovoplayground.data.models.CityLite
import yiyo.com.glovoplayground.data.models.Country
import yiyo.com.glovoplayground.data.repositories.CityRepository
import yiyo.com.glovoplayground.data.repositories.CountryRepository
import yiyo.com.glovoplayground.helpers.extensions.getCenter
import yiyo.com.glovoplayground.helpers.extensions.plusAssign
import yiyo.com.glovoplayground.helpers.utils.QuickHull
import yiyo.com.glovoplayground.ui.items.CityItem
import yiyo.com.glovoplayground.ui.items.ExpandableHeaderItem

class MapsViewModel : ViewModel(), GoogleMap.OnMarkerClickListener {

    private val countryRepository by lazy { CountryRepository() }
    private val cityRepository by lazy { CityRepository() }
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val cachedCities = mutableListOf<CityLite>()
    private val markersByCity = hashMapOf<String, MarkerOptions>()
    private val polygonsByCity = hashMapOf<String, PolygonOptions>()

    private val actionsSubject = PublishSubject.create<ActionsUiModel>()

    fun loadCities(onNext: Consumer<Pair<MarkerOptions, PolygonOptions>>, onComplete: Action = Action {}) {
        compositeDisposable += cityRepository.getCities()
            .doOnNext { cities -> cachedCities.addAll(cities) }
            .flatMap { cities -> Observable.fromIterable(cities) }
            .map { city -> buildMarkerPolygon(city) }
            .doOnNext { (marker, polygon) ->
                markersByCity[marker.snippet] = marker
                polygonsByCity[marker.snippet] = polygon
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(onNext, Consumer { Log.e("MapsViewModel", it.message) }, onComplete)
    }

    private fun buildMarkerPolygon(city: CityLite): Pair<MarkerOptions, PolygonOptions> {
        val points = city.workingArea
            .filter { it.isNotBlank() }
            .map { encodedPolygon -> PolygonOptions().addAll(PolyUtil.decode(encodedPolygon)) }
            .flatMap { it.points }

        val simplifiedPolygon = PolygonOptions().addAll(QuickHull.convexHull(points))

        val marker = MarkerOptions()
        marker.title(city.name)
        marker.snippet(city.code)
        marker.position(simplifiedPolygon.getCenter())

        return Pair(marker, simplifiedPolygon)
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

    fun observeActions(): Observable<ActionsUiModel> = actionsSubject.hide()

    fun moveToCity(cityCode: String) {
        val marker = markersByCity[cityCode]
        val polygon = polygonsByCity[cityCode]
        if (marker != null && polygon != null) {
            actionsSubject.onNext(MoveToPosition(marker.position, polygon))
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        moveToCity(marker.snippet)
        return true
    }
}
