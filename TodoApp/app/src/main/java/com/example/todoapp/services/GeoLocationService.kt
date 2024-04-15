package com.example.todoapp.services

import android.location.Location
import android.location.LocationListener
import android.util.Log
import com.example.todoapp.viewmodels.TodosViewModel

object GeoLocationService: LocationListener {
    var locationViewModel: TodosViewModel? = null
    override fun onLocationChanged(newLocation: Location) {
        locationViewModel?.updateLocation(newLocation)
        Log.i("location-logs", "Location new: $newLocation")
    }

    fun updateLatestLocation(latestLocation: Location) {
        locationViewModel?.updateLocation(latestLocation)
        Log.i("location-logs", "Location set to $latestLocation")
    }
}