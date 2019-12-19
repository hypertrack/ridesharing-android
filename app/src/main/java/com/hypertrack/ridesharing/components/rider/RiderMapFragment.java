package com.hypertrack.ridesharing.components.rider;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.components.MapFragment;
import com.hypertrack.ridesharing.models.Place;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.views.Snackbar;


public class RiderMapFragment extends MapFragment<RiderMapPresenter> implements RiderMapPresenter.RiderView {
    private static final String TAG = "RiderMapFragment";

    private Snackbar chooseDestSnackbar;
    private Snackbar bookRideSnackbar;
    private TextView pickupText;
    private TextView dropoffText;
    private TextView dropoffText2;

    public static SupportMapFragment newInstance() {
        SupportMapFragment fragment = new RiderMapFragment();
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        presenter = new RiderMapPresenter(getActivity(), this);
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((TextView)stateSnackbar.getView().findViewById(R.id.text)).setText(R.string.finding_drivers);
        stateSnackbar.setAction(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showOrderCancelDialog();
            }
        });

        chooseDestSnackbar = Snackbar.make(view, R.layout.snackbar_choose_dest, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.id.dropoff, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.chooseDropoff();
                    }
                });

        bookRideSnackbar = Snackbar.make(view, R.layout.snackbar_book_ride, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.id.pickup, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.choosePickup();
                    }
                })
                .setAction(R.id.dropoff, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.chooseDropoff();
                    }
                })
                .setAction(R.id.my_location, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.setMyLocationAsPickup();
                    }
                })
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.orderTaxi();
                    }
                });
        dropoffText = chooseDestSnackbar.getView().findViewById(R.id.dropoff);
        dropoffText2 = bookRideSnackbar.getView().findViewById(R.id.dropoff_text);
        pickupText = bookRideSnackbar.getView().findViewById(R.id.pickup_text);
    }

    @Override
    public void showChooseDest() {
        presenter.addSnackbar(chooseDestSnackbar);
    }

    @Override
    public void dismissChooseDest() {
        presenter.removeSnackbar(chooseDestSnackbar);
    }

    @Override
    public void showBookRide() {
        presenter.addSnackbar(bookRideSnackbar);
    }

    @Override
    public void dismissBookRide() {
        presenter.removeSnackbar(bookRideSnackbar);
    }

    @Override
    public Marker addMarker(User driver) {
        MarkerOptions options = new MarkerOptions()
                .anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car_marker_dark))
                .rotation(driver.location.getBearing())
                .position(new LatLng(driver.location.getLatitude(), driver.location.getLongitude()));
        return mGoogleMap.addMarker(options);
    }

    @Override
    public Marker addMarker(Place place, int iconResId) {
        MarkerOptions options = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(iconResId))
                .draggable(true)
                .anchor(0.5f, 0.5f)
                .position(new LatLng(place.latitude, place.longitude));
        return mGoogleMap.addMarker(options);
    }

    @Override
    public void updatePickup(Place place) {
        if (place == null) {
            pickupText.setText("");
        } else {
            pickupText.setText(TextUtils.isEmpty(place.preview) ? place.address : place.preview);
        }
    }

    @Override
    public void updateDropoff(Place place) {
        if (place == null) {
            dropoffText.setText("");
            dropoffText2.setText("");
        } else {
            dropoffText.setText(place.address);
            dropoffText2.setText(place.address);
        }
    }
}
