package com.nearbyplacesapp.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.nearbyplacesapp.data.model.Place
import com.nearbyplacesapp.data.model.UserLocationResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.*

class LocationRepository(private val context: Context) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    suspend fun getCurrentLocation(): UserLocationResult {
        if (!hasLocationPermission()) {
            Log.d("LocationRepo", " Permission not granted")
            return UserLocationResult.PermissionDenied
        }
        if (!isLocationEnabled()) {
            Log.d("LocationRepo", " Location services disabled")
            return UserLocationResult.LocationDisabled
        }

        try {
            val lastLocation = fusedClient.lastLocation.await()
            if (lastLocation != null) {
                Log.d("LocationRepo", " Last known: ${lastLocation.latitude}, ${lastLocation.longitude}")
                return UserLocationResult.Success(lastLocation.latitude, lastLocation.longitude)
            }
            Log.d("LocationRepo", " Last known location is null, trying fresh GPS...")
        } catch (e: Exception) {
            Log.d("LocationRepo", " Last known failed: ${e.message}")
        }

        val gpsResult = withTimeoutOrNull(15_000L) {
            requestFreshLocation(highAccuracy = true)
        }

        if (gpsResult is UserLocationResult.Success) {
            Log.d("LocationRepo", " Fresh GPS success")
            return gpsResult
        }

        Log.d("LocationRepo", " GPS timed out or failed — trying network fallback...")

        val networkResult = withTimeoutOrNull(8_000L) {
            requestFreshLocation(highAccuracy = false)
        }

        return networkResult ?: run {
            Log.d("LocationRepo", " All location methods failed")
            UserLocationResult.Error("LOCATION_UNAVAILABLE")
        }
    }

   
    private suspend fun requestFreshLocation(highAccuracy: Boolean): UserLocationResult =
        suspendCancellableCoroutine { continuation ->

            val priority = if (highAccuracy)
                LocationRequest.PRIORITY_HIGH_ACCURACY       
            else
                LocationRequest.PRIORITY_LOW_POWER           

            val request = LocationRequest.create().apply {
                this.priority = priority
                numUpdates = 1
                interval = 2000
                fastestInterval = 1000
                maxWaitTime = if (highAccuracy) 10_000 else 7_000
            }

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    val location = result.lastLocation
                    Log.d("LocationRepo", " Got location (highAccuracy=$highAccuracy): ${location?.latitude}, ${location?.longitude}")

                    if (location != null) {
                        continuation.resume(
                            UserLocationResult.Success(location.latitude, location.longitude)
                        )
                    } else {
                        continuation.resume(
                            UserLocationResult.Error("Location result was null")
                        )
                    }
                    fusedClient.removeLocationUpdates(this)
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    Log.d("LocationRepo", " Available=${availability.isLocationAvailable} (highAccuracy=$highAccuracy)")
                }
            }

            try {
                fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
                continuation.invokeOnCancellation {
                    fusedClient.removeLocationUpdates(callback)
                }
            } catch (e: SecurityException) {
                Log.d("LocationRepo", " SecurityException: ${e.message}")
                continuation.resume(UserLocationResult.PermissionDenied)
            }
        }

    suspend fun getNearbyPlaces(lat: Double, lng: Double): List<Place> {
        val mockPlaces = listOf(
            Triple("Central Park",   lat + 0.003, lng + 0.002),
            Triple("Sunrise Cafe",   lat - 0.002, lng + 0.005),
            Triple("City Library",   lat + 0.005, lng - 0.003),
            Triple("Metro Station",  lat - 0.004, lng - 0.001),
            Triple("Sports Complex", lat + 0.007, lng + 0.004),
            Triple("Night Market",   lat - 0.006, lng + 0.007)
        )

        return mockPlaces.mapIndexed { index, (name, pLat, pLng) ->
            Place(
                id = "place_$index",
                name = name,
                latitude = pLat,
                longitude = pLng,
                category = if (index % 2 == 0) "park" else "restaurant",
                distanceMeters = haversine(lat, lng, pLat, pLng)
            )
        }.sortedBy { it.distanceMeters }
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}