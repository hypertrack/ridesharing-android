package com.hypertrack.ridesharing.components;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.hypertrack.maps.google.widget.GoogleMapAdapter;
import com.hypertrack.maps.google.widget.GoogleMapConfig;
import com.hypertrack.sdk.views.DeviceUpdatesHandler;
import com.hypertrack.sdk.views.HyperTrackViews;
import com.hypertrack.sdk.views.QueryResultHandler;
import com.hypertrack.sdk.views.dao.StatusUpdate;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.sdk.views.maps.GpsLocationProvider;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.hypertrack.sdk.views.maps.Predicate;
import com.hypertrack.ridesharing.MySharedPreferences;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.adapters.MapInfoWindowAdapter;
import com.hypertrack.ridesharing.models.Order;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.parsers.OrderParser;
import com.hypertrack.ridesharing.utils.HyperTrackUtils;
import com.hypertrack.ridesharing.utils.MapUtils;
import com.hypertrack.ridesharing.views.Snackbar;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.reactivex.disposables.CompositeDisposable;

@SuppressWarnings("WeakerAccess")
public abstract class MapPresenter<V extends MapPresenter.View, S extends MapPresenter.State>
        implements LocationListener, DeviceUpdatesHandler, Predicate<Trip> {
    private static final String TAG = "MapPresenter";

    private static final String ALERT_DIALOG_ORDER_CANCELED_KEY = "ALERT_DIALOG_ORDER_CANCELED";

    protected final Context mContext;

    protected final V mView;
    protected final S mState;

    private GoogleMapConfig mapConfig;
    protected HyperTrackViews hyperTrackViews;
    protected HyperTrackMap hyperTrackMap;

    private final Autocomplete.IntentBuilder autocompleteIntentBuilder;
    private final int mapPadding;
    protected GoogleMap googleMap;

    protected final LocationManager locationManager;
    protected final FirebaseFirestore db;

    protected final CompositeDisposable disposables = new CompositeDisposable();
    private ListenerRegistration orderListenerRegistration;

    public MapPresenter(Context context, V view, S state) {
        mContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        mView = view;
        mState = state;
        mapPadding = mContext.getResources().getDimensionPixelSize(R.dimen.map_padding);

        Places.initialize(mContext, MainActivity.GOOGLE_API_KEY);

        mapConfig = MapUtils.getBuilder(context).build();

        hyperTrackViews = HyperTrackViews.getInstance(mContext, HyperTrackUtils.getPubKey(context));
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        db = FirebaseFirestore.getInstance();

        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                Place.Field.ADDRESS, Place.Field.ADDRESS_COMPONENTS
        );
        autocompleteIntentBuilder = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields);
    }

    public void map(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        GoogleMapAdapter mapAdapter = new GoogleMapAdapter(googleMap, mapConfig);
        hyperTrackMap = HyperTrackMap.getInstance(mContext, mapAdapter);
        hyperTrackMap.bind(new GpsLocationProvider(mContext));
        hyperTrackMap.setLocationUpdatesListener(this);
        googleMap.setInfoWindowAdapter(new MapInfoWindowAdapter(mContext, mapAdapter));

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {

                if (mState.isUIVisible) {
                    mView.hideUI();
                    for (Snackbar snackbar : mState.activeSnackbars) {
                        snackbar.dismiss();
                    }
                } else {
                    mView.showUI();
                    for (Snackbar snackbar : mState.activeSnackbars) {
                        snackbar.show();
                    }
                }
                mState.isUIVisible = !mState.isUIVisible;
            }
        });
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return MapPresenter.this.onMarkerClick(marker);
            }
        });
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
            }
        });

        if (mState.getOrder() != null) {
            subscribeOrderUpdates();
        }
    }

    public void onViewReady() {
    }

    public void setCameraFixedEnabled(boolean enabled) {
        if (hyperTrackMap != null) {
            mState.isCameraFixed = enabled;
            hyperTrackMap.adapter().setCameraFixedEnabled(enabled);
            if (enabled) {
                mView.hideUI();
            } else {
                mView.showUI();
            }
        }
    }

    public void animateCamera(final LatLng latLng) {
        if (googleMap != null) {
            googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            latLng,
                            17
                    ));
                }
            });
        }
    }

    public void animateCamera(final LatLngBounds latLngBounds) {
        if (googleMap != null) {
            googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    googleMap.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(latLngBounds, mapPadding),
                            1000,
                            null);
                }
            });
        }
    }

    protected abstract boolean onMarkerClick(Marker marker);

    @Override
    public void onLocationChanged(Location location) {
        boolean isFirstLocation = mState.currentLocation == null;
        mState.currentLocation = location;
        if (isFirstLocation) {
            if (hyperTrackMap != null) {
                hyperTrackMap.moveToMyLocation();
            }
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d("Location updates:", s + " provider enabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d("Location updates:", s + " provider disabled");
    }

    protected void openPlaceSearch(String hint, int autocompleteRequestCode) {
        mView.startActivityForResult(
                autocompleteIntentBuilder
                        .setHint(hint)
                        .build(mContext),
                autocompleteRequestCode);
    }

    public void addSnackbar(Snackbar snackbar) {
        mState.activeSnackbars.add(snackbar);
        snackbar.show();
        mState.isUIVisible = true;
        if (!mState.isCameraFixed) {
            mView.showUI();
        }
    }

    public void removeSnackbar(Snackbar snackbar) {
        mState.activeSnackbars.remove(snackbar);
        snackbar.dismiss();
    }

    protected void onOrderChanged() {
        if (mState.getOrder() != null) {
            Log.d(TAG, "ORDER STATUS: " + mState.getOrder().status);

            if (Order.COMPLETED.equals(mState.getOrder().status)) {
                User user = User.USER_ROLE_DRIVER.equals(mState.getUser().role)
                        ? mState.getOrder().rider : mState.getOrder().driver;
                mView.showTripEndInfo(mState.getTrip(), user);
                mView.showUI();
            }
        }
    }

    protected void subscribeOrderUpdates() {
        if (mState.getOrder() != null) {
            if (orderListenerRegistration != null) {
                orderListenerRegistration.remove();
            }
            onOrderChanged();
            orderListenerRegistration = db.collection("orders").document(mState.getOrder().id)
                    .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                        @Override
                        public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                            @Nullable FirebaseFirestoreException e) {
                            if (documentSnapshot != null) {

                                OrderParser parser = new OrderParser();
                                Order order = parser.parse(documentSnapshot);

                                if (order == null || mState.getOrder() == null ||
                                        (!Order.CANCELLED.equals(mState.getOrder().status) && Order.CANCELLED.equals(order.status))) {

                                    if (orderListenerRegistration != null) {
                                        orderListenerRegistration.remove();
                                        orderListenerRegistration = null;
                                    }
                                    mState.updateOrder(null);
                                    onOrderChanged();
                                    mView.showAlertDialog(ALERT_DIALOG_ORDER_CANCELED_KEY, R.string.order_was_cancelled);
                                    return;
                                }

                                if (Order.COMPLETED.equals(mState.getOrder().status)) {
                                    onTripUpdateReceived(mState.getTrip());
                                }

                                mState.updateOrder(order);
                                onOrderChanged();

                                if (!TextUtils.isEmpty(mState.getOrder().tripId) && mState.getTrip() == null) {
                                    hyperTrackViews.getTrip(mState.getOrder().tripId, new QueryResultHandler<Trip>() {
                                        @Override
                                        public void onQueryResult(Trip trip) {
                                            mState.updateTrip(trip);
                                            hyperTrackMap.moveToTrip(trip);
                                            onOrderChanged();
                                        }

                                        @Override
                                        public void onQueryFailure(Exception e) {

                                        }
                                    });
                                }
                            }
                        }
                    });
        }
    }

    public void onAlertDialogDismissed(String key) {
        switch (key) {
            case ALERT_DIALOG_ORDER_CANCELED_KEY:
                break;
        }
    }

    public void cancelOrder() {
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
        }
        if (mState.getOrder() != null) {
            mView.showProgressBar();

            DocumentReference washingtonRef = db.collection("orders").document(mState.getOrder().id);
            washingtonRef
                    .update(
                            "status", Order.CANCELLED
                    )
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            mView.hideProgressBar();
                            if (mState.getOrder() != null && mState.getOrder().marker != null) {
                                mState.getOrder().marker.remove();
                            }
                            mState.updateOrder(null);
                            mView.dismissOrderInfo();
                            mView.dismissTripEndInfo();
                            onOrderChanged();
                            Log.d(TAG, "DocumentSnapshot successfully updated!");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            mView.hideProgressBar();
                            Log.w(TAG, "Error updating document", e);
                        }
                    });
        }
    }

    public void closeCompletedOrder() {
        mState.updateOrder(null);
        mState.updateTrip(null);
        onOrderChanged();
        mView.dismissTripEndInfo();
    }

    @Override
    public boolean apply(Trip trip) {
        return mState.getOrder() != null
                && trip.getTripId().equals(mState.getOrder().tripId)
                // It's needed to hide first trip after a driver started a ride
                && (trip.getStatus().equals("active") || mState.getOrder().status.equals(Order.COMPLETED));
    }

    @Override
    public void onLocationUpdateReceived(@NonNull com.hypertrack.sdk.views.dao.Location location) {
    }

    @Override
    public void onBatteryStateUpdateReceived(int i) {
    }

    @Override
    public void onStatusUpdateReceived(@NonNull StatusUpdate statusUpdate) {
    }

    @Override
    public void onTripUpdateReceived(Trip trip) {
        if (trip != null && mState.getOrder() != null && trip.getTripId().equals(mState.getOrder().tripId)) {
            mState.updateTrip(trip);
            if (Order.COMPLETED.equals(mState.getOrder().status) && "completed".equals(trip.getStatus())) {
                User user = User.USER_ROLE_DRIVER.equals(mState.getUser().role)
                        ? mState.getOrder().rider : mState.getOrder().driver;
                mView.showTripEndInfo(mState.getTrip(), user);
            }
        }
    }

    @Override
    public void onError(Exception e, String s) {
    }

    @Override
    public void onCompleted(String s) {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void destroy() {
        if (orderListenerRegistration != null) {
            orderListenerRegistration.remove();
        }
        hyperTrackViews.stopAllUpdates();
        googleMap = null;
        if (hyperTrackMap != null) {
            hyperTrackMap.destroy();
        }
        disposables.clear();
    }

    public interface View {

        void showState();

        void dismissState();

        void showOrderInfo(User user, CharSequence address);

        void dismissOrderInfo();

        void showTripEndInfo(Trip trip, User user);

        void dismissTripEndInfo();

        void showAlertDialog(String key, int textResId);

        void showUI();

        void hideUI();

        void showProgressBar();

        void hideProgressBar();

        void showNotification(int textResId);

        void startActivityForResult(Intent intent, int requestCode);

    }

    public static class State {
        private final SharedPreferences sharedPreferences;
        private final ObjectMapper mapper = new ObjectMapper();

        public Location currentLocation;
        public boolean isUIVisible = true;
        public Set<Snackbar> activeSnackbars = new HashSet<>();
        private User user;
        private Order order;
        private Trip trip;
        public boolean isCameraFixed = false;

        public User getUser() {
            return user;
        }

        public Order getOrder() {
            return order;
        }

        public Trip getTrip() {
            return trip;
        }

        public State(Context context) {
            context = context.getApplicationContext() == null ? context : context.getApplicationContext();
            sharedPreferences = MySharedPreferences.get(context);

            String json = sharedPreferences.getString(MySharedPreferences.USER_KEY, null);
            if (json != null) {
                try {
                    user = mapper.readValue(json, User.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            json = sharedPreferences.getString(MySharedPreferences.ORDER_KEY, null);
            if (json != null) {
                try {
                    order = mapper.readValue(json, Order.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
            json = sharedPreferences.getString(MySharedPreferences.TRIP_KEY, null);
            if (json != null) {
                try {
                    trip = mapper.readValue(json, Trip.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }

        public void updateUser(User user) {
            if (user != null && !TextUtils.isEmpty(user.id)) {
                try {
                    String json = mapper.writeValueAsString(user);
                    sharedPreferences.edit()
                            .putString(MySharedPreferences.USER_KEY, json)
                            .apply();
                    this.user = user;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                sharedPreferences.edit().remove(MySharedPreferences.USER_KEY).apply();
                this.user = null;
            }
        }

        public void updateOrder(Order order) {
            if (order != null && !TextUtils.isEmpty(order.id)) {
                try {
                    String json = mapper.writeValueAsString(order);
                    sharedPreferences.edit()
                            .putString(MySharedPreferences.ORDER_KEY, json)
                            .apply();
                    this.order = order;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                sharedPreferences.edit().remove(MySharedPreferences.ORDER_KEY).apply();
                this.order = null;
            }
        }

        public void updateTrip(Trip trip) {
            if (trip != null && !TextUtils.isEmpty(trip.getTripId())) {
                try {
                    String json = mapper.writeValueAsString(trip);
                    sharedPreferences.edit()
                            .putString(MySharedPreferences.TRIP_KEY, json)
                            .apply();
                    this.trip = trip;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else {
                sharedPreferences.edit().remove(MySharedPreferences.TRIP_KEY).apply();
                this.trip = null;
            }
        }
    }
}
