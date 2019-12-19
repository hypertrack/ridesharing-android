package com.hypertrack.ridesharing.utils;

import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hypertrack.maps.google.widget.GoogleMapConfig;
import com.hypertrack.ridesharing.R;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MapUtils {
    public static GoogleMapConfig.Builder getBuilder(Context context) {
        int width = context.getResources().getDisplayMetrics().widthPixels;
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int mapRouteWidth = context.getResources().getDimensionPixelSize(R.dimen.map_route_width);
        GoogleMapConfig.TripOptions tripOptions = GoogleMapConfig.newTripOptions()
                .tripDestinationMarker(new MarkerOptions()
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_destination_marker)))
                .tripPassedRoutePolyline(null)
                .tripComingRoutePolyline(new PolylineOptions()
                        .width(mapRouteWidth)
                        .color(Color.BLACK)
                        .pattern(Collections.<PatternItem>singletonList(new Dash(mapRouteWidth))));
        return GoogleMapConfig.newBuilder(context)
                .tripOptions(tripOptions)
                .boundingBoxDimensions(width, height / 3);
    }

    public static Single<String> getAddress(Context context, final LatLng latLng) {
        final Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        return Single.fromCallable(new Callable<String>() {
            @Override
            public String call() {
                try {
                    List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                    if (!addresses.isEmpty()) {
                        Address address = addresses.get(0);
                        return address.getSubThoroughfare() + " " + address.getThoroughfare() + ", " + address.getLocality();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return "";
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
