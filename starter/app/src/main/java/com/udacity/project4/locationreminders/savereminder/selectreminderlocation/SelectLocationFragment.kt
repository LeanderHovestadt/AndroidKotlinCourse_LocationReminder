package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var pointOfInterest: PointOfInterest

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.i(TAG, "onCreateView called.")

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

//        TODO: add the map setup implementation
//        TODO: zoom to the user location after taking his permission
//        TODO: add style to the map
//        TODO: put a marker to location that the user selected

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        TODO: call this function after the user confirms on the selected location
        binding.saveLocation.setOnClickListener { onLocationSelected() }

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        if (this::pointOfInterest.isInitialized) {
            Log.i(TAG, "Successfully selected location.")
            _viewModel.latitude.value = pointOfInterest.latLng.latitude
            _viewModel.longitude.value = pointOfInterest.latLng.longitude
            _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
            _viewModel.selectedPOI.value = pointOfInterest
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
        } else {
            Log.i(TAG, "Location has not yet been selected.")
            _viewModel.showSnackBar.postValue(getString(R.string.err_select_location))
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            if (this::map.isInitialized) {
                Log.i(TAG, "Normal map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            } else {
                Log.w(TAG, "Map has not yet been initialized.")
                false
            }
        }
        R.id.hybrid_map -> {
            if (this::map.isInitialized) {
                Log.i(TAG, "Hybrid map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            } else {
                Log.w(TAG, "Map has not yet been initialized.")
                false
            }
        }
        R.id.satellite_map -> {
            if (this::map.isInitialized) {
                Log.i(TAG, "Satellite map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            } else {
                Log.w(TAG, "Map has not yet been initialized.")
                false
            }
        }
        R.id.terrain_map -> {
            if (this::map.isInitialized) {
                Log.i(TAG, "Terrain map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            } else {
                Log.w(TAG, "Map has not yet been initialized.")
                false
            }
        }
        else -> super.onOptionsItemSelected(item)
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onMapReady(googleMap: GoogleMap) {
        Log.i(TAG, "Map is ready.")

        map = googleMap

        //Default location
        val latitude = 48.09314459611282
        val longitude = 11.53787488905538
        val zoomLevel = 15f
        val munich = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(munich, zoomLevel))

        val homeLatLng = LatLng(latitude, longitude)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

        setPoiClickListener(map)
        setOnMapClickListener(map)
        setMapStyle(map)
        checkPermissionsAndEnableMyLocation()
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun setPoiClickListener(map: GoogleMap) {
        Log.i(TAG, "Setting POI click listener.")

        map.setOnPoiClickListener { poi ->

            map.clear()
            pointOfInterest = poi

            map.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            ).showInfoWindow()

            map.addCircle(
                CircleOptions()
                    .center(poi.latLng)
                    .radius(200.0)
                    .strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(4F)

            )
        }
    }


    private fun setOnMapClickListener(map: GoogleMap) {
        Log.i(TAG, "Setting POI click listener.")

        map.setOnMapClickListener { point ->

            map.clear()
            pointOfInterest = PointOfInterest(LatLng(point.latitude, point.longitude), UUID.randomUUID().toString(), "Point at %.2f/%.2f".format(point.latitude, point.longitude))

            map.addMarker(
                MarkerOptions()
                    .position(pointOfInterest.latLng)
                    .title(pointOfInterest.name)
            ).showInfoWindow()

            map.addCircle(
                CircleOptions()
                    .center(pointOfInterest.latLng)
                    .radius(200.0)
                    .strokeColor(Color.argb(255, 255, 0, 0))
                    .fillColor(Color.argb(64, 255, 0, 0)).strokeWidth(4F)

            )
        }
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndEnableMyLocation() {
        Log.d(TAG, "checkPermissionsAndStartGooglemap")
        if (foregroundLocationPermissionApproved()) {
            Log.i(TAG, "foreground and background location permission approved")
            checkDeviceLocationSettingsAndEnableMyLocation()
        } else {
            Log.i(TAG, "requesting foreground and background location permission")
            requestForegroundLocationPermissions()
        }
    }

    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult called.")

        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED)
        {
            Log.i(TAG, "permission request has been denied.")
            _viewModel.showSnackBar.postValue(getString(R.string.permission_denied_explanation))
        } else {
            Log.i(TAG, "permission request has been approved.")
            checkDeviceLocationSettingsAndEnableMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndEnableMyLocation(resolve:Boolean = true){
        Log.d(TAG, "checkDeviceLocationSettingsAndStartGoogleMap called.")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve){
                try {
                    exception.startResolutionForResult(requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                _viewModel.showSnackBar.postValue(getString(R.string.location_required_error))
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if ( it.isSuccessful ) {
                Log.i(TAG, "setting isMyLocationEnabled to true.")
                map.isMyLocationEnabled = true
            }
        }
    }

    /*
 *  Determines whether the app has the appropriate permissions across Android 10+ and all other
 *  Android versions.
 */
    @TargetApi(29)
    private fun foregroundLocationPermissionApproved(): Boolean {
        Log.d(TAG, "foregroundAndBackgroundLocationPermissionApproved called.")
        return (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29 )
    private fun requestForegroundLocationPermissions() {
        Log.d(TAG, "requestForegroundAndBackgroundLocationPermissions called.")
        if (foregroundLocationPermissionApproved())
            return
        val permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        Log.i(TAG, "Requesting foreground location permission")
        requestPermissions(
            permissionsArray,
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

}

private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val TAG = "SelectLocationFragment"
