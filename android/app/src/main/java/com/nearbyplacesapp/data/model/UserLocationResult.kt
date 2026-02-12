package com.nearbyplacesapp.data.model

sealed class UserLocationResult {

    data class Success(
        val latitude: Double,
        val longitude: Double
    ) : UserLocationResult()

    object PermissionDenied : UserLocationResult()

    object LocationDisabled : UserLocationResult()

    data class Error(
        val message: String
    ) : UserLocationResult()
}
