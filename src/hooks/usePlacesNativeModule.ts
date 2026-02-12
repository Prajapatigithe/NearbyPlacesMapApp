import { useState, useCallback } from 'react';
import { NativeModules, PermissionsAndroid, Platform } from 'react-native';
import type { Place, PlacesResult } from '../types/Place';

const { PlacesModule } = NativeModules;

interface UsePlacesState {
  places: Place[];
  userLocation: { lat: number; lng: number } | null;
  isLoading: boolean;
  error: string | null;
  loadPlaces: () => void;
}

async function ensureLocationPermission(): Promise<boolean> {
  if (Platform.OS !== 'android') return true;

  const already = await PermissionsAndroid.check(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
  );
  if (already) return true;

  const result = await PermissionsAndroid.request(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
    {
      title: 'Location Permission Required',
      message: 'This app needs your location to find nearby places.',
      buttonPositive: 'Allow',
      buttonNegative: 'Deny',
      buttonNeutral: 'Ask Me Later',
    }
  );
  return result === PermissionsAndroid.RESULTS.GRANTED;
}

export function usePlacesNativeModule(): UsePlacesState {
  const [places, setPlaces] = useState<Place[]>([]);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadPlaces = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    try {
      const granted = await ensureLocationPermission();
      if (!granted) {
        setError('PERMISSION_DENIED');
        return;
      }

      const result: PlacesResult = await PlacesModule.getNearbyPlaces();

      setUserLocation({ lat: result.userLat, lng: result.userLng });
      setPlaces(result.places);

    } catch (e: any) {
      const code = e.code || e.message || 'UNKNOWN_ERROR';
      setError(code);
    } finally {
      setIsLoading(false);
    }
  }, []);

  return { places, userLocation, isLoading, error, loadPlaces };
}