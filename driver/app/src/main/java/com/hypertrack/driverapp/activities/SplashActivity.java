package com.hypertrack.driverapp.activities;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hypertrack.driverapp.R;
import com.hypertrack.driverapp.models.StatusEnum;
import com.hypertrack.driverapp.models.Trip;
import com.hypertrack.driverapp.utils.Constants;
import com.hypertrack.driverapp.utils.SharedValues;
import com.hypertrack.driverapp.utils.Utils;
import com.hypertrack.lib.HyperTrack;

/**
 * Created by pkharche on 06/04/18.
 */
public class SplashActivity extends BaseActivity {

    private final int SPLASH_TIME_OUT = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        launchActivity();
    }

    private void launchActivity() {
        //Log.d(Constants.TAG, "HyperTrack User Id:  " +HyperTrack.getUserId());

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser == null || TextUtils.isEmpty(firebaseUser.getUid())) {
            launchActivity(LoginIntoFirebaseActivity.class);

        } else if(TextUtils.isEmpty(HyperTrack.getUserId())) {
            launchActivity(GetHyperTrackUserActivity.class);

        } else {
            String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
            String isTripAccepted = SharedValues.getValue(mContext, Constants.TRIP_IS_ACCEPTED);

            Log.d(Constants.TAG, "Splash :: ride :  " +rideId+"  isTripAccepted:  " +isTripAccepted);


            //Check if NO on-going ride is present, show Find ride screen
            if (TextUtils.isEmpty(isTripAccepted) && TextUtils.isEmpty(rideId)) {

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        findNewTrip(null);
                        finish();
                    }
                }, SPLASH_TIME_OUT);

            } else if (!TextUtils.isEmpty(rideId) && !TextUtils.isEmpty(isTripAccepted)) {
                //If there is ON GOING ride is found, take driver to Ride screen
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        launchActivity(StartRideActivity.class);
                        finish();
                    }
                }, SPLASH_TIME_OUT);

            } else if (!TextUtils.isEmpty(rideId) && TextUtils.isEmpty(isTripAccepted)) {
                //A trip was found but driver didn't accept it yet. <onBackPressed clicked on Accept screen scnenario>
                //launchActivity(AcceptRideActivity.class);
                searchRide(rideId);

            } else {
                findNewTrip("Error finding any saved trip asssigned to driver.");
            }
        }
    }

    //Make query to check if this ride is not accepted by someone else
    private void searchRide(final String rideId) {
        Utils.showProgressDialog(mActivity, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);
        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Utils.hideProgressDialog();

                Trip trip = dataSnapshot.getValue(Trip.class);
                if(trip != null && trip.getStatus() == StatusEnum.trip_not_started) {
                    launchActivity(AcceptRideActivity.class);

                } else {
                    //Trip seems to be assigned to some other Driver, so look for new Trip
                    findNewTrip(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                findNewTrip("Error finding trip : " + rideId);
            }
        });
    }

}
