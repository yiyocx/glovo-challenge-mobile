package yiyo.com.glovoplayground.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import yiyo.com.glovoplayground.R
import yiyo.com.glovoplayground.data.models.ActionsUiModel
import yiyo.com.glovoplayground.data.models.ActionsUiModel.MoveToPosition
import yiyo.com.glovoplayground.helpers.extensions.isPermissionGranted
import yiyo.com.glovoplayground.helpers.extensions.plusAssign
import yiyo.com.glovoplayground.helpers.extensions.requestPermission
import yiyo.com.glovoplayground.viewModels.MapsViewModel

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private lateinit var viewModel: MapsViewModel
    private val polygonColor by lazy { ContextCompat.getColor(this, R.color.polygonColor) }
    private val polygonStrokeColor by lazy { ContextCompat.getColor(this, R.color.secondaryColor) }
    private val compositeDisposable by lazy { CompositeDisposable() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        viewModel = ViewModelProviders.of(this)[MapsViewModel::class.java]
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
            is MoveToPosition -> {
                val builder = LatLngBounds.Builder()
                action.polygon.points.forEach { builder.include(it) }
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 50))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isMapToolbarEnabled = false
        map.setOnMarkerClickListener(viewModel)

        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (isPermissionGranted(permission)) {
            onPermissionGranted()
        } else {
            requestPermission(
                permission, getString(R.string.dialog_location_title),
                getString(R.string.dialog_location_message), LOCATION_PERMISSIONS_REQUEST
            ) { onPermissionDenied() }
        }
    }

    private fun onPermissionGranted() {
        viewModel.loadCities(drawPolygon(), Action { updateLocationUI() })
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
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                ZOOM_LOCATE_USER
                            )
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
        viewModel.loadCities(drawPolygon(), Action {
            val fragmentDialog = CountryListBottomDialogFragment.newInstance()
            fragmentDialog.show(supportFragmentManager, CountryListBottomDialogFragment.TAG)
        })
    }

    private fun drawPolygon(): Consumer<Pair<MarkerOptions, PolygonOptions>> {
        return Consumer { (marker, polygonOptions) ->
            val polygon = map.addPolygon(polygonOptions)
            polygon.fillColor = polygonColor
            polygon.strokeColor = polygonStrokeColor
            polygon.strokeWidth = 5f

            map.addMarker(marker)
        }
    }

    companion object {
        const val LOCATION_PERMISSIONS_REQUEST = 1
        const val ZOOM_LOCATE_USER = 15f
    }
}
