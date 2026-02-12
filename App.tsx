import React from 'react';
import {  StyleSheet } from 'react-native';
import MapScreen from './src/screens/MapScreen';
import { SafeAreaView } from 'react-native-safe-area-context';

export default function App() {
  return (
    <SafeAreaView style={styles.root}>
      <MapScreen />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#fff' },
});