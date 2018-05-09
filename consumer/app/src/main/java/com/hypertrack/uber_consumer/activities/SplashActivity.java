package com.hypertrack.uber_consumer.activities;

import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.utils.Constants;
import com.hypertrack.uber_consumer.utils.SharedValues;

/**
 * Created by pkharche on 06/04/18.
 */
public class SplashActivity extends BaseActivity {

    private final int SPLASH_TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mCoordinatorLayout = findViewById(R.id.coordinator_layout);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                launchActivity();
                finish();
            }
        }, SPLASH_TIME_OUT);
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
            Log.d(Constants.TAG, "Splash :: customer ride :  " +rideId);


            String pickUpPlace = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
            String dropPlace = SharedValues.getValue(mContext, Constants.DROPOFF_PLACE);

            if (TextUtils.isEmpty(rideId) && !TextUtils.isEmpty(pickUpPlace) && !TextUtils.isEmpty(dropPlace)) {
                //A unbooked trip was found, so show book ride screen
                launchActivity(BookRideActivity.class);

            } else if (TextUtils.isEmpty(rideId)) {
                //Check if NO on-going ride is present, show Generate/Book ride screen
                launchActivity(GenerateRideActivity.class);

            } else {
                String isTripAccepted = SharedValues.getValue(mContext, Constants.TRIP_IS_ACCEPTED);
                if(!TextUtils.isEmpty(isTripAccepted)) {
                    //A trip was found show ride tracking screen
                    launchActivity(TrackRideActivity.class);

                } else {
                    //A trip was found but it was not yet assigned show ride tracking screen
                    launchActivity(BookRideActivity.class);
                }

            }
        }
    }

}
