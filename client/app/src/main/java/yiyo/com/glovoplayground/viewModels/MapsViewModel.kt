package yiyo.com.glovoplayground.viewModels

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.maps.android.PolyUtil
import com.xwray.groupie.ExpandableGroup
import com.xwray.groupie.Section
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import yiyo.com.glovoplayground.data.models.ActionsUiModel
import yiyo.com.glovoplayground.data.models.ActionsUiModel.*
import yiyo.com.glovoplayground.data.models.City
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

    private val actionsSubject = PublishSubject.create<ActionsUiModel>()

    val currentCityName = ObservableField("")
    val currentCityLanguage = ObservableField("")
    val currentCityCurrency = ObservableField("")
    val currentCityTimezone = ObservableField("")

    fun loadCities(onComplete: () -> Unit = {}) {
        compositeDisposable += cityRepository.getCities()
            .doOnNext { cities -> cachedCities.addAll(cities) }
            .flatMap { cities -> Observable.fromIterable(cities) }
            .map { city -> buildMarkerPolygon(city) }
            .map { DrawCitiesInfo(it) }
            .subscribeOn(Schedulers.io())
            .subscribe(
                { actionsSubject.onNext(it) },
                { actionsSubject.onNext(ShowError(it)) },
                { actionsSubject.onNext(OnDrawCitiesComplete(onComplete)) }
            )
    }

    private fun buildMarkerPolygon(city: CityLite): Triple<String, MarkerOptions, PolygonOptions> {
        val points = city.workingArea
            .filter { it.isNotBlank() }
            .map { encodedPolygon -> PolygonOptions().addAll(PolyUtil.decode(encodedPolygon)) }
            .flatMap { it.points }

        val simplifiedPolygon = PolygonOptions().addAll(QuickHull.convexHull(points))

        val marker = MarkerOptions()
        marker.title(city.name)
        marker.snippet(city.code)
        marker.position(simplifiedPolygon.getCenter())

        return Triple(city.code, marker, simplifiedPolygon)
    }

    fun loadFullCountries() {
        if (cachedCities.isEmpty()) {
            loadCities { mergeCountriesAndCities() }
        } else {
            mergeCountriesAndCities()
        }
    }

    private fun mergeCountriesAndCities() {
        compositeDisposable += Observable.combineLatest(
            countryRepository.getCountries(),
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
            .map<ActionsUiModel> { CountryList(it) }
            .onErrorReturn { ShowError(it) }
            .subscribeOn(Schedulers.io())
            .subscribe { actionsSubject.onNext(it) }
    }

    fun observeActions(): Observable<ActionsUiModel> = actionsSubject.hide()

    fun moveToCity(cityCode: String) {
        actionsSubject.onNext(MoveToCity(cityCode))
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        marker.showInfoWindow()
        moveToCity(marker.snippet)
        return true
    }

    fun showCityList() = actionsSubject.onNext(ShowCityList)

    fun onMapPositionChange(cityCode: String) {
        compositeDisposable += cityRepository.getCityDetail(cityCode)
            .map<ActionsUiModel> { ShowCityInfo(it) }
            .onErrorReturn { ShowError(it) }
            .subscribeOn(Schedulers.io())
            .subscribe { actionsSubject.onNext(it) }
    }

    fun showCityInfo(city: City) {
        with(city) {
            currentCityName.set(name)
            currentCityLanguage.set(languageCode)
            currentCityCurrency.set(currency)
            currentCityTimezone.set(timeZone)
        }
    }

    fun onOutOfCoverage(message: String) {
        currentCityName.set(message)
        currentCityLanguage.set(message)
        currentCityCurrency.set(message)
        currentCityTimezone.set(message)
    }
}
