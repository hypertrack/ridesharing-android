package com.hypertrack.uber_consumer.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.Action;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.tracking.CTAButton;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.basemvp.BaseView;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.base.MyHyperTrackMapAdapter;
import com.hypertrack.uber_consumer.models.StatusEnum;
import com.hypertrack.uber_consumer.models.Trip;
import com.hypertrack.uber_consumer.utils.Constants;
import com.hypertrack.uber_consumer.utils.SharedValues;
import com.hypertrack.uber_consumer.utils.Utils;
import com.hypertrack.uber_consumer.widgets.DetailsPanelView;

import java.util.List;

/**
 * Created by pkharche on 08/04/18.
 */
public class TrackRideActivity extends BaseActivity {
    private static final int REQUEST_CALL_PHONE_PERMISSION = 1001;

    private Trip mTripObject = null;

    private HyperTrackMapFragment mHyperTrackMapFragment;
    private MyHyperTrackMapAdapter myHyperTrackMapAdapter;

    private View mBottomView;
    private BaseView mBaseView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_ride);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationServices();
    }

    @Override
    protected void locationServicesEnabled() {
        super.locationServicesEnabled();
        getRefreshedRide();
        observerRide();
    }

    @Override
    protected void onPause() {
        super.onPause();
        HyperTrack.removeActions(null);
    }

    //HyperTrack
    private void setUpHyperTrackBottomView() {

        mHyperTrackMapFragment = (HyperTrackMapFragment) getSupportFragmentManager().findFragmentById(R.id.htMapfragment);
        mHyperTrackMapFragment.setMapStyle(R.raw.mapstyle_uberx);

        myHyperTrackMapAdapter = new MyHyperTrackMapAdapter(mContext);
        mHyperTrackMapFragment.setMapAdapter(myHyperTrackMapAdapter);

        mBaseView = DetailsPanelView.getInstance();
        mHyperTrackMapFragment.setUseCase(mBaseView);

        mHyperTrackMapFragment.setMapCallback(new MapFragmentCallback() {

            @Override
            public void onBottomBaseViewCreated(int useCaseType) {
                setAndFillDataInBottomView();
                mBaseView.hideCTAButton();
                showActionStatus();
            }
        });
    }

    private void setAndFillDataInBottomView() {
        mBottomView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_user_panel, null);
        mBaseView.addBottomView(mBottomView);

        if (mTripObject != null) {

            //Fill UberX customer details
            if (mTripObject.getDriver() != null) {
                ((TextView) mBottomView.findViewById(R.id.tv_user_name)).setText(mTripObject.getDriver().getName());
                ((TextView) mBottomView.findViewById(R.id.tv_vehicle_details)).setText(mTripObject.getDriver().getVehicle() + " | " + mTripObject.getDriver().getVehicleNo());

                if(!TextUtils.isEmpty(mTripObject.getDriver().getPhone())) {
                    mBottomView.findViewById(R.id.tv_cta).setVisibility(View.VISIBLE);
                    mBottomView.findViewById(R.id.tv_cta).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            callUser();
                        }
                    });
                }

                ImageView ivUserImage = (ImageView) mBottomView.findViewById(R.id.iv_user_image);
                RequestOptions requestOptions = new RequestOptions();
                requestOptions.placeholder(R.drawable.profile_avatar);
                requestOptions.error(R.drawable.profile_avatar);

                boolean isFinishing = isFinishing();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    isFinishing = isFinishing() || isDestroyed();
                }

                if (!isFinishing) {
                    Glide.with(mContext)
                            .load(mTripObject.getDriver().getImageUrl())
                            .apply(requestOptions)
                            .into(ivUserImage);
                }
                mBottomView.findViewById(R.id.ll_user_panel).setVisibility(View.VISIBLE);
            }
        }
    }

    //Set up the Call to Action button provided by HyperTrack bottom view
    private void setCallToActionButtonInBottomView(Boolean isTripDone) {
        mBaseView.setCTAButtonArrowVisibility(false);

        if(isTripDone) {
            //View when trip is completed --> BOOK NEW RIDES
            mBaseView.showCTAButton();
            mBaseView.setCTATitleIconResource(R.drawable.directions_button);
            mBaseView.setCTAButtonTitle(getString(R.string.book_another_ride));
            mBaseView.setCTAButtonClickListener(new CTAButton.OnClickListener() {
                @Override
                public void onTitleButtonClick() {
                    SharedValues.resetTripValues(mContext);
                    launchActivity(GenerateRideActivity.class);
                }

                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                }
            });

        } else if(mTripObject.getStatus() == StatusEnum.started_to_drop_off_customer) { //Show Share icon only while Drop action is going on
            //Initial view when trip is going on --> SHARE RIDE
            mBaseView.showCTAButton();
            mBaseView.setCTATitleIconResource(R.drawable.ic_share);
            mBaseView.setCTAButtonTitle(getString(R.string.share_trip));
            mBaseView.setCTAButtonClickListener(new CTAButton.OnClickListener() {
                @Override
                public void onTitleButtonClick() {
                    shareTrip();
                }

                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                }
            });

        } else {
            mBaseView.hideCTAButton();
        }
    }

    private void shareTrip() {
        if(mTripObject != null && mTripObject.getHypertrack() != null && !TextUtils.isEmpty(mTripObject.getHypertrack().getDropTrackingUrl())) {
            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, "Hey! Track " + mTripObject.getUser().getName() + "\'s UberX ride : " + mTripObject.getHypertrack().getDropTrackingUrl());
            sendIntent.setType("text/plain");
            startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share_trip)));
        }
    }

    /**
     * Observe for any Trip status changes on Firebase/ server.
     *
     * This can be handled by slient push as well.
     */
    private void observerRide() {
        Utils.showProgressDialog(mActivity, false);

        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        //Log.d(Constants.TAG,"StartRide:  getRide: rideId:  " +rideId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);

        ridesRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(Constants.TAG,"onChildChanged:  " +dataSnapshot);

                if(dataSnapshot != null && dataSnapshot.getKey() != null && dataSnapshot.getKey().equals("status")) {
                    Log.d(Constants.TAG,"onChildChanged:  status changed:  " +dataSnapshot);
                    //if status has changed, get update Trip object (updated Trip object will have drop_action_id)
                    getRefreshedRide();
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) { }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

            @Override
            public void onCancelled(DatabaseError databaseError) { }
        });
    }

    /**
     * Get Trip details as saved on Firebase/ server
     */
    private void getRefreshedRide() {
        Utils.showProgressDialog(mActivity, false);

        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        //Log.d(Constants.TAG,"StartRide:  getRide: rideId:  " +rideId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);

        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Utils.hideProgressDialog();

                mTripObject = dataSnapshot.getValue(Trip.class);

                if(mTripObject != null) {
                    Log.d(Constants.TAG, "getRefreshedRide:   " + mTripObject.getStatus());
                }

                if(mHyperTrackMapFragment == null) {
                    setUpHyperTrackBottomView();
                } else {
                    showActionStatus();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                showSnackBar("Firebase Error finding Trip to Start : " + databaseError.getMessage());
                Utils.hideProgressDialog();
            }
        });
    }

    private void showActionStatus() {
        if (mTripObject != null) {
            Log.e(Constants.TAG, "showActionStatus : Trip  " + mTripObject.getId() +"  status  " + mTripObject.getStatus());

            StatusEnum status = mTripObject.getStatus();
            switch (status) {
                case trip_completed:

                    showTripSummaryView();
                    //showSnackBar("Thanks for taking a ride with us. Please provide feedback regarding your ride", false);
                    break;

                case started_to_drop_off_customer:
                    setCallToActionButtonInBottomView(false);

                    //showSnackBar("Relax & enjoy your ride with " +mTripObject.getDriver().getName(), false);
                    if(mTripObject.getHypertrack() != null) {
                        trackAction(mTripObject.getHypertrack().getDropUniqueId());
                    }
                    break;

                case customer_pickup_completed:
                    //showSnackBar(mTripObject.getDriver().getName() + " has arrived at " + mTripObject.getPickup().getDisplayAddress(), false);
                    break;

                case started_to_pick_up_customer:
                    //showSnackBar(mTripObject.getDriver().getName() + " has started to pick you", false);
                    if(mTripObject.getHypertrack() != null) {
                        trackAction(mTripObject.getHypertrack().getPickupUniqueId());
                    }
                    break;

                case trip_assigned:
                    //showSnackBar(mTripObject.getDriver().getName() + " is assigned to your ride", false);
                    break;

                case trip_not_started:
                    //showSnackBar("Trip is not yet assigned to any driver", false);
                    break;
                }

        } else {
            showSnackBar("No trip booked");
            SharedValues.resetTripValues(mContext);
            launchActivity(GenerateRideActivity.class);
        }
    }

    private void trackAction(final String uniqueId) {
        Log.d(Constants.TAG, "trackAction:: uniqueId:  " + uniqueId);

        HyperTrack.trackActionByUniqueId(uniqueId, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Log.d(Constants.TAG,"trackAction::  onSuccess: uniqueId:  " + uniqueId);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                Log.e(Constants.TAG, "trackAction: onError: uniqueId:  " + uniqueId + "   " + errorResponse.getErrorCode() + "   " + errorResponse.getErrorMessage());
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CALL_PHONE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callUser();

            } else {
                // Handle Call Permission denied error
                showSnackBar("Call Permission denied");
            }
        }
    }

    private void callUser() {
        if (mTripObject != null && mTripObject.getDriver() != null && !TextUtils.isEmpty(mTripObject.getUser().getPhone())) {

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
                return;

            } else {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mTripObject.getDriver().getPhone()));
                startActivity(intent);
            }
        }
    }

    //This view is summary of the Trip.
    //This is custom view as per 3rd party developer wants
    //comment the updateActionSummary in DetailsPanelview (your view) so we dont see HyperTrack's summary view
    private void showTripSummaryView() {
        Utils.showProgressDialog(mActivity, false);

        myHyperTrackMapAdapter.setShowActionRoute(true);

        mBaseView.removeBottomView();

        HyperTrack.getAction(mTripObject.getHypertrack().getDropActionId(), new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Utils.hideProgressDialog();

                final com.hypertrack.lib.models.Action action = (com.hypertrack.lib.models.Action) response.getResponseObject();
                if(action != null) {
                    setCallToActionButtonInBottomView(true);

                    View tripSummaryview = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_trip_summary, null);
                    mBaseView.addBottomView(tripSummaryview);
                    if (action.getDistanceInKm() != null) {
                        ((TextView) tripSummaryview.findViewById(R.id.tv_distance)).setText(String.valueOf(action.getDistanceInKm()) + "kms");
                    } else {
                        ((TextView) tripSummaryview.findViewById(R.id.tv_distance)).setText(getString(R.string.no_data));
                    }
                    ((TextView) tripSummaryview.findViewById(R.id.tv_time)).setText(String.valueOf(action.getTotalDurationInMinutes()) + "mins");
                    ((TextView) tripSummaryview.findViewById(R.id.tv_name)).setText(mTripObject.getDriver().getName());
                    ((TextView) tripSummaryview.findViewById(R.id.tv_category)).setText(mTripObject.getDriver().getVehicleNo());
                }
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                Utils.hideProgressDialog();
            }
        });
    }
}
