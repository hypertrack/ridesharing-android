package com.hypertrack.driverapp.activities;

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
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hypertrack.driverapp.R;
import com.hypertrack.driverapp.base.MyHyperTrackMapAdapter;
import com.hypertrack.driverapp.models.HypertrackDetails;
import com.hypertrack.driverapp.models.StatusEnum;
import com.hypertrack.driverapp.models.Trip;
import com.hypertrack.driverapp.utils.Constants;
import com.hypertrack.driverapp.utils.SharedValues;
import com.hypertrack.driverapp.utils.Utils;
import com.hypertrack.driverapp.widgets.DetailsPanelView;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.internal.consumer.utils.AnimationUtils;
import com.hypertrack.lib.models.Action;
import com.hypertrack.lib.models.ActionParams;
import com.hypertrack.lib.models.ActionParamsBuilder;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.GeoJSONLocation;
import com.hypertrack.lib.models.Place;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.tracking.CTAButton;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.basemvp.BaseView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pkharche on 08/04/18.
 */
public class StartRideActivity extends BaseActivity {
    private static final int REQUEST_CALL_PHONE_PERMISSION = 1001;

    private Trip mTripObject = null;

    private HyperTrackMapFragment mHyperTrackMapFragment;
    private MyHyperTrackMapAdapter myHyperTrackMapAdapter;

    private View mBottomView;
    private BaseView mBaseView;
    private View mBtStartRide;
    private View mBtEndRide;
    private View mStartRideAnimationView;
    private View mEndRideAnimationView;

    private void onStartRideClicked() {
        markActionComplete(mTripObject.getHypertrack().getPickupActionId(), mTripObject.getHypertrack().getPickupUniqueId(), true);
    }

    private void onEndRideClicked() {
        markActionComplete(mTripObject.getHypertrack().getDropActionId(), mTripObject.getHypertrack().getDropUniqueId(), false);
    }

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
        getRide();
    }

    @Override
    protected void onPause() {
        super.onPause();
        HyperTrack.removeActions(null);
    }

    private void updateFireBase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference tripRef = database.getReference(Constants.FIREBASE_TRIPS);
        tripRef.child(mTripObject.getId()).setValue(mTripObject, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

            }
        });
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
                //super.onBottomBaseViewCreated(useCaseType);

                setAndFillDataInBottomView();
                setCallToActionButtonInBottomView(false);
                showActionStatus();
            }

            @Override
            public void onActionStatusChanged(List<String> changedStatusActionIds, List<Action> changedStatusActions) {
                super.onActionStatusChanged(changedStatusActionIds, changedStatusActions);

                for(Action action : changedStatusActions) {
                    Log.i(Constants.TAG, "action:  " +action.getId()+"  uniqueId:  " + action.getUniqueId()+"  status: " +action.getStatus());
                }
                actionStatusChanged(changedStatusActions);
            }

            @Override
            public void onErrorOccured() {
                super.onErrorOccured();
            }
        });
    }

    private void setAndFillDataInBottomView() {
        mBottomView = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_user_panel, null);
        mBaseView.addBottomView(mBottomView);

        if (mTripObject != null) {

            //Fill UberX customer details
            if (mTripObject.getUser() != null) {
                ((TextView) mBottomView.findViewById(R.id.tv_user_name)).setText(mTripObject.getUser().getName());
                ((TextView) mBottomView.findViewById(R.id.tv_user_rating)).setText(String.valueOf(mTripObject.getUser().getRating()));
                ((RatingBar) mBottomView.findViewById(R.id.tv_user_rating_bar)).setRating(mTripObject.getUser().getRating());

                if(!TextUtils.isEmpty(mTripObject.getUser().getPhone())) {
                    mBottomView.findViewById(R.id.tv_cta).setVisibility(View.VISIBLE);
                    mBottomView.findViewById(R.id.tv_cta).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            callUser();
                        }
                    });
                }

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
                            .load(mTripObject.getUser().getImageUrl())
                            .apply(requestOptions)
                            .into(ivUserImage);
                }
                mBottomView.findViewById(R.id.ll_user_panel).setVisibility(View.VISIBLE);
                mBottomView.findViewById(R.id.ll_user_panel).setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
            }


            //Fill UberX customer address details
            TextView addressTitle = mBottomView.findViewById(R.id.tv_address_title);
            addressTitle.setVisibility(View.VISIBLE);

            TextView addressDetails = mBottomView.findViewById(R.id.tv_address_details);
            addressDetails.setVisibility(View.VISIBLE);

            switch (mTripObject.getStatus()) {
                case trip_assigned:
                case started_to_pick_up_customer:
                    addressTitle.setText("PICKUP");
                    addressDetails.setText(mTripObject.getPickup().getDisplayAddress());
                    break;

                case customer_pickup_completed:
                case started_to_drop_off_customer:
                    addressTitle.setText("DROPOFF");
                    addressDetails.setText(mTripObject.getDrop().getDisplayAddress());
                    break;
            }

            //Get the buttons to control the trip/ actions
            mBtStartRide = mBottomView.findViewById(R.id.bt_start_ride);
            mBtStartRide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onStartRideClicked();
                }
            });
            mBtEndRide = mBottomView.findViewById(R.id.bt_end_ride);
            mBtEndRide.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onEndRideClicked();
                }
            });

            //These views are created for animation purpose only
            mStartRideAnimationView = mBottomView.findViewById(R.id.ll_start_ride);
            mEndRideAnimationView = mBottomView.findViewById(R.id.ll_end_ride);

            //On clicking on user details view show: Start/ End ride button with animation
            mBottomView.findViewById(R.id.bottom_layout_user_panel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(mTripObject.getStatus() == StatusEnum.started_to_drop_off_customer) {

                        if(mEndRideAnimationView.getVisibility() == View.GONE) {
                            showButtons(false, false);
                        } else {
                            showButtons(false, true);
                        }
                    } else if(mTripObject.getStatus() == StatusEnum.started_to_pick_up_customer) {
                        if(mStartRideAnimationView.getVisibility() == View.GONE) {
                            showButtons(true, false);
                        } else {
                            showButtons(true, true);
                        }
                    }
                }
            });

            mBottomView.findViewById(R.id.bottom_layout_user_panel).setVisibility(View.VISIBLE);
        }
    }

    //Set up the Call to Action button provided by HyperTrack bottom view
    private void setCallToActionButtonInBottomView(Boolean isTripDone) {
        mBaseView.showCTAButton();
        mBaseView.setCTAButtonArrowVisibility(false);

        if(isTripDone) {
            //View when trip is completed --> FIND NEW RIDES
            mBaseView.setCTATitleIconResource(R.drawable.directions_button);
            mBaseView.setCTAButtonTitle(getString(R.string.find_new_rides));
            mBaseView.setCTAButtonClickListener(new CTAButton.OnClickListener() {
                @Override
                public void onTitleButtonClick() {
                    launchActivity(FindRideActivity.class);
                }

                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                }
            });

        } else {
            //Initial view when trip is going on --> GET DIRECTIONS
            mBaseView.setCTATitleIconResource(R.drawable.directions_button);
            mBaseView.setCTAButtonTitle(getString(R.string.get_directions));
            mBaseView.setCTAButtonClickListener(new CTAButton.OnClickListener() {
                @Override
                public void onTitleButtonClick() {
                    String uri = "http://maps.google.com/maps?"
                            + "saddr=" + mTripObject.getPickup().getCoordinate().getLatitude() + "," + mTripObject.getPickup().getCoordinate().getLongitude()
                            + "&daddr=" + mTripObject.getDrop().getCoordinate().getLatitude() + "," + mTripObject.getDrop().getCoordinate().getLongitude();

                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }

                @Override
                public void onLeftButtonClick() {
                }

                @Override
                public void onRightButtonClick() {
                }
            });
        }
    }

    private void actionStatusChanged(List<Action> changedStatusActions) {
        if(changedStatusActions != null) {
            for (Action action : changedStatusActions) {
                Log.d(Constants.TAG, "actionStatusChanged: action id:  " + action.getId() +"   " +action.getStatus());

                if (action != null && (action.isArriving() || action.isArrived()) && action.getId() != null) { // action.isArrived()
                    if (action.getId().equalsIgnoreCase(mTripObject.getHypertrack().getPickupActionId())) {
                        showButtons(true, false);
                        Log.d(Constants.TAG, "DRIVER HAS ARRIVED");

                    } else if (action.getId().equalsIgnoreCase(mTripObject.getHypertrack().getDropActionId())) {
                        showButtons(false, false);
                        Log.d(Constants.TAG, "CUSTOMER DROP IS GETTING DONE");
                    }
                } else {
                    Log.e(Constants.TAG, "Error while Action status changed");
                }
            }
        }
    }

    private void getRide() {
        Utils.showProgressDialog(mActivity, false);

        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        Log.d(Constants.TAG,"StartRide:  getRide: rideId:  " +rideId);

        if (!TextUtils.isEmpty(rideId)) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);
            ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();

                    mTripObject = dataSnapshot.getValue(Trip.class);
                    setUpHyperTrackBottomView();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Error finding Trip to Start : " + databaseError.getMessage());
                    Utils.hideProgressDialog();
                }
            });
        } else {
            showSnackBar(getString(R.string.no_ride_found));
            SharedValues.resetTripValues(mContext);
            launchActivity(FindRideActivity.class);
        }
    }

    private void showActionStatus() {
        Log.e(Constants.TAG, "showActionStatus : mTripObject  " + mTripObject.getStatus());

        if (mTripObject != null) {

            StatusEnum status = mTripObject.getStatus();
            switch (status) {
                case trip_completed:
                    showTripSummaryView(mTripObject.getHypertrack().getDropActionId());
                    break;

                case started_to_drop_off_customer:
                    if (mTripObject.getHypertrack() != null &&
                            !TextUtils.isEmpty(mTripObject.getHypertrack().getDropActionId())) {
                        trackAction(mTripObject.getHypertrack().getDropUniqueId());
                    }
                    break;

                case customer_pickup_completed:
                    if(mTripObject.getHypertrack() != null) {
                        if (TextUtils.isEmpty(mTripObject.getHypertrack().getDropActionId())) {
                            createDropAction();
                        } else {
                            trackAction(mTripObject.getHypertrack().getDropUniqueId());
                        }
                    }
                    break;

                case started_to_pick_up_customer:
                case trip_assigned:
                    if (mTripObject.getHypertrack() == null || TextUtils.isEmpty(mTripObject.getHypertrack().getPickupActionId())) {
                        //As no pickup is done, the hypertrack json will be empty. So check this: mTripObject.getHypertrack() == null
                        createPickUpAction();

                    } else {
                        trackAction(mTripObject.getHypertrack().getPickupUniqueId());
                    }
                    break;

                case trip_not_started:
                    showSnackBar("Trip is not yet assigned");
                    break;
                }

        } else {
            showSnackBar("showActionStatus: No ride available");
        }

    }

    private void createPickUpAction() {

        Utils.showProgressDialog(mActivity, false);

        final String uniqueId = "pick_up_" + mTripObject.getId();

        Log.d(Constants.TAG,"createPickUpAction::  uniqueId:  " +uniqueId);

        Place place = new Place();
        place.setLocation(mTripObject.getPickup().getCoordinate().getLatitude(), mTripObject.getPickup().getCoordinate().getLongitude());

        /*AutoCompleteRule autoCompleteRule = new AutoCompleteRule();
        autoCompleteRule.setRadius(100);
        autoCompleteRule.setType("geofence");

        JsonObject jsonObject = new Gson().toJsonTree(autoCompleteRule).getAsJsonObject();*/

        ActionParams actionParams = new ActionParamsBuilder()
                .setType(com.hypertrack.lib.models.Action.TYPE_PICKUP)
                .setUniqueId(uniqueId)
                .setExpectedPlace(place)
                .setUserId(HyperTrack.getUserId())
                .setCollectionId(mTripObject.getId())
                .build();

        //Replace createMockAction --> createAction for actually tracking
        //Mock action provides way to test your tracking implementation
        HyperTrack.createMockAction(actionParams, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Log.d(Constants.TAG, "createPickAction: onSuccess: " + response.getResponseObject());
                Utils.hideProgressDialog();

                if(response != null && response.getResponseObject() instanceof Action) {
                    if (mBtStartRide != null) {
                        mBtStartRide.setVisibility(View.VISIBLE);
                    }

                    final com.hypertrack.lib.models.Action action = (com.hypertrack.lib.models.Action) response.getResponseObject();
                    String actionId = action.getId();
                    String trackingUrl = action.getTrackingURL();

                    Log.d(Constants.TAG, "createHyperTrackAction:: actionId:  " + actionId + "   trackingUrl:  " + trackingUrl);

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_TRIPS);

                    HypertrackDetails hypertrackDetails = mTripObject.getHypertrack();
                    if(hypertrackDetails == null) {
                        hypertrackDetails = new HypertrackDetails();
                    }
                    hypertrackDetails.setPickupActionId(actionId);
                    hypertrackDetails.setPickupUniqueId(uniqueId);
                    hypertrackDetails.setPickupTrackingUrl(trackingUrl);
                    hypertrackDetails.setCollectionId(mTripObject.getId());
                    mTripObject.setHypertrack(hypertrackDetails);
                    mTripObject.setStatus(StatusEnum.started_to_pick_up_customer);

                    ridesRef.child(mTripObject.getId()).setValue(mTripObject, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            trackAction(action.getUniqueId());
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

                Utils.hideProgressDialog();
                Log.e(Constants.TAG, "createPickUpAction: onError: " + errorResponse.getErrorCode() + "   " + errorResponse.getErrorMessage());
                //showSnackBar("Error creating Pick action: " + errorResponse.getErrorMessage());

                //Action with this unique_id already exists
                if(errorResponse.getErrorCode() == 400) {
                    trackAction(uniqueId);
                }
            }
        });

    }

    private void createDropAction() {

        Utils.showProgressDialog(mActivity, false);
        final String uniqueId = "drop_off_" + mTripObject.getId();

        Log.d(Constants.TAG,"createDropAction::  uniqueId:  " +uniqueId);
        Place place = new Place();
        GeoJSONLocation geoJSONLocation = new GeoJSONLocation(mTripObject.getDrop().getCoordinate().getLatitude(), mTripObject.getDrop().getCoordinate().getLongitude());
        place.setLocation(geoJSONLocation);

        ActionParams actionParams = new ActionParamsBuilder()
                .setType(Action.TYPE_DROPOFF)
                .setUniqueId(uniqueId)
                .setUserId(HyperTrack.getUserId())
                .setExpectedPlace(place)
                .setCollectionId(mTripObject.getHypertrack().getCollectionId())
                .build();

        //Replace createMockAction --> createAction for actually tracking
        //Mock action provides way to test your tracking implementation
        HyperTrack.createMockAction(actionParams, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Log.d(Constants.TAG, "createDropAction: onSuccess: " + response.getResponseObject());

                Utils.hideProgressDialog();

                if(response != null && response.getResponseObject() instanceof Action) {

                    final com.hypertrack.lib.models.Action action = (com.hypertrack.lib.models.Action) response.getResponseObject();
                    String actionId = action.getId();
                    String trackingUrl = action.getTrackingURL();

                    Log.d(Constants.TAG, "createDropAction:: actionId:  " + actionId + "   trackingUrl:  " + trackingUrl);

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_TRIPS);

                    mTripObject.getHypertrack().setDropActionId(actionId);
                    mTripObject.getHypertrack().setDropUniqueId(uniqueId);
                    mTripObject.getHypertrack().setDropTrackingUrl(trackingUrl);
                    mTripObject.setStatus(StatusEnum.started_to_drop_off_customer);

                    ridesRef.child(mTripObject.getId()).setValue(mTripObject, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                            trackAction(action.getUniqueId());
                        }
                    });
                }
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

                Utils.hideProgressDialog();
                Log.e(Constants.TAG, "createDropAction: onError: " + errorResponse.getErrorCode() + "   " + errorResponse.getErrorMessage());

                //showSnackBar("Error creating Drop action: " + errorResponse.getErrorMessage());

                //Action with this unique_id already exists
                if(errorResponse.getErrorCode() == 400) {
                    trackAction(uniqueId);
                }
            }
        });

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
                Log.e(Constants.TAG, "createDropAction: onError: uniqueId:  " + uniqueId + "   " + errorResponse.getErrorCode() + "   " + errorResponse.getErrorMessage());
            }
        });
    }

    private void markActionComplete(final String actionId, final String uniqueId, final boolean isPickUp) {
        Utils.showProgressDialog(mActivity, false);

        Log.d(Constants.TAG, "markActionComplete:: uniqueId:  " + uniqueId);

        HyperTrack.completeActionWithUniqueIdInSync(uniqueId, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Utils.hideProgressDialog();

                List<String> actionIds = new ArrayList<>(1);
                actionIds.add(actionId);
                HyperTrack.removeActions(actionIds);

                if (isPickUp) {
                    mTripObject.setStatus(StatusEnum.customer_pickup_completed);
                    updateFireBase();

                    TextView addressTitle = mBottomView.findViewById(R.id.tv_address_title);
                    addressTitle.setText("DROP OFF");
                    addressTitle.setVisibility(View.VISIBLE);

                    TextView addressDetails = mBottomView.findViewById(R.id.tv_address_details);
                    addressDetails.setText(mTripObject.getDrop().getDisplayAddress());
                    addressDetails.setVisibility(View.VISIBLE);

                    showButtons(true, true);
                    createDropAction();

                } else {
                    SharedValues.resetTripValues(mContext);
                    mTripObject.setStatus(StatusEnum.trip_completed);
                    updateFireBase();

                    if(mBtEndRide != null) {
                        mBtEndRide.setVisibility(View.GONE);
                    }

                    showTripSummaryView(mTripObject.getHypertrack().getDropActionId());
                }
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {

                Utils.hideProgressDialog();
                Log.e(Constants.TAG, "markActionComplete: isPick: " + isPickUp+"  onError: " + errorResponse.getErrorCode() + "   " + errorResponse.getErrorMessage());
                showSnackBar("Error completeing action: " + errorResponse.getErrorMessage());
            }
        });
    }

    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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

        if (!TextUtils.isEmpty(mTripObject.getUser().getPhone())) {

            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE_PERMISSION);
                return;

            } else {

                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mTripObject.getUser().getPhone()));
                startActivity(intent);
            }
        }
    }

    //This view is summary of the Trip.
    //This is custom view as per 3rd party developer wants
    //comment the updateActionSummary in DetailsPanelview (your view) so we dont see HyperTrack's summary view
    private void showTripSummaryView(String actionId) {
        Utils.showProgressDialog(mActivity, false);

        myHyperTrackMapAdapter.setShowActionRoute(true);

        HyperTrack.getAction(actionId, new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Utils.hideProgressDialog();

                final com.hypertrack.lib.models.Action action = (com.hypertrack.lib.models.Action) response.getResponseObject();

                if(action != null) {
                    mBaseView.removeBottomView();

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

    private void showButtons(boolean isPickUp, boolean collapse) {
        if(isPickUp) {
            if(collapse) {
                AnimationUtils.collapse(mStartRideAnimationView);
            } else {
                AnimationUtils.expand(mStartRideAnimationView);
            }
        } else {
            if(collapse) {
                AnimationUtils.collapse(mEndRideAnimationView);
            } else {
                AnimationUtils.expand(mEndRideAnimationView);
            }
        }
    }
}
