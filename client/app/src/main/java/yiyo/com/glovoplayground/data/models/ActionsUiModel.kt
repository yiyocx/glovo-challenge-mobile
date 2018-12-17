package yiyo.com.glovoplayground.data.models

import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.xwray.groupie.ExpandableGroup

sealed class ActionsUiModel {
    data class DrawCitiesInfo(val data: Triple<String, MarkerOptions, PolygonOptions>) : ActionsUiModel()
    data class OnDrawCitiesComplete(val onComplete: () -> Unit) : ActionsUiModel()
    data class ShowError(val error: Throwable) : ActionsUiModel()
    data class CountryList(val countryGroups: List<ExpandableGroup>) : ActionsUiModel()
    data class MoveToCity(val cityCode: String) : ActionsUiModel()
    object ShowCityList : ActionsUiModel()
    data class ShowCityInfo(val city: City) : ActionsUiModel()
    object OutOfCoverage : ActionsUiModel()
}