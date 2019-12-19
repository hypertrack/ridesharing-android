package com.hypertrack.ridesharing.tracking;

import android.content.Context;
import android.location.LocationListener;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.hypertrack.sdk.views.DeviceUpdatesHandler;
import com.hypertrack.sdk.views.HyperTrackViews;
import com.hypertrack.sdk.views.dao.MovementStatus;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.sdk.views.maps.HyperTrackMap;
import com.hypertrack.sdk.views.maps.widget.MapAdapter;

@SuppressWarnings({"unused", "WeakerAccess"})
public class MapViewsPresenter {
    private static final String TAG = "MapViewsPresenter";

    protected final Context mContext;
    private final View mView;
    private final State mState;

    private HyperTrackViews hyperTrackViews;
    private HyperTrackMap hyperTrackMap;

    public MapViewsPresenter(@NonNull Context context, @NonNull View view, @NonNull String hyperTrackPubKey) {
        mContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        mView = view;
        mState = new State(hyperTrackPubKey);

        hyperTrackViews = HyperTrackViews.getInstance(mContext, mState.getHyperTrackPubKey());
    }

    public void map(@NonNull MapAdapter mapAdapter) {
        hyperTrackMap = HyperTrackMap.getInstance(mContext, mapAdapter);
    }

    public void subscribe(@NonNull String deviceId) {
        if (!TextUtils.isEmpty(deviceId)) {
            hyperTrackViews.getDeviceMovementStatus(deviceId, new Consumer<MovementStatus>() {
                @Override
                public void accept(MovementStatus movementStatus) {
                    for (Trip item : movementStatus.trips) {
                        if (item.getStatus().equals("active")) {
                            mState.setTripId(item.getTripId());
                            mView.onTripChanged(item);
                            break;
                        }
                    }
                }
            });
            hyperTrackMap.bind(hyperTrackViews, deviceId);
        }
    }

    public void setLocationUpdatesListener(LocationListener locationListener) {
        if (hyperTrackMap != null) {
            hyperTrackMap.setLocationUpdatesListener(locationListener);
        }
    }

    public void subscribeToDeviceUpdates(String deviceId, DeviceUpdatesHandler deviceUpdatesHandler) {
        hyperTrackViews.subscribeToDeviceUpdates(deviceId, deviceUpdatesHandler);
    }

    public void moveToMyLocation() {
        if (hyperTrackMap != null) {
            hyperTrackMap.moveToMyLocation();
        }
    }

    public void setMyLocationEnabled(boolean enabled) {
        if (hyperTrackMap != null) {
            hyperTrackMap.setMyLocationEnabled(enabled);
        }
    }

    public void destroy() {
        if (hyperTrackMap != null) {
            hyperTrackMap.destroy();
            hyperTrackMap = null;
        }
    }

    public interface View {

        void onTripChanged(Trip trip);

        void showProgressBar();

        void hideProgressBar();
    }

    public static class State {
        private final String hyperTrackPubKey;
        private String tripId;

        @NonNull
        public String getHyperTrackPubKey() {
            return hyperTrackPubKey;
        }

        @Nullable
        public String getTripId() {
            return tripId;
        }

        public void setTripId(String tripId) {
            this.tripId = tripId;
        }

        public State(@NonNull String hyperTrackPubKey) {
            this.hyperTrackPubKey = hyperTrackPubKey;
        }
    }
}
