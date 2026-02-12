import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity,
  ActivityIndicator, Alert, StatusBar, Platform,
} from 'react-native';
import MapView, { Marker, Region } from 'react-native-maps';
import { usePlacesNativeModule } from '../hooks/usePlacesNativeModule';
import type { Place } from '../types/Place';

export default function MapScreen() {
  const { places, userLocation, isLoading, error, loadPlaces } = usePlacesNativeModule();
  const [selectedPlace, setSelectedPlace] = useState<Place | null>(null);
  const mapRef = useRef<MapView>(null);

  useEffect(() => {
    loadPlaces();
  }, []);

  useEffect(() => {
    if (userLocation && mapRef.current) {
      mapRef.current.animateToRegion(
        {
          latitude: userLocation.lat,
          longitude: userLocation.lng,
          latitudeDelta: 0.02,
          longitudeDelta: 0.02,
        },
        1000 
      );
    }
  }, [userLocation]);

  useEffect(() => {
    if (!error) return;

    const errorMap: Record<string, { title: string; message: string }> = {
      PERMISSION_DENIED: {
        title: ' Permission Denied',
        message: 'Location permission is required. Please enable it in Settings.',
      },
      LOCATION_DISABLED: {
        title: ' Location Is Off',
        message: 'Please turn on Location Services in your device Settings.',
      },
      LOCATION_UNAVAILABLE: {
        title: ' Location Unavailable',
        message: 'Could not get your GPS signal. Move outdoors or near a window, then retry.',
      },
    };

    const info = errorMap[error] ?? { title: 'Error', message: error };
    Alert.alert(info.title, info.message, [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Retry', onPress: loadPlaces },
    ]);
  }, [error]);

  const formatDistance = (meters: number): string => {
    if (meters < 1000) return `${Math.round(meters)}m`;
    return `${(meters / 1000).toFixed(1)}km`;
  };
  const mapRegion: Region = {
    latitude: userLocation?.lat ?? 28.6139,
    longitude: userLocation?.lng ?? 77.209,
    latitudeDelta: 0.02,
    longitudeDelta: 0.02,
  };

  return (
    <View style={styles.container}>
      <StatusBar barStyle="dark-content" />

      <View style={styles.header}>
        <Text style={styles.headerTitle}>Nearby Places</Text>
        <Text style={styles.headerSub}>
          {isLoading
            ? ' Locating you…'
            : userLocation
            ? `📍 ${places.length} places found near you`
            : 'Waiting for location…'}
        </Text>
      </View>
      <MapView
        ref={mapRef}
        style={styles.map}
        initialRegion={mapRegion}   
        showsUserLocation={true}    
        showsMyLocationButton={true}
        showsCompass={true}
        onPress={() => setSelectedPlace(null)}
      >
        {places.map((place) => (
          <Marker
            key={place.id}
            coordinate={{
              latitude: place.latitude,
              longitude: place.longitude,
            }}
            title={place.name}
            description={`${formatDistance(place.distanceMeters)} away`}
            onPress={() => setSelectedPlace(place)}
            pinColor={place.category === 'park' ? '#00C853' : '#FF6D00'}
          />
        ))}
      </MapView>
      {isLoading && (
        <View style={styles.loadingOverlay}>
          <ActivityIndicator size="large" color="#4F9EFF" />
          <Text style={styles.loadingText}>Finding nearby places…</Text>
        </View>
      )}
      {selectedPlace && (
        <View style={styles.infoCard}>
          <View style={styles.infoCardInner}>
            <View style={styles.infoLeft}>
              <Text style={styles.placeName}>{selectedPlace.name}</Text>
              <Text style={styles.placeCategory}>{selectedPlace.category}</Text>
            </View>
            <View style={styles.distanceBadge}>
              <Text style={styles.distanceText}>
                {formatDistance(selectedPlace.distanceMeters)}
              </Text>
              <Text style={styles.distanceLabel}>away</Text>
            </View>
          </View>
          <TouchableOpacity
            style={styles.closeBtn}
            onPress={() => setSelectedPlace(null)}
          >
            <Text style={styles.closeBtnText}>✕</Text>
          </TouchableOpacity>
        </View>
      )}
      {userLocation && !isLoading && (
        <TouchableOpacity
          style={styles.centerBtn}
          onPress={() => {
            mapRef.current?.animateToRegion(
              {
                latitude: userLocation.lat,
                longitude: userLocation.lng,
                latitudeDelta: 0.02,
                longitudeDelta: 0.02,
              },
              800
            );
          }}
        >
          <Text style={styles.centerBtnText}>📍</Text>
        </TouchableOpacity>
      )}
      {!isLoading && (
        <TouchableOpacity style={styles.refreshBtn} onPress={loadPlaces}>
          <Text style={styles.refreshText}>↻ Refresh</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#fff' },
  header: {
    paddingTop: Platform.OS === 'android' ? 48 : 56,
    paddingBottom: 12,
    paddingHorizontal: 20,
    backgroundColor: '#fff',
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  headerTitle: { fontSize: 22, fontWeight: '700', color: '#111' },
  headerSub: { fontSize: 13, color: '#888', marginTop: 2 },
  map: { flex: 1 },
  loadingOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(255,255,255,0.85)',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 12,
  },
  loadingText: { fontSize: 14, color: '#4F9EFF', fontWeight: '500' },
  infoCard: {
    position: 'absolute', bottom: 100, left: 16, right: 16,
    backgroundColor: '#fff', borderRadius: 16, padding: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.15, shadowRadius: 12, elevation: 8,
  },
  infoCardInner: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  infoLeft: { flex: 1 },
  placeName: { fontSize: 17, fontWeight: '700', color: '#111' },
  placeCategory: {
    fontSize: 13, color: '#888', marginTop: 2, textTransform: 'capitalize',
  },
  distanceBadge: {
    backgroundColor: '#EBF4FF', borderRadius: 12,
    paddingVertical: 8, paddingHorizontal: 14, alignItems: 'center',
  },
  distanceText: { fontSize: 18, fontWeight: '800', color: '#4F9EFF' },
  distanceLabel: { fontSize: 11, color: '#7aabff', fontWeight: '600' },
  closeBtn: { position: 'absolute', top: -8, right: -8, padding: 8 },
  closeBtnText: { fontSize: 14, color: '#bbb', fontWeight: '700' },
  centerBtn: {
    position: 'absolute', bottom: 100, right: 16,
    width: 48, height: 48, borderRadius: 24,
    backgroundColor: '#fff', elevation: 6,
    alignItems: 'center', justifyContent: 'center',
    shadowColor: '#000', shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.2, shadowRadius: 4,
  },
  centerBtnText: { fontSize: 22 },
  refreshBtn: {
    position: 'absolute', bottom: 32, alignSelf: 'center',
    backgroundColor: '#4F9EFF', paddingHorizontal: 24, paddingVertical: 12,
    borderRadius: 100, elevation: 6,
  },
  refreshText: { color: '#fff', fontWeight: '700', fontSize: 14 },
});