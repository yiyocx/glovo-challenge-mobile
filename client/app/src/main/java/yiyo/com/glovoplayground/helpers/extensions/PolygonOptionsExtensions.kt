package yiyo.com.glovoplayground.helpers.extensions

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolygonOptions

fun PolygonOptions.getCenter(): LatLng {
    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    return builder.build().center
}