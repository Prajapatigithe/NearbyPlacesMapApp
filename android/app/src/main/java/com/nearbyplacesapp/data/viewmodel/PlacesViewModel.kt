package com.nearbyplacesapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nearbyplacesapp.data.model.Place
import com.nearbyplacesapp.data.model.UserLocationResult
import com.nearbyplacesapp.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlacesUiState(
    val isLoading: Boolean = false,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val places: List<Place> = emptyList(),
    val error: String? = null
)

class PlacesViewModel(private val repository: LocationRepository) : ViewModel() {

    private val _state = MutableStateFlow(PlacesUiState())
    val state: StateFlow<PlacesUiState> = _state.asStateFlow()

    fun loadNearbyPlaces() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            when (val locationResult = repository.getCurrentLocation()) {

                is UserLocationResult.Success -> {
                    val places = repository.getNearbyPlaces(
                        locationResult.latitude,
                        locationResult.longitude
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        userLat = locationResult.latitude,
                        userLng = locationResult.longitude,
                        places = places,
                        error = null
                    )
                }

                UserLocationResult.PermissionDenied -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "PERMISSION_DENIED"
                    )
                }

                UserLocationResult.LocationDisabled -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "LOCATION_DISABLED"
                    )
                }

                is UserLocationResult.Error -> {
                    
                    val errorCode = when {
                        locationResult.message.contains("LOCATION_UNAVAILABLE") 
                        locationResult.message.contains("null")                 
                        locationResult.message.contains("unavailable")          
                        locationResult.message.contains("timed out")           
                        else -> locationResult.message
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = errorCode
                    )
                }
            }
        }
    }
}