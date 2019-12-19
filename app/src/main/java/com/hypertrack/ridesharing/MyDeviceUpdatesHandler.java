package com.hypertrack.ridesharing;

import androidx.annotation.NonNull;

import com.hypertrack.sdk.views.DeviceUpdatesHandler;
import com.hypertrack.sdk.views.dao.Location;
import com.hypertrack.sdk.views.dao.StatusUpdate;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.sdk.views.maps.HyperTrackMap;

public abstract class MyDeviceUpdatesHandler implements DeviceUpdatesHandler {

    public void onLocationUpdateReceived(@NonNull android.location.Location location) {
    }

    @Override
    public void onLocationUpdateReceived(@NonNull Location location) {
        android.location.Location convertedLocation = new android.location.Location(HyperTrackMap.VIEWS_LOCATION_PROVIDER);
        convertedLocation.setLatitude(location.getLatitude());
        convertedLocation.setLongitude(location.getLongitude());
        convertedLocation.setAltitude(location.getAltitude() != null ? location.getAltitude() : 0.0f);
        convertedLocation.setAccuracy(location.getAccuracy() != null ? location.getAccuracy().floatValue() : 0.0f);
        convertedLocation.setBearing(location.getBearing() != null ? location.getBearing().floatValue() : 0.0f);
        convertedLocation.setSpeed(location.getSpeed() != null ? location.getSpeed().floatValue() : 0.0f);
        convertedLocation.setTime(System.currentTimeMillis());
        convertedLocation.setElapsedRealtimeNanos(System.nanoTime());

        onLocationUpdateReceived(convertedLocation);
    }

    @Override
    public void onBatteryStateUpdateReceived(int i) {
    }

    @Override
    public void onStatusUpdateReceived(@NonNull StatusUpdate statusUpdate) {
    }

    @Override
    public void onTripUpdateReceived(@NonNull Trip trip) {
    }

    @Override
    public void onError(Exception e, String s) {
    }

    @Override
    public void onCompleted(String s) {
    }
}
