
## Table of Contents
- [Features](#features)
- [MVVM Architecture](#mvvm-architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running the App](#running-the-app)
- [Error States Handled](#error-states-handled)
- [Folder Structure](#folder-structure)
- [Learn More](#learn-more)

---

## Features
- Show nearby places on Google Maps
- Location permission handling
- Distance calculation using Haversine formula
- Loading and error states
- Fully modular and testable code using MVVM
---

## MVVM Architecture

This app uses a strict MVVM separation between Android native code
and React Native UI.

### Layers

**React Native (UI Layer)**
- `MapScreen.tsx` — renders map, markers, info card
- `usePlacesNativeModule.ts` — thin hook wrapping NativeModules.PlacesModule
- ZERO business logic, ZERO location API calls

**Native Module Bridge**
- `PlacesModule.kt` — exposes `requestPermission()` and `getNearbyPlaces()`
  methods to React Native. Delegates all work to ViewModel.

**ViewModel (`PlacesViewModel.kt`)**
- Orchestrates repository calls
- Manages UI state via StateFlow (loading, error, data)
- Handles all error cases: PermissionDenied, LocationDisabled, generic errors

**Repository (`LocationRepository.kt`)**
- ONLY class that touches Android Location APIs
- Uses FusedLocationProviderClient for GPS
- Returns mock nearby places (can swap for Google Places API)
- Haversine formula calculates distance from user

## Running the App

```bash
npm install
cd android && ./gradlew clean && cd ..
npx react-native run-android
```

## Error States Handled
- Location permission denied → Alert with Settings redirect
- Location services off → Alert prompting user to enable
- Network/GPS error → Alert with Retry button
- Loading state → Full-screen activity indicator