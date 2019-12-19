package com.hypertrack.ridesharing.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.hypertrack.maps.google.widget.GoogleMapAdapter;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.hypertrack.sdk.views.maps.models.MapLocation;
import com.hypertrack.sdk.views.maps.models.MapObject;
import com.hypertrack.sdk.views.maps.models.MapTrip;
import com.hypertrack.sdk.views.dao.DeviceStatus;
import com.hypertrack.sdk.views.dao.Location;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.utils.MapUtils;
import com.hypertrack.ridesharing.utils.TextFormatUtils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class MapInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private final LayoutInflater layoutInflater;
    private final GoogleMapAdapter mapAdapter;

    public MapInfoWindowAdapter(Context context, GoogleMapAdapter mapAdapter) {
        layoutInflater = LayoutInflater.from(context);
        this.mapAdapter = mapAdapter;
    }

    @SuppressLint("CheckResult")
    @Override
    public View getInfoWindow(final Marker marker) {
        MapObject mapObject = mapAdapter.findMapObjectByMarker(marker);
        if (mapObject != null) {
            Single<String> single;
            ViewHolder viewHolder = (ViewHolder) marker.getTag();
            if (mapObject.getType() == HyperTrackMap.TRIP_MAP_OBJECT_TYPE) {

                marker.setInfoWindowAnchor(4.2f, 1.6f);
                GoogleMapAdapter.GMapTrip mapTrip = (GoogleMapAdapter.GMapTrip) mapObject;
                if (mapTrip.getDestinationMarker().getId().equals(marker.getId())) {
                    if (viewHolder == null) {
                        View view = layoutInflater.inflate(R.layout.info_window_to, null);
                        viewHolder = new ViewHolder(view);
                        marker.setTag(viewHolder);
                    }
                    single = viewHolder.updateTripDestination(mapTrip);
                } else {
                    if (!mapTrip.trip.getStatus().equals("completed")) {
                        return null;
                    }
                    if (viewHolder == null) {
                        View view = layoutInflater.inflate(R.layout.info_window_from, null);
                        viewHolder = new ViewHolder(view);
                        marker.setTag(viewHolder);
                    }
                    single = viewHolder.updateTripOrigin(mapTrip);
                }
            } else {

                marker.setInfoWindowAnchor(3.4f, 1.3f);
                MapLocation mapLocation = (MapLocation) mapObject;
                if (viewHolder == null) {
                    View view = layoutInflater.inflate(R.layout.info_window_from, null);
                    viewHolder = new ViewHolder(view);
                    marker.setTag(viewHolder);
                }
                single = viewHolder.updateLocation(mapLocation);
            }
            single.subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) throws Exception {
                    if (!TextUtils.isEmpty(s)) {
                        marker.showInfoWindow();
                    }
                }
            });
            return viewHolder.view;
        }
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @SuppressLint("CheckResult")
    static class ViewHolder {
        View view;
        TextView title;
        TextView text;

        ViewHolder(View view) {
            this.view = view;
            title = view.findViewById(R.id.title);
            text = view.findViewById(R.id.text);
        }

        Single<String> updateLocation(MapLocation mapLocation) {
            String status = view.getContext().getString(R.string.my_location);
            if (mapLocation.deviceStatus != -1) {
                if (DeviceStatus.STOP == mapLocation.deviceStatus) {
                    status = view.getContext().getString(R.string.stopped);
                } else {
                    status = view.getContext().getString(R.string.driver);
                }
            }
            title.setText(status);
            LatLng latLng = new LatLng(mapLocation.location.getLatitude(), mapLocation.location.getLongitude());
            return MapUtils.getAddress(view.getContext(), latLng).map(new Function<String, String>() {
                @Override
                public String apply(String s) throws Exception {
                    if (!s.equals(text.getText().toString())) {
                        text.setText(s);
                        return s;
                    }
                    return "";
                }
            });
        }

        Single<String> updateTripDestination(MapTrip mapTrip) {
            Trip.Destination destination = mapTrip.trip.getDestination();
            if (mapTrip.trip.getStatus().equals("completed")) {
                if (destination != null) {
                    return MapUtils.getAddress(view.getContext(),
                            new LatLng(destination.getLatitude(), destination.getLongitude()))
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    if (!s.equals(title.getText().toString())) {
                                        title.setText(s);
                                        return s;
                                    }
                                    return "";
                                }
                            });
                }
                if (mapTrip.trip.getEndDate() != null) {
                    String date = TextFormatUtils.getRelativeDateTimeString(view.getContext(), mapTrip.trip.getEndDate().getTime());
                    String text = view.getContext().getString(R.string.dropoff) + " | " + date;
                    this.text.setText(text);
                }
            } else {
                if (mapTrip.trip.getEstimate() != null
                        && mapTrip.trip.getEstimate().getRoute() != null
                        && mapTrip.trip.getEstimate().getRoute().getDuration() != null) {
                    long estimate = TimeUnit.SECONDS.toMinutes(
                            mapTrip.trip.getEstimate().getRoute().getDuration()
                    );
                    title.setText(String.format(view.getContext().getString(R.string.mins), estimate));
                }
                if (destination != null) {
                    LatLng latLng = new LatLng(destination.getLatitude(), destination.getLongitude());
                    return MapUtils.getAddress(view.getContext(), latLng).map(new Function<String, String>() {
                        @Override
                        public String apply(String s) throws Exception {
                            if (!s.equals(text.getText().toString())) {
                                text.setText(s);
                                return s;
                            }
                            return "";
                        }
                    });
                }
            }
            return null;
        }

        Single<String> updateTripOrigin(MapTrip mapTrip) {
            if (mapTrip.trip.getStatus().equals("completed")) {
                if (mapTrip.trip.getSummary() != null && !mapTrip.trip.getSummary().getLocations().isEmpty()) {
                    Location location = mapTrip.trip.getSummary().getLocations().get(0);
                    return MapUtils.getAddress(view.getContext(),
                            new LatLng(location.getLatitude(), location.getLongitude()))
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    if (!s.equals(title.getText().toString())) {
                                        title.setText(s);
                                        return s;
                                    }
                                    return "";
                                }
                            });
                }
                if (mapTrip.trip.getStartDate() != null) {
                    String date = TextFormatUtils.getRelativeDateTimeString(view.getContext(), mapTrip.trip.getStartDate().getTime());
                    String text = view.getContext().getString(R.string.dropoff) + " | " + date;
                    this.text.setText(text);
                }
            }
            return null;
        }
    }
}
