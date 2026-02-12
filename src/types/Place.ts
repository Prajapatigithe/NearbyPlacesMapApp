export interface Place {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  category: string;
  distanceMeters: number;
}

export interface PlacesResult {
  userLat: number;
  userLng: number;
  places: Place[];
}