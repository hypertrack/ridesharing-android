package com.hypertrack.uber_driver.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.hypertrack.uber_driver.R;
import com.hypertrack.uber_driver.base.MyHyperTrackMapAdapter;
import com.hypertrack.uber_driver.models.Driver;
import com.hypertrack.uber_driver.models.StatusEnum;
import com.hypertrack.uber_driver.utils.Constants;
import com.hypertrack.uber_driver.utils.SharedValues;
import com.hypertrack.uber_driver.utils.Utils;
import com.hypertrack.uber_driver.widgets.DetailsPanelView;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.Place;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.basemvp.BaseView;
import com.hypertrack.lib.tracking.model.MarkerModel;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pkharche on 08/04/18.
 */
public class FindRideActivity extends BaseActivity {

    private HyperTrackMapFragment mHyperTrackMapFragment;
    private BaseView mBaseView;
    private View mBottomView;
    private Driver mDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_ride);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);

        //timer();
    }

    /*private void timer() {
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                Utils.hideProgressDialog(); // when the task active then close the dialog
                t.cancel(); // also just top the timer thread, otherwise, you may receive a crash report
            }
        }, 3000); // after 3 second (or 3000 miliseconds), the task will be active.

    }*/

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationServices();
    }

    @Override
    protected void locationServicesEnabled() {
        super.locationServicesEnabled();
        setUpHyperTrackBottomView();
    }

    private void searchRide() {
        Utils.showProgressDialog(mActivity, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference();

        Query query = ridesRef.child(Constants.FIREBASE_TRIPS)
                        .orderByChild(Constants.FIREBASE_TRIP_STATUS)
                        .equalTo(StatusEnum.trip_not_started.toString())
                        .limitToFirst(1);

        query.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if(dataSnapshot != null) {
                    if (!TextUtils.isEmpty(dataSnapshot.getKey())) {
                        SharedValues.saveValue(mContext, Constants.TRIP_ID, dataSnapshot.getKey());

                        Log.d(Constants.TAG,"dataSnapshot:   " +dataSnapshot);
                        launchActivity(AcceptRideActivity.class);
                    } else {
                        showSnackBar(getString(R.string.no_ride_found));
                    }
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //HyperTrack
    private void setUpHyperTrackBottomView() {
        mHyperTrackMapFragment = (HyperTrackMapFragment) getSupportFragmentManager().findFragmentById(R.id.htMapfragment);
        mHyperTrackMapFragment.setMapStyle(R.raw.mapstyle_uberx);

        MyHyperTrackMapAdapter adapter = new MyHyperTrackMapAdapter(mContext);
        mHyperTrackMapFragment.setMapAdapter(adapter);

        mBaseView = DetailsPanelView.getInstance(); //This is the view which will be shown as bottom view
        mHyperTrackMapFragment.setUseCase(mBaseView);

        mHyperTrackMapFragment.setMapCallback(new MapFragmentCallback() {
            @Override
            public void onMapReadyCallback(Context context, GoogleMap map) {
                super.onMapReadyCallback(context, map);
                showCurrentLocation();
            }

            @Override
            public void onBottomBaseViewCreated(int useCaseType) {
                setAndFillDataInBottomView();
                setCallToActionButtonInBottomView();
            }
        });
    }

    private void showCurrentLocation() {
        Utils.showProgressDialog(mActivity, false);

        HyperTrack.getCurrentLocation(new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Utils.hideProgressDialog();

                Location location = (Location) response.getResponseObject();
                showMarker(location);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                Utils.hideProgressDialog();
            }
        });
    }

    private void showMarker(Location location) {
        com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());
        MarkerModel pickUpMarker = new MarkerModel(latLng, R.drawable.icondrive, MarkerModel.Type.CUSTOM);
        mHyperTrackMapFragment.addOrUpdateMarker(pickUpMarker);
    }

    private void setAndFillDataInBottomView() {
        mBottomView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_user_panel, null); //bottom_layout_user_panel
        mBaseView.addBottomView(mBottomView);

        if(isInternetConnected()) {
            searchDriver();
        } else {
            showNetworkError();
        }
    }

    //Set up the Call to Action button provided by HyperTrack bottom view
    private void setCallToActionButtonInBottomView() {
        mBaseView.setCTAButtonArrowVisibility(false);
        mBaseView.setCTAButtonTitle(getString(R.string.cta_text_finding_riders_near_you));
        mBaseView.showCTAButton();
    }

    private void searchDriver() {
        Utils.showProgressDialog(mActivity, false);

        String driverId = SharedValues.getValue(mContext, Constants.DRIVER_ID);
        Log.d(Constants.TAG,"FindRide:  driverId " +driverId);

        if(!TextUtils.isEmpty(driverId)) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_DRIVERS);
            Query query = ridesRef.child(driverId);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();

                    mDriver = dataSnapshot.getValue(Driver.class);
                    fillDriverData();

                    if(isInternetConnected()) {
                        searchRide();
                    } else {
                        showNetworkError();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Firebase Error finding driver details: " + databaseError.getMessage());
                    Utils.hideProgressDialog();
                }
            });
        } else {
            showSnackBar("No driver available");
            logOutFirebaseUser();
        }
    }

    private void fillDriverData() {
        if (mDriver != null && mBottomView != null) {
            mBottomView.findViewById(R.id.tv_address_title).setVisibility(View.GONE);
            mBottomView.findViewById(R.id.tv_address_details).setVisibility(View.GONE);

            ((TextView) mBottomView.findViewById(R.id.tv_user_name)).setText(mDriver.getName());
            ((TextView) mBottomView.findViewById(R.id.tv_user_rating)).setText(String.valueOf(mDriver.getRating()));
            ((RatingBar) mBottomView.findViewById(R.id.tv_user_rating_bar)).setRating(mDriver.getRating());

            ImageView ivUserImage = mBottomView.findViewById(R.id.iv_user_image);
            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.profile_avatar);
            requestOptions.error(R.drawable.profile_avatar);

            boolean isFinishing = isFinishing();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                isFinishing = isFinishing() || isDestroyed();
            }

            if (!isFinishing) {
                Glide.with(mContext)
                        .load(mDriver.getImageUrl())
                        .apply(requestOptions)
                        .into(ivUserImage);
            }
            mBottomView.findViewById(R.id.ll_user_panel).setVisibility(View.VISIBLE);
            mBottomView.findViewById(R.id.bottom_layout_user_panel).setVisibility(View.VISIBLE);
        }
    }
}
