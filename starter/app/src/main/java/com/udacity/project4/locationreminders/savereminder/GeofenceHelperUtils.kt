package com.udacity.project4.locationreminders.savereminder

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R

fun buildGeofence(id: String, latLng: LatLng, radius: Float, transitionTypes: Int): Geofence {
    return Geofence.Builder()
        .setCircularRegion(latLng.latitude, latLng.longitude, radius)
        .setRequestId(id)
        .setTransitionTypes(transitionTypes)
        .setLoiteringDelay(5000)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .build()
}

fun buildGeofenceRequest(geofence: Geofence): GeofencingRequest {
    return GeofencingRequest.Builder()
        .addGeofence(geofence)
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .build()
}
