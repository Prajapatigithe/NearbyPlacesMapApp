package com.nearbyplacesapp.data.model

data class Place(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,     
    val distanceMeters: Double  
)