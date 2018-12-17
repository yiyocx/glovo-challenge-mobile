package yiyo.com.glovoplayground.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.data.models.ActionsUiModel
import yiyo.com.glovoplayground.data.models.ActionsUiModel.*
import yiyo.com.glovoplayground.databinding.ActivityMapsBinding
import yiyo.com.glovoplayground.helpers.extensions.isPermissionGranted
import yiyo.com.glovoplayground.helpers.extensions.plusAssign
import yiyo.com.glovoplayground.helpers.extensions.requestPermission
import yiyo.com.glovoplayground.viewModels.MapsViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

    private lateinit var map: GoogleMap
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var viewModel: MapsViewModel
    private val polygonColor by lazy { ContextCompat.getColor(this, R.color.polygonColor) }
    private val polygonStrokeColor by lazy { ContextCompat.getColor(this, R.color.secondaryColor) }
    private val compositeDisposable by lazy { CompositeDisposable() }
    private val binding by lazy {
        DataBindingUtil.setContentView<ActivityMapsBinding>(this, R.layout.activity_maps)
    }
    private val noCoverageMessage by lazy { resources.getString(R.string.out_of_coverage) }
    private val markersByCity = hashMapOf<String, Marker>()
    private val polygonsByCity = hashMapOf<String, Polygon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)[MapsViewModel::class.java]
        binding.viewModel = viewModel

        subscribeToActions()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun subscribeToActions() {
        compositeDisposable += viewModel.observeActions()
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { action -> handleAction(action) }
    }

    private fun handleAction(action: ActionsUiModel) {
        when (action) {
            is DrawCitiesInfo -> drawMarkerAndPolygon(action.data)
            is OnDrawCitiesComplete -> action.onComplete()
            is MoveToCity -> moveToCity(action.cityCode)
            is ShowCityList -> showCityList()
            is ShowCityInfo -> viewModel.showCityInfo(action.city)
            is OutOfCoverage -> viewModel.onOutOfCoverage(noCoverageMessage)
            is ShowError -> Toast.makeText(this, action.error.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun moveToCity(cityCode: String) {
        val builder = LatLngBounds.Builder()
        val polygon = polygonsByCity[cityCode]
        polygon?.points?.forEach { builder.include(it) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMapToolbarEnabled = false
        map.setOnMarkerClickListener(viewModel)
        map.setOnCameraIdleListener(this)

        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (isPermissionGranted(permission)) {
            onPermissionGranted()
        } else {
            requestPermission(
                permission, getString(R.string.dialog_location_title),
                getString(R.string.dialog_location_message), LOCATION_PERMISSIONS_REQUEST
                , onCancel = { onPermissionDenied() })
        }
    }

    private fun onPermissionGranted() {
        viewModel.loadCities { updateLocationUI() }
    }

    private fun updateLocationUI() {
        try {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = true
            val lastLocationTask = fusedLocationClient.lastLocation
            lastLocationTask.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    task.result?.let {
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), ZOOM_LOCATE_USER)
                        )
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
                    PackageManager.PERMISSION_GRANTED -> onPermissionGranted()
                    PackageManager.PERMISSION_DENIED -> onPermissionDenied()
                }
            }
        }
    }

    private fun onPermissionDenied() {
        viewModel.loadCities { showCityList() }
    }

    private fun showCityList() {
        val fragmentDialog = CountryListBottomDialogFragment.newInstance()
        fragmentDialog.show(supportFragmentManager, CountryListBottomDialogFragment.TAG)
    }

    private fun drawMarkerAndPolygon(data: Triple<String, MarkerOptions, PolygonOptions>) {
        val (cityCode, markerOptions, polygonOptions) = data

        val polygon = map.addPolygon(polygonOptions)
        polygon.fillColor = polygonColor
        polygon.strokeColor = polygonStrokeColor
        polygon.strokeWidth = 5f
        polygonsByCity[cityCode] = polygon

        val marker = map.addMarker(markerOptions)
        markersByCity[cityCode] = marker
    }

    override fun onCameraIdle() {
        val shouldShowMarkers = map.cameraPosition.zoom < ZOOM_SHOW_MARKERS
        markersByCity.values.forEach { it.isVisible = shouldShowMarkers }
        polygonsByCity.values.forEach { it.isVisible = !shouldShowMarkers }

        val currentPosition = map.cameraPosition.target
        val currentCityPolygon = polygonsByCity.asIterable()
            .firstOrNull { (_, polygon) -> PolyUtil.containsLocation(currentPosition, polygon.points, false) }

        if (currentCityPolygon != null) {
            viewModel.onMapPositionChange(currentCityPolygon.key)
        } else {
            viewModel.onOutOfCoverage(noCoverageMessage)
            viewModel.onOutOfCoverage(noCoverageMessage)
        }
    }

    companion object {
        const val LOCATION_PERMISSIONS_REQUEST = 1
        const val ZOOM_LOCATE_USER = 15f
        const val ZOOM_SHOW_MARKERS = 10f
    }
}
