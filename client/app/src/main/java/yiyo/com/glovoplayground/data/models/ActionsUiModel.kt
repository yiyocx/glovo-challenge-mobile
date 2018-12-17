package yiyo.com.glovoplayground.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolygonOptions

sealed class ActionsUiModel {
    data class MoveToPosition(val position: LatLng, val polygon: PolygonOptions) : ActionsUiModel()
    object ShowCityList : ActionsUiModel()
    data class ShowCityInfo(val city: City) : ActionsUiModel()
    object OutOfCoverage : ActionsUiModel()
}