package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.android.parcel.Parcelize
import org.koin.android.ext.android.inject
import java.util.*

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSaveReminderBinding
    private lateinit var geofencingClient: GeofencingClient
    var pendingIntent: PendingIntent? = null

    private val GEOFENCE_RADIUS = 500f
    private val GEOFENCE_REQUEST_CODE = 16843
    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q

    // used to save the requested geofence
    @Parcelize
    data class GeofenceData(val title: String, val description: String, val location: String, val latLng: LatLng, val radius: Float, val id: String) : Parcelable
    private var pendingGeofence: GeofenceData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        geofencingClient = LocationServices.getGeofencingClient(requireContext())

        binding.saveReminder.setOnClickListener {
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

//            TODO: use the user entered reminder details to:
//             1) add a geofencing request
//             2) save the reminder to the local db
            if (title == null){
                _viewModel.showSnackBar.postValue(getString(R.string.err_enter_title))
                return@setOnClickListener
            }

            if (description == null){
                _viewModel.showSnackBar.postValue(getString(R.string.err_enter_description))
                return@setOnClickListener
            }

            if (latitude == null || longitude == null || location == null) {
                _viewModel.showSnackBar.postValue(getString(R.string.err_select_location))
                return@setOnClickListener
            }

            val id = UUID.randomUUID().toString()
            val geofenceData = GeofenceData(title, description, location, LatLng(latitude, longitude), GEOFENCE_RADIUS, id)

            checkPermissionsAndCreateGeofence(geofenceData)
        }
    }

    private fun navigateBackToReminderListFragment() {
        Log.i(TAG, "Navigating back to reminders list fragment.")
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.BackTo(
                R.id.reminderListFragment
            )
        )
    }

    /**
     * Starts the permission check and Geofence process only if the Geofence associated with the
     * current hint isn't yet active.
     */
    private fun checkPermissionsAndCreateGeofence(geofenceData: GeofenceData) {
        Log.d(TAG, "checkPermissionsAndStartGeofence")
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            Log.i(TAG, "foreground and background location permission approved")
             checkDeviceLocationSettingsAndStartGeofence(geofenceData)
        } else {
            Log.i(TAG, "requesting foreground and background location permission")

            // saving pending Geofence data
            pendingGeofence = geofenceData

            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun checkDeviceLocationSettingsAndStartGeofence(geofenceData: GeofenceData, resolve: Boolean = true
    ) {
        Log.d(TAG, "checkDeviceLocationSettingsAndStartGoogleMap called.")
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    val bundle = bundleOf(KEY_GEOFENCE to geofenceData)
                    startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0, 0, 0, bundle)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                _viewModel.showSnackBar.postValue(getString(R.string.location_required_error))
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.i(TAG, "starting geofence.")
                addGeofence(geofenceData)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {

            if (data == null){
                Log.e(TAG, "Could not retrieve bundle from onActivityResult Intent. Geofence can not be created.")
                return
            }

            val bundle = data.extras

            if (bundle == null){
                Log.e(TAG, "Bundle is null. Geofence can not be created.")
                return
            }

            val geofenceData = bundle.get(KEY_GEOFENCE)

            if (geofenceData == null){
                Log.e(TAG, "Could not retrieve data from bundle. Geofence can not be created.")
                return
            }

            checkDeviceLocationSettingsAndStartGeofence(geofenceData as GeofenceData)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(geofenceData: GeofenceData
    ) {
        val geofence = buildGeofence(
            geofenceData.id,
            geofenceData.latLng,
            geofenceData.radius,
            Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
        )
        val geofenceRequest = buildGeofenceRequest(geofence)
        pendingIntent = getGeofencePendingIntent()

        geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Log.i(TAG, "Successfully added geofence")
                _viewModel.validateAndSaveReminder(
                    ReminderDataItem(
                        geofenceData.title,
                        geofenceData.description,
                        geofenceData.location,
                        geofenceData.latLng.latitude,
                        geofenceData.latLng.longitude,
                        geofenceData.id
                    )
                )

                _viewModel.onClear()

                navigateBackToReminderListFragment()
            })
            .addOnFailureListener(OnFailureListener { e ->
                _viewModel.showSnackBar.postValue(getString(R.string.location_required_error))
                Log.w(
                    TAG,
                    "Error during creating geofence: ${e.localizedMessage}"
                )
            })
    }

    private fun getGeofencePendingIntent(): PendingIntent? {
        if (pendingIntent != null) {
            return pendingIntent
        }
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        pendingIntent =
            PendingIntent.getBroadcast(
                requireContext(),
                GEOFENCE_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        return pendingIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /*
*  Determines whether the app has the appropriate permissions across Android 10+ and all other
*  Android versions.
*/
    @TargetApi(29)
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        Log.d(TAG, "foregroundAndBackgroundLocationPermissionApproved called.")
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @TargetApi(29)
    private fun requestForegroundAndBackgroundLocationPermissions() {
        Log.d(TAG, "requestForegroundAndBackgroundLocationPermissions called.")
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                Log.d(TAG, "Requesting foreground and background location permission")
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> {
                Log.d(TAG, "Requesting foreground location permission only")
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            }
        }

        Log.i(TAG, "Requesting permissions for code $resultCode")
        requestPermissions(
            permissionsArray,
            resultCode
        )
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

        if ((requestCode == REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE &&
            grantResults[LOCATION_PERMISSION_INDEX] ==
            PackageManager.PERMISSION_GRANTED) ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_GRANTED &&
            grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
            PackageManager.PERMISSION_GRANTED))
        {
            // permission has been granted
            Log.i(TAG, "permission request has been granted.")

            pendingGeofence?.let {
                checkDeviceLocationSettingsAndStartGeofence(it)
                pendingGeofence = null
            }
                ?: Log.e(TAG, "There is no set pending Geofence.")
        }
        else{
            // permission has been denied
            Log.i(TAG, "permission request has been denied.")
            _viewModel.showSnackBar.postValue(getString(R.string.location_required_error))
        }
    }
    
}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val KEY_GEOFENCE = "key_geofence"
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SaveReminderFragment"
