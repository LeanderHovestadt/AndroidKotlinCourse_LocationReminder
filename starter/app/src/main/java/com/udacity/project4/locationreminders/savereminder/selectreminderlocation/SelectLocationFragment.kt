package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var pointOfInterest: PointOfInterest

    private val REQUEST_CODE_LOCATION_PERMISSION = 1
    private val REQUEST_CODE_BACKGROUND_LOCATION = 2

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        Log.i("SelectLocationFragment", "onCreateView called.")

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
        binding.saveButton.setOnClickListener { onLocationSelected() }

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
        if (this::pointOfInterest.isInitialized) {
            Log.i("SelectLocationFragment", "Successfully selected location.")
            _viewModel.latitude.value = pointOfInterest.latLng.latitude
            _viewModel.longitude.value = pointOfInterest.latLng.longitude
            _viewModel.reminderSelectedLocationStr.value = pointOfInterest.name
            _viewModel.selectedPOI.value = pointOfInterest
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment())
        } else {
            Log.i("SelectLocationFragment", "Location has not yet been selected.")
            _viewModel.showToast.postValue("Please select a location")
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            if (this::map.isInitialized) {
                Log.i("SelectLocationFragment", "Normal map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            } else {
                Log.w("SelectLocationFragment", "Map has not yet been initialized.")
                false
            }
        }
        R.id.hybrid_map -> {
            if (this::map.isInitialized) {
                Log.i("SelectLocationFragment", "Hybrid map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            } else {
                Log.w("SelectLocationFragment", "Map has not yet been initialized.")
                false
            }
        }
        R.id.satellite_map -> {
            if (this::map.isInitialized) {
                Log.i("SelectLocationFragment", "Satellite map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            } else {
                Log.w("SelectLocationFragment", "Map has not yet been initialized.")
                false
            }
        }
        R.id.terrain_map -> {
            if (this::map.isInitialized) {
                Log.i("SelectLocationFragment", "Terrain map type selected.")
                map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            } else {
                Log.w("SelectLocationFragment", "Map has not yet been initialized.")
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
        Log.i("SelectLocationFragment", "Map is ready.")

        map = googleMap

        //Default location
        val latitude = 48.09314459611282
        val longitude = 11.53787488905538
        val zoomLevel = 20f
        val munich = LatLng(latitude, longitude)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(munich, zoomLevel))

        val homeLatLng = LatLng(latitude, longitude)

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, zoomLevel))

        setPoiClickListener(map)
        //setMapStyle(map)
        enableMyLocation()
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
                Log.e("SelectLocationFragment", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("SelectLocationFragment", "Can't find style. Error: ", e)
        }
    }

    private fun setPoiClickListener(map: GoogleMap) {
        Log.i("SelectLocationFragment", "Setting POI click listener.")

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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        // Check if location permissions are granted and if so enable the
        // location data layer.
        Log.i("SelectLocationFragment", "onRequestPermissionsResult is called.")
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION || requestCode == REQUEST_CODE_BACKGROUND_LOCATION) {
            if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                enableMyLocation()
            }
        }
    }

    private fun enableMyLocation() {
        Log.i("SelectLocationFragment", "enableMyLocation is called.")

        val accessFinePermissionGranted = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!accessFinePermissionGranted) {
            Log.i("SelectLocationFragment", "accessFinePermission is not granted. Requesting access fine permission.")
            requestAccessFinePermission()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val backgroundPermissionGranted = ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!backgroundPermissionGranted) {
                Log.i("SelectLocationFragment", "backgroundPermission is not granted. Requesting background permission.")
                requestBackgroundPermission()
            } else if (accessFinePermissionGranted) {
                Log.i("SelectLocationFragment", "setting isMyLocationEnabled to true.")
                map.isMyLocationEnabled = true
            }
        } else {
            if (accessFinePermissionGranted
            ) {
                Log.i("SelectLocationFragment", "setting isMyLocationEnabled to true.")
                map.isMyLocationEnabled = true
            }
        }
    }

    private fun requestBackgroundPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf<String>(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_CODE_BACKGROUND_LOCATION
        )
    }

    private fun requestAccessFinePermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE_LOCATION_PERMISSION
        )
    }

}
