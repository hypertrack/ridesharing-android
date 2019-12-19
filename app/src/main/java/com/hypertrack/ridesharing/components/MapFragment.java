package com.hypertrack.ridesharing.components;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hypertrack.sdk.views.dao.Trip;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.adapters.OrdersAdapter;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.views.Snackbar;

import java.util.concurrent.TimeUnit;

public abstract class MapFragment<P extends MapPresenter> extends SupportMapFragment
        implements OnMapReadyCallback, MapPresenter.View {
    private static final String TAG = "MapFragment";

    protected P presenter;
    protected GoogleMap mGoogleMap;
    private FloatingActionButton locationButton;

    protected Snackbar stateSnackbar;
    protected Snackbar infoOrderSnackbar;
    protected Snackbar tripEndSnackbar;
    protected RecyclerView recyclerView;
    protected View blockingView;

    protected OrdersAdapter ordersAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.fragment_map, null);
        View mapView = super.onCreateView(layoutInflater, viewGroup, bundle);
        ((FrameLayout) view.findViewById(R.id.map_frame))
                .addView(mapView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
        recyclerView.setLayoutManager(layoutManager);
        ordersAdapter = new OrdersAdapter();
        recyclerView.setAdapter(ordersAdapter);
        blockingView = view.findViewById(R.id.blocking_view);

        stateSnackbar = Snackbar.make(view, R.layout.snackbar_state, Snackbar.LENGTH_INDEFINITE);
        infoOrderSnackbar = Snackbar.make(view, R.layout.snackbar_order_info, Snackbar.LENGTH_INDEFINITE);
        tripEndSnackbar = Snackbar.make(view, R.layout.snackbar_trip_end_info, Snackbar.LENGTH_INDEFINITE);
        locationButton = view.findViewById(R.id.locationButton);
        locationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                presenter.setCameraFixedEnabled(true);
                blockingView.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        presenter.setCameraFixedEnabled(false);
                        blockingView.setOnTouchListener(null);
                        return false;
                    }
                });
            }
        });

        getMapAsync(this);
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                presenter.onViewReady();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        presenter.map(googleMap);
    }

    @Override
    public void showState() {
        presenter.addSnackbar(stateSnackbar);
    }

    @Override
    public void dismissState() {
        presenter.removeSnackbar(stateSnackbar);
    }

    public void showOrderCancelDialog() {
        if (getActivity() != null) {
            AlertDialog cancelledOrderAlert = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.cancel_ride)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            presenter.cancelOrder();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            cancelledOrderAlert.show();
        }
    }

    @Override
    public void showOrderInfo(final User user, CharSequence address) {
        if (user != null) {
            TextView destination = infoOrderSnackbar.getView().findViewById(R.id.destination);
            destination.setText(address);
            TextView name = infoOrderSnackbar.getView().findViewById(R.id.name);
            name.setText(user.name);
            TextView rating = infoOrderSnackbar.getView().findViewById(R.id.rating);
            RatingBar ratingBar = infoOrderSnackbar.getView().findViewById(R.id.rating_bar);
            float ratingIndex = 4.8f;
            rating.setText(String.valueOf(ratingIndex));
            ratingBar.setRating(ratingIndex);

            infoOrderSnackbar.setAction(R.id.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showOrderCancelDialog();
                }
            });
            infoOrderSnackbar.setAction(R.id.call, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);

                    intent.setData(Uri.parse("tel:" + user.phoneNumber));
                    startActivity(intent);
                }
            });
            presenter.addSnackbar(infoOrderSnackbar);
        }
    }

    @Override
    public void dismissOrderInfo() {
        presenter.removeSnackbar(infoOrderSnackbar);
    }

    @Override
    public void showTripEndInfo(Trip trip, User user) {
        TextView header = tripEndSnackbar.getView().findViewById(R.id.header);
        String headerText = User.USER_ROLE_DRIVER.equals(user.role) ?
                getString(R.string.find_new_rides) : getString(R.string.book_another_ride);
        header.setText(headerText);
        if (trip != null) {
            if (trip.getSummary() != null) {
                TextView distance = tripEndSnackbar.getView().findViewById(R.id.distance);
                double miles = trip.getSummary().getDistance() * 0.000621371;
                distance.setText(String.format(getString(R.string.miles), miles));
                TextView rideTime = tripEndSnackbar.getView().findViewById(R.id.ride_time);
                long mins = TimeUnit.SECONDS.toMinutes(trip.getSummary().getDuration());
                rideTime.setText(String.format(getString(R.string.mins), mins));
                TextView fare = tripEndSnackbar.getView().findViewById(R.id.fare);
                fare.setText(String.format(getString(R.string.fare), (int) (miles * 2)));
            }
            tripEndSnackbar.setAction(R.id.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.closeCompletedOrder();
                }
            });
            TextView riderName = tripEndSnackbar.getView().findViewById(R.id.name);
            riderName.setText(user.name);

            presenter.addSnackbar(tripEndSnackbar);
        }
    }

    @Override
    public void dismissTripEndInfo() {
        presenter.removeSnackbar(tripEndSnackbar);
    }

    @Override
    public void showAlertDialog(final String key, int textResId) {
        if (getActivity() != null) {
            AlertDialog cancelledOrderAlert = new AlertDialog.Builder(getActivity())
                    .setTitle(textResId)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            presenter.onAlertDialogDismissed(key);
                        }
                    })
                    .create();
            cancelledOrderAlert.show();
        }
    }

    @Override
    public void showUI() {
        locationButton.show();
    }

    @Override
    public void hideUI() {
        locationButton.hide();
    }

    @Override
    public void showProgressBar() {
    }

    @Override
    public void hideProgressBar() {
    }

    @Override
    public void showNotification(int textResId) {
        if (getActivity() != null) {
            Toast.makeText(getActivity(), textResId, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        presenter.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.destroy();
    }
}
