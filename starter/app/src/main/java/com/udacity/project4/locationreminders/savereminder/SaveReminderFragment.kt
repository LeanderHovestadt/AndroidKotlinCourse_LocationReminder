package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
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
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragmentDirections
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
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
            if (latitude == null || longitude == null || title == null || description == null){
                _viewModel.showToast.postValue("Please select all fields first")
                return@setOnClickListener
            }

            val id = UUID.randomUUID().toString()
            addGeofence(LatLng(latitude, longitude), GEOFENCE_RADIUS, id)

            _viewModel.validateAndSaveReminder(ReminderDataItem(title,description,location, latitude,longitude))

            _viewModel.onClear()

            navigateBackToReminderListFragment()
        }
    }

    private fun navigateBackToReminderListFragment() {
        Log.i("SaveReminderFragment", "Navigating back to reminders list fragment.")
        //use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.BackTo(
                R.id.reminderListFragment
            )
        )
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(
        latLng: LatLng,
        radius: Float,
        id: String) {
        val geofence = buildGeofence(id, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        val geofenceRequest = buildGeofenceRequest(geofence)
        pendingIntent = getGeofencePendingIntent()

        geofencingClient.addGeofences(geofenceRequest, pendingIntent)
            .addOnSuccessListener(OnSuccessListener<Void?> {
                Log.i("SaveReminderFragment", "Successfully added geofence")
            })
            .addOnFailureListener(OnFailureListener { e ->
                Toast.makeText(requireContext(), "Could not create geofence. Background location permission is missing.", Toast.LENGTH_LONG).show()
                Log.w("SaveReminderFragment", "Error during creating geofence: ${e.localizedMessage}")
            })
    }

    private fun getGeofencePendingIntent() : PendingIntent?{
        if (pendingIntent != null) {
            return pendingIntent
        }
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        pendingIntent =
            PendingIntent.getBroadcast(requireContext(), GEOFENCE_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
