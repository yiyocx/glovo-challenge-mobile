package yiyo.com.glovoplayground.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.helpers.isPermissionGranted
import yiyo.com.glovoplayground.helpers.requestPermission

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (isPermissionGranted(permission)) {
            updateLocationUI()
        } else {
            requestPermission(
                permission, getString(R.string.dialog_location_title),
                getString(R.string.dialog_location_message), LOCATION_PERMISSIONS_REQUEST
            ) { chooseCityManually() }
        }
    }

    private fun updateLocationUI() {
        try {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            val lastLocationTask = fusedLocationClient.lastLocation
            lastLocationTask.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    task.result?.let {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f))
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == LOCATION_PERMISSIONS_REQUEST) {
            grantResults.forEachIndexed { index, _ ->
                when (grantResults[index]) {
                    PackageManager.PERMISSION_GRANTED -> updateLocationUI()
                    PackageManager.PERMISSION_DENIED -> chooseCityManually()
                }
            }
        }
    }

    private fun chooseCityManually() {
        println("I will show countries with cities")
    }

    companion object {
        const val LOCATION_PERMISSIONS_REQUEST = 1
    }
}
