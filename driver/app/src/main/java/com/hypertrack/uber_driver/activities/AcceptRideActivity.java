package com.hypertrack.uber_driver.activities;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hypertrack.uber_driver.R;
import com.hypertrack.uber_driver.models.Driver;
import com.hypertrack.uber_driver.models.StatusEnum;
import com.hypertrack.uber_driver.models.Trip;
import com.hypertrack.uber_driver.utils.Constants;
import com.hypertrack.uber_driver.utils.SharedValues;
import com.hypertrack.uber_driver.utils.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by pkharche on 08/04/18.
 */
public class AcceptRideActivity extends BaseActivity {

    private final int VIEW_TIME_OUT = 5000;
    private Driver mDriver = null;

    @BindView(R.id.tv_address_title)
    TextView mTvAddressTitle;

    @BindView(R.id.tv_address_details)
    TextView mTvAddressDetails;

    @BindView(R.id.ll_user_panel)
    View mUserDetailsPanel;

    @BindView(R.id.tv_cta)
    View mCtaButton;

    @BindView(R.id.tv_user_name)
    TextView mTvUserName;

    @BindView(R.id.tv_user_rating)
    TextView mTvUserRating;

    @BindView(R.id.tv_user_rating_bar)
    RatingBar mRatingBar;

    @BindView(R.id.iv_user_image)
    ImageView mIvUserProfileImage;

    @BindView(R.id.iv_cirlce_map)
    ImageView mIvCircularMap;

    @BindView(R.id.bt_cancel)
    View mBtCancel;

    private Trip mTripObject = null;

    @OnClick(R.id.bt_cancel)
    public void cancelClicked(View v) {
        findNewTrip(null);
    }

    @OnClick(R.id.bt_accept_ride)
    public void acceptRideClicked(View v) {
        updateFirebase();
    }

    /**
     * Trip accepted by Driver is updated to Firebase
     */
    private void updateFirebase() {
        searchDriver();
    }

    private void searchDriver() {
        Utils.showProgressDialog(mActivity, false);

        String driverId = SharedValues.getValue(mContext, Constants.DRIVER_ID);
        Log.d(Constants.TAG,"AcceptRide:  driverId " +driverId);

        if(!TextUtils.isEmpty(driverId)) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_DRIVERS);
            Query query = ridesRef.child(driverId);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();
                    mDriver = dataSnapshot.getValue(Driver.class);
                    if(mDriver == null) {
                        logOutFirebaseUser();
                    } else {
                        updateFirebaseTrip();
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
        }
    }

    /**
     * Update Driver in firebase
     */
    private void updateFirebaseDriver() {
        Utils.showProgressDialog(mActivity, false);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference driverRef = database.getReference(Constants.FIREBASE_DRIVERS);

        driverRef.child(mDriver.getId()).setValue(mDriver, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Utils.hideProgressDialog();

                if (databaseError != null) {
                    Log.d(Constants.TAG, "Driver could not be updated on Firebase" + databaseError.getMessage());

                } else {
                    Log.d(Constants.TAG, "Driver successfully updated on Firebase");
                    SharedValues.saveValue(mContext, Constants.TRIP_IS_ACCEPTED, "1");
                    launchActivity(StartRideActivity.class);
                }
            }
        });
    }

    /**
     * Update Trip in Firebase
     */
    private void updateFirebaseTrip() {
        Utils.showProgressDialog(mActivity, false);
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        //These values are in Trip for reference
        mDriver.setOnRide(true);
        mDriver.setRideId(mTripObject.getId());

        mTripObject.setDriver(mDriver);
        mTripObject.setStatus(StatusEnum.trip_assigned);

        DatabaseReference tripRef = database.getReference(Constants.FIREBASE_TRIPS);
        tripRef.child(mTripObject.getId()).setValue(mTripObject, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Utils.hideProgressDialog();

                if (databaseError != null) {
                    Log.d(Constants.TAG,"Trip Data could not be updated on Firebase" + databaseError.getMessage());

                } else {
                    Log.d(Constants.TAG,"Trip data successfully updated on Firebase");
                    updateFirebaseDriver();
                }
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accept_ride);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        ButterKnife.bind(this);

        searchRide();
    }

    private void searchRide() {
        Utils.showProgressDialog(mActivity, false);

        final String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        Log.d(Constants.TAG,"AcceptRide:  rideId " +rideId);

        if(!TextUtils.isEmpty(rideId)) {

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_TRIPS);
            Query query = ridesRef.child(rideId);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();
                    if(dataSnapshot.exists()) {
                        mTripObject = dataSnapshot.getValue(com.hypertrack.uber_driver.models.Trip.class);
                        if (mTripObject != null) { //check null, in case trip is deleted from Firebase
                            mTripObject.setId(dataSnapshot.getKey());

                            mCoordinatorLayout.setVisibility(View.VISIBLE);
                            fillUserData();
                        }
                    } else {
                        Log.d(Constants.TAG,"AcceptRide:  rideId " +rideId+"  is not available");
                        findNewTrip(null);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Firebase Error finding Trip details: " + databaseError.getMessage());
                    Utils.hideProgressDialog();
                }
            });
        } else {
            findNewTrip("No trip available currently...try after sometime");
        }
    }

    private void fillUserData() {

        //Display CANCEL button after 5secs
        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mBtCancel.setVisibility(View.VISIBLE);
            }
        }, VIEW_TIME_OUT);*/


        if (mTripObject != null) {
            //Display user data
            if (mTripObject.getUser() != null) {
                mTvUserName.setText(mTripObject.getUser().getName());
                mTvUserRating.setText(String.valueOf(mTripObject.getUser().getRating()));
                mRatingBar.setRating(mTripObject.getUser().getRating());

                RequestOptions requestOptions = new RequestOptions();
                requestOptions.placeholder(R.drawable.profile_avatar);
                requestOptions.error(R.drawable.profile_avatar);

                boolean isFinishing = isFinishing();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    isFinishing = isFinishing() || isDestroyed();
                }

                if (!isFinishing) {
                    Glide.with(this)
                            .load(mTripObject.getUser().getImageUrl())
                            .apply(requestOptions)
                            .into(mIvUserProfileImage);
                }

                mCtaButton.setVisibility(View.GONE);
                mUserDetailsPanel.setVisibility(View.VISIBLE);

                findViewById(R.id.bottom_layout_user_panel).setVisibility(View.VISIBLE);
                findViewById(R.id.ll_user_panel).setVisibility(View.VISIBLE);
            }

            //Display Map data using Static maps
            if (mTripObject.getPickup() != null) {
                if (!TextUtils.isEmpty(mTripObject.getPickup().getDisplayAddress())) {
                    mTvAddressDetails.setText(mTripObject.getPickup().getDisplayAddress());
                    mTvAddressDetails.setVisibility(View.VISIBLE);

                    mTvAddressTitle.setText("PICKUP");
                    mTvAddressTitle.setVisibility(View.VISIBLE);
                }

                String url = "https://maps.googleapis.com/maps/api/staticmap?"
                        + "center=" + mTripObject.getPickup().getCoordinate().getLatitude() + "," + mTripObject.getPickup().getCoordinate().getLongitude()
                        + "&markers=color:0x000000"
                        + "%7C" + mTripObject.getPickup().getCoordinate().getLatitude() + "," + mTripObject.getPickup().getCoordinate().getLongitude()
                        + "&zoom=13"
                        + "&scale=2"
                        + "&size=350x350"
                        + "&path=weight:3%7"
                        + "&key=" + getString(R.string.google_maps_key);

                RequestOptions requestOptions1 = new RequestOptions();
                requestOptions1.placeholder(R.drawable.map_loadingbackground);
                requestOptions1.error(R.drawable.map_loadingbackground);

                boolean isFinishing = isFinishing();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    isFinishing = isFinishing() || isDestroyed();
                }

                if (!isFinishing) {
                    Glide.with(this)
                            .load(Uri.parse(Uri.decode(url)))
                            .apply(requestOptions1)
                            .into(mIvCircularMap);
                }
            }
        }
    }

}
