package com.hypertrack.ridesharing.components.rider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hypertrack.maps.google.widget.GoogleMapAdapter;
import com.hypertrack.maps.google.widget.GoogleMapConfig;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.hypertrack.ridesharing.FirebaseFirestoreApi;
import com.hypertrack.ridesharing.MyDeviceUpdatesHandler;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.adapters.MapInfoWindowAdapter;
import com.hypertrack.ridesharing.components.MapPresenter;
import com.hypertrack.ridesharing.models.Order;
import com.hypertrack.ridesharing.models.Place;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.parsers.UserParser;
import com.hypertrack.ridesharing.utils.MapUtils;
import com.hypertrack.ridesharing.utils.TextFormatUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.functions.Consumer;

@SuppressWarnings("WeakerAccess")
public class RiderMapPresenter extends MapPresenter<RiderMapPresenter.RiderView, RiderMapPresenter.RiderState> {
    private static final String TAG = "RiderMapPresenter";

    private static final int PICKUP_AUTOCOMPLETE_REQUEST_CODE = 201;
    private static final int DROPOFF_AUTOCOMPLETE_REQUEST_CODE = 202;

    private final String myLocationText;
    private final GoogleMapConfig driverMapConfig;
    private ListenerRegistration driversListenerRegistration;

    public RiderMapPresenter(Context context, RiderMapPresenter.RiderView view) {
        super(context, view, new RiderState(context));
        myLocationText = context.getString(R.string.my_location);
        driverMapConfig = MapUtils.getBuilder(context)
                .locationMarker(new MarkerOptions()
                        .anchor(0.5F, 0.5F)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_drive_base_transparent)))
                .accuracyCircle(null)
                .bearingMarker(new MarkerOptions()
                        .anchor(0.5f, 0.5f)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker_dark)))
                .build();
    }

    @Override
    public void map(@NonNull GoogleMap googleMap) {
        super.map(googleMap);

        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(final LatLng latLng) {
                disposables.add(MapUtils.getAddress(mContext, latLng).subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) {
                        Place place = new Place(latLng);
                        place.preview = null;
                        place.address = s;
                        updateDropOffPlace(place);
                    }
                }));
            }
        });
        googleMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(Marker marker) {
            }

            @Override
            public void onMarkerDrag(Marker marker) {
            }

            @Override
            public void onMarkerDragEnd(final Marker marker) {
                if (mState.pickupPlace.marker != null &&
                        mState.pickupPlace.marker.getId().equals(marker.getId())) {
                    mState.isMyLocationBind = false;
                    disposables.add(MapUtils.getAddress(mContext, marker.getPosition()).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Place place = new Place(marker.getPosition());
                            place.preview = null;
                            place.address = s;
                            updatePickupPlace(place);
                        }
                    }));
                } else if (mState.dropOffPlace.marker != null &&
                        mState.dropOffPlace.marker.getId().equals(marker.getId())) {
                    disposables.add(MapUtils.getAddress(mContext, marker.getPosition()).subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) {
                            Place place = new Place(marker.getPosition());
                            place.preview = null;
                            place.address = s;
                            updateDropOffPlace(place);
                        }
                    }));
                }
            }
        });
    }

    @Override
    public void onViewReady() {
        super.onViewReady();

        if (mState.getOrder() == null) {
            if (mState.dropOffPlace == null) {
                mView.showChooseDest();
            } else {
                mView.showBookRide();
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location);
        if (mState.isMyLocationBind) {
            setMyLocationAsPickup();
        }
    }

    @Override
    protected boolean onMarkerClick(Marker marker) {
        return false;
    }

    @Override
    protected void onOrderChanged() {
        super.onOrderChanged();

        if (mState.getOrder() != null) {

            if (hyperTrackMap != null) {
                hyperTrackMap.setMyLocationEnabled(false);
            }
            if (Order.COMPLETED.equals(mState.getOrder().status)) {
                updatePickupPlace(null);
                updateDropOffPlace(null);
            } else {
                updatePickupPlace(mState.getOrder().pickup);
                updateDropOffPlace(mState.getOrder().dropoff);
            }


            if (mState.getOrder().driver == null) {
//                subscribeDriverUpdates();
                unsubscribeDriver();

                mView.showState();
            } else {
//                unsubscribeDriverUpdates();
                subscribeDriver(mState.getOrder().driver.deviceId);

                removePickupMarker();
                removeDropOffMarker();

                mView.dismissState();
                if (!Order.COMPLETED.equals(mState.getOrder().status)) {
                    mView.showOrderInfo(mState.getOrder().driver,
                            TextFormatUtils.getDestinationName(mContext, mState.getOrder()));
                } else {
                    mView.dismissOrderInfo();
                }
            }
            mView.dismissBookRide();
        } else {

            if (hyperTrackMap != null) {
                hyperTrackMap.setMyLocationEnabled(true);
            }
            unsubscribeDriver();
//            subscribeDriverUpdates();

            setMyLocationAsPickup();
            if (mState.dropOffPlace != null) {
                updateDropOffPlace(mState.dropOffPlace);
            }

            mView.dismissState();
            mView.dismissOrderInfo();
            mView.showBookRide();
        }

        hyperTrackMap.adapter().notifyDataSetChanged();
    }

    public void setMyLocationAsPickup() {
        if (mState.getOrder() == null
                && mState.currentLocation != null) {

            removePickupMarker();
            if (mState.pickupPlace == null) {
                mState.pickupPlace = new Place();
            }

            mState.isMyLocationBind = true;
            mState.pickupPlace.latitude = mState.currentLocation.getLatitude();
            mState.pickupPlace.longitude = mState.currentLocation.getLongitude();
            mState.pickupPlace.preview = myLocationText;

            disposables.add(MapUtils.getAddress(mContext, mState.pickupPlace.getLatLng()).subscribe(new Consumer<String>() {
                @Override
                public void accept(String s) {
                    mState.pickupPlace.address = s;
                    mView.updatePickup(mState.pickupPlace);
                    updateMapCamera();
                    //TODO debug
                    updatePickupPlace(mState.pickupPlace);
                }
            }));
        }
    }

    private void updatePickupPlace(Place place) {
        if (place == null) {
            removePickupMarker();
            mState.pickupPlace = null;
        } else {
            if (mState.pickupPlace == null) {
                mState.pickupPlace = new Place();
            }
            mState.pickupPlace.latitude = place.latitude;
            mState.pickupPlace.longitude = place.longitude;
            mState.pickupPlace.address = place.address;
            mState.pickupPlace.preview = place.preview;
            if (mState.pickupPlace.marker == null) {
                mState.pickupPlace.marker = mView.addMarker(mState.pickupPlace, R.drawable.ic_source_marker);
            } else {
                mState.pickupPlace.marker.setPosition(place.getLatLng());
            }
        }
        mView.updatePickup(place);
    }

    private void removePickupMarker() {
        if (mState.pickupPlace != null && mState.pickupPlace.marker != null) {
            mState.pickupPlace.marker.remove();
            mState.pickupPlace.marker = null;
        }
    }

    private void updateDropOffPlace(Place place) {
        if (place == null) {
            removeDropOffMarker();
            mState.dropOffPlace = null;
        } else {
            if (mState.dropOffPlace == null) {
                mView.dismissChooseDest();
                mView.showBookRide();
                mState.dropOffPlace = new Place();
            }

            mState.dropOffPlace.latitude = place.latitude;
            mState.dropOffPlace.longitude = place.longitude;
            mState.dropOffPlace.address = place.address;
            mState.dropOffPlace.preview = place.preview;
            if (mState.dropOffPlace.marker == null) {
                mState.dropOffPlace.marker = mView.addMarker(mState.dropOffPlace, R.drawable.ic_destination_marker);
            } else {
                mState.dropOffPlace.marker.setPosition(place.getLatLng());
            }
        }
        mView.updateDropoff(place);
    }

    private void removeDropOffMarker() {
        if (mState.dropOffPlace != null && mState.dropOffPlace.marker != null) {
            mState.dropOffPlace.marker.remove();
            mState.dropOffPlace.marker = null;
        }
    }

    private void updateMapCamera() {
        if (mState.pickupPlace != null && mState.dropOffPlace != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            if (mState.currentLocation != null) {
                builder.include(new LatLng(mState.currentLocation.getLatitude(), mState.currentLocation.getLongitude()));
            }
            builder.include(mState.pickupPlace.getLatLng());
            builder.include(mState.dropOffPlace.getLatLng());
            animateCamera(builder.build());
        } else {
            if (mState.pickupPlace != null) {
                animateCamera(mState.pickupPlace.getLatLng());
            } else if (mState.dropOffPlace != null) {
                animateCamera(mState.dropOffPlace.getLatLng());
            }
        }
    }

    public void choosePickup() {
        openPlaceSearch(mContext.getString(R.string.enter_pickup), PICKUP_AUTOCOMPLETE_REQUEST_CODE);
    }

    public void chooseDropoff() {
        openPlaceSearch(mContext.getString(R.string.enter_dropoff), DROPOFF_AUTOCOMPLETE_REQUEST_CODE);
    }

    public void orderTaxi() {
        if (mState.pickupPlace == null || mState.dropOffPlace == null) {
            mView.showNotification(R.string.choose_origin_and_destination);
            return;
        }

        final Order order = new Order();
        order.pickup = mState.pickupPlace;
        order.dropoff = mState.dropOffPlace;
        order.rider = mState.getUser();

        mView.showProgressBar();
        FirebaseFirestoreApi.createOrder(order)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        mView.hideProgressBar();

                        order.id = documentReference.getId();
                        mState.updateOrder(order);

                        subscribeOrderUpdates();
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        mView.hideProgressBar();
                        Log.w(TAG, "Error adding document", e);
                    }
                });
    }

    private void subscribeDriverUpdates() {
        if (driversListenerRegistration != null) {
            driversListenerRegistration.remove();
        }
        driversListenerRegistration = db.collection("users")
                .whereEqualTo("role", User.USER_ROLE_DRIVER)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        if (value != null) {
                            UserParser parser = new UserParser();
                            for (QueryDocumentSnapshot doc : value) {
                                User driver = mState.drivers.get(doc.getId());
                                if (driver == null) {
                                    final User newDriver = parser.parse(doc);
                                    if (newDriver != null && !TextUtils.isEmpty(newDriver.deviceId)) {
                                        newDriver.deviceUpdatesHandler = new MyDeviceUpdatesHandler() {
                                            @Override
                                            public void onLocationUpdateReceived(@NonNull Location location) {
                                                newDriver.location = location;
                                                if (newDriver.marker == null) {
                                                    newDriver.marker = mView.addMarker(newDriver);
                                                } else {
                                                    newDriver.marker.setRotation(newDriver.location.getBearing());
                                                    newDriver.marker.setPosition(
                                                            new LatLng(newDriver.location.getLatitude(), newDriver.location.getLongitude())
                                                    );
                                                }
                                            }
                                        };
                                        hyperTrackViews.subscribeToDeviceUpdates(newDriver.deviceId, newDriver.deviceUpdatesHandler);
                                        mState.drivers.put(newDriver.id, newDriver);
                                    }
                                }
                            }
                        }
                    }
                });
    }

    private void unsubscribeDriverUpdates() {
        if (driversListenerRegistration != null) {
            driversListenerRegistration.remove();
        }
        List<User> drivers = new ArrayList<>(mState.drivers.values());
        for (User driver : drivers) {
            if (driver.deviceUpdatesHandler != null) {
                hyperTrackViews.unsubscribeFromDeviceUpdates(driver.deviceUpdatesHandler);
                driver.deviceUpdatesHandler = null;
            }
            if (driver.marker != null) {
                driver.marker.remove();
            }
        }
        mState.drivers.clear();
    }

    private void subscribeDriver(String deviceId) {
        Log.d("subscribeDriver", deviceId + " | " + mState.driver + " | " + mState.getOrder().tripId);
        if (mState.driver == null) {
            hyperTrackViews.subscribeToDeviceUpdates(deviceId, this);
            GoogleMapAdapter mapAdapter = new GoogleMapAdapter(googleMap, driverMapConfig);
            mapAdapter.addTripFilter(this);
            mState.driver = HyperTrackMap.getInstance(mContext, mapAdapter);
            mState.driver.bind(hyperTrackViews, deviceId);
            googleMap.setInfoWindowAdapter(new MapInfoWindowAdapter(mContext, mapAdapter));
        }
        if (!TextUtils.isEmpty(mState.getOrder().tripId)) {
            mState.driver.subscribeTrip(mState.getOrder().tripId);
        }
        if (Order.COMPLETED.equals(mState.getOrder().status)) {
            mState.driver.setMyLocationEnabled(false);
        }
        mState.driver.adapter().notifyDataSetChanged();
    }

    private void unsubscribeDriver() {
        Log.d("unsubscribeDriver", mState.driver + "");
        hyperTrackViews.unsubscribeFromDeviceUpdates(this);
        if (mState.driver != null) {
            mState.driver.destroy();
            mState.driver = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKUP_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mState.isMyLocationBind = false;
                updatePickupPlace(new Place(Autocomplete.getPlaceFromIntent(data)));
                updateMapCamera();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null) Log.i(TAG, status.getStatusMessage());
            }
        } else if (requestCode == DROPOFF_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                updateDropOffPlace(new Place(Autocomplete.getPlaceFromIntent(data)));
                updateMapCamera();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                if (status.getStatusMessage() != null) Log.i(TAG, status.getStatusMessage());
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        unsubscribeDriverUpdates();
        unsubscribeDriver();
    }

    public interface RiderView extends MapPresenter.View {

        void showChooseDest();

        void dismissChooseDest();

        void showBookRide();

        void dismissBookRide();

        Marker addMarker(User driver);

        Marker addMarker(Place place, int iconResId);

        void updatePickup(Place place);

        void updateDropoff(Place place);

    }

    public static class RiderState extends MapPresenter.State {
        public Map<String, User> drivers = new HashMap<>();
        private Place pickupPlace = new Place();
        private Place dropOffPlace;
        private HyperTrackMap driver;
        private boolean isMyLocationBind = true;

        public RiderState(Context context) {
            super(context);
        }
    }
}
