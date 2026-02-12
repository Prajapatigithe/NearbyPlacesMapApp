package com.nearbyplacesapp.module  

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import com.nearbyplacesapp.data.repository.LocationRepository
import com.nearbyplacesapp.viewmodel.PlacesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class PlacesModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val repository = LocationRepository(reactContext)
    private val viewModel = PlacesViewModel(repository)
    private val moduleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var pendingPermissionPromise: Promise? = null

    override fun getName() = "PlacesModule"

    private val permissionListener = PermissionListener { requestCode, _, grantResults ->
        if (requestCode == LOCATION_PERMISSION_CODE) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                pendingPermissionPromise?.resolve("granted")
            } else {
                pendingPermissionPromise?.reject("PERMISSION_DENIED", "User denied")
            }
            pendingPermissionPromise = null
            return@PermissionListener true
        }
        false
    }

    @ReactMethod
    fun requestPermission(promise: Promise) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            reactContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            promise.resolve("granted")
            return
        }

        val activity = reactContext.currentActivity as? PermissionAwareActivity ?: run {
            Log.e("PlacesModule", "No PermissionAwareActivity found")
            promise.reject("NO_ACTIVITY", "Activity unavailable")
            return
        }

        pendingPermissionPromise = promise
        activity.requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_CODE,
            permissionListener
        )
    }

    @ReactMethod
    fun getNearbyPlaces(promise: Promise) {
        val hasPermission = ContextCompat.checkSelfPermission(
            reactContext,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            promise.reject("PERMISSION_DENIED", "Location permission not granted")
            return
        }

        moduleScope.launch {
            try {
                viewModel.loadNearbyPlaces()

                val finalState = viewModel.state.first { !it.isLoading }

                if (finalState.error != null) {
                    promise.reject(finalState.error, finalState.error)
                    return@launch
                }

                val result = Arguments.createMap()
                result.putDouble("userLat", finalState.userLat ?: 0.0)
                result.putDouble("userLng", finalState.userLng ?: 0.0)

                val placesArray = Arguments.createArray()
                finalState.places.forEach { place ->
                    val placeMap = Arguments.createMap()
                    placeMap.putString("id", place.id)
                    placeMap.putString("name", place.name)
                    placeMap.putDouble("latitude", place.latitude)
                    placeMap.putDouble("longitude", place.longitude)
                    placeMap.putString("category", place.category)
                    placeMap.putDouble("distanceMeters", place.distanceMeters)
                    placesArray.pushMap(placeMap)
                }
                result.putArray("places", placesArray)
                promise.resolve(result)

            } catch (e: Exception) {
                promise.reject("UNKNOWN_ERROR", e.message ?: "Unknown error")
            }
        }
    }

    override fun invalidate() {
        super.invalidate()
        moduleScope.cancel()
    }

    companion object {
        private const val LOCATION_PERMISSION_CODE = 1001
    }
}
