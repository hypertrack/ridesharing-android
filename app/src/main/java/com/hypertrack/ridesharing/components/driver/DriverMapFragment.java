package com.hypertrack.ridesharing.components.driver;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hypertrack.sdk.TrackingError;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.adapters.OrdersAdapter;
import com.hypertrack.ridesharing.components.MapFragment;
import com.hypertrack.ridesharing.models.Order;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.views.Dialog;
import com.hypertrack.ridesharing.views.Snackbar;

import java.util.Collection;

public class DriverMapFragment extends MapFragment<DriverMapPresenter> implements DriverMapPresenter.DriverView {
    private static final String TAG = "DriverMapFragment";

    private Dialog acceptRideDialog;
    private Snackbar infoDriverSnackbar;

    public static SupportMapFragment newInstance() {
        SupportMapFragment fragment = new DriverMapFragment();
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        presenter = new DriverMapPresenter(getActivity(), this);
        return super.onCreateView(layoutInflater, viewGroup, bundle);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        infoOrderSnackbar.setAction(R.id.get_directions, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.getDirections();
            }
        });
        ordersAdapter.setOnItemClickListener(new OrdersAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.Adapter<?> adapter, View view, int position) {
                recyclerView.setVisibility(View.INVISIBLE);
                presenter.selectOrder(ordersAdapter.getOrder(position).id);
            }
        });
        acceptRideDialog = new Dialog(getActivity(), R.layout.dialog_accept_ride)
                .setAction(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        presenter.acceptRide();
                        acceptRideDialog.dismiss();
                        ordersAdapter.clear();
                        ordersAdapter.notifyDataSetChanged();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                })
                .setAction(R.id.cancel, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        acceptRideDialog.dismiss();
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                });
        infoDriverSnackbar = Snackbar.make(view, R.layout.snackbar_driver_info, Snackbar.LENGTH_INDEFINITE);
    }

    @Override
    public Marker addMarker(Order order) {
        MarkerOptions options = new MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.iconmap_marker_filled))
                .position(new LatLng(order.pickup.latitude, order.pickup.longitude));
        return mGoogleMap.addMarker(options);
    }

    @Override
    public void updateNotifications(Collection<Order> orders) {
        ordersAdapter.addAll(orders);
        ordersAdapter.notifyDataSetChanged();
    }

    @Override
    public void showAcceptRide(Order order) {
        if (order != null) {
            recyclerView.setVisibility(View.INVISIBLE);
            TextView pickupAddress = acceptRideDialog.findViewById(R.id.pickup_address);
            TextView riderName = acceptRideDialog.findViewById(R.id.rider_name);
            pickupAddress.setText(order.pickup.address);
            riderName.setText(order.rider.name);
            acceptRideDialog.show();
        }
    }

    @Override
    public void showDriverInfo(User user) {
        if (user != null) {
            TextView name = infoDriverSnackbar.getView().findViewById(R.id.name);
            name.setText(user.name);
            presenter.addSnackbar(infoDriverSnackbar);
        }
    }

    @Override
    public void dismissDriverInfo() {
        presenter.removeSnackbar(infoDriverSnackbar);
    }

    @Override
    public void showInfoUserStartTripButton() {
        infoOrderSnackbar.setAction(R.id.start_trip, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.startRide();
            }
        });
    }

    @Override
    public void hideInfoUserStartTripButton() {
        infoOrderSnackbar.setAction(R.id.start_trip, null);
    }

    @Override
    public void showInfoUserEndTripButton() {
        infoOrderSnackbar.setAction(R.id.end_trip, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.endRide();
            }
        });
    }

    @Override
    public void hideInfoUserEndTripButton() {
        infoOrderSnackbar.setAction(R.id.end_trip, null);

    }

    @Override
    public void onError(TrackingError trackingError) {

    }

    @Override
    public void onTrackingStart() {

    }

    @Override
    public void onTrackingStop() {

    }
}
