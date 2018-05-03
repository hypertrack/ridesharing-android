package com.hypertrack.driverapp.activities;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.hypertrack.driverapp.R;
import com.hypertrack.driverapp.models.Driver;
import com.hypertrack.driverapp.utils.Constants;
import com.hypertrack.driverapp.utils.SharedValues;
import com.hypertrack.driverapp.utils.Utils;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.models.User;
import com.hypertrack.lib.models.UserParams;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by pkharche on 16/04/18.
 */
public class GetHyperTrackUserActivity extends BaseActivity {

    @BindView(R.id.iv_user_image)
    ImageView mIvUserImageView;

    @BindView(R.id.et_name)
    EditText mEtNameView;

    @BindView(R.id.et_phone)
    EditText mEtPhoneView;

    @BindView(R.id.et_vehicle)
    EditText mEtVehicleView;

    @BindView(R.id.et_vehicle_no)
    EditText mEtVehicleNoView;

    @BindView(R.id.bt_submit)
    TextView mBtSubmit;

    private FirebaseUser mFirebaseUser = null;
    private Driver mDriver = null;
    private boolean isNewUser = false;

    @OnClick(R.id.bt_submit)
    public void onSubmitClicked(View v) {
        if(isInternetConnected()) {
            createHyperTrackUser();
        } else {
            showNetworkError();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_user);
        ButterKnife.bind(this);

        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        addTextWatchers();
        mEtPhoneView.setText(mFirebaseUser.getPhoneNumber());
        searchUser();
    }

    private void addTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               validateInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
        mEtNameView.addTextChangedListener(textWatcher);
        mEtPhoneView.addTextChangedListener(textWatcher);
        mEtVehicleView.addTextChangedListener(textWatcher);
        mEtVehicleNoView.addTextChangedListener(textWatcher);
    }

    private void validateInputs() {
        if(mEtNameView.getText() != null && mEtNameView.getText().toString().trim().length() > 0 &&
                mEtPhoneView.getText() != null && mEtPhoneView.getText().toString().trim().length() > 0 &&
                mEtVehicleView.getText() != null && mEtVehicleView.getText().toString().trim().length() > 0 &&
                mEtVehicleNoView.getText() != null && mEtVehicleNoView.getText().toString().trim().length() > 0 ) {

            showSubmitButton(true);

        } else {
            showSubmitButton(false);
        }
    }

    private void showSubmitButton(boolean show) {
        if (show) {
            mBtSubmit.setVisibility(View.VISIBLE);
        } else {
            mBtSubmit.setVisibility(View.GONE);
        }
    }

    private void searchUser() {
        Log.d(Constants.TAG, "GetUser:  userId " + mFirebaseUser.getUid()+"   mFirebaseUser.getPhoneNumber()  " +mFirebaseUser.getPhoneNumber());

        if (!TextUtils.isEmpty(mFirebaseUser.getUid())) {

            Utils.showProgressDialog(mActivity, false);
            showSnackBar(getString(R.string.fetching_profile_details), false);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference driverRef = database.getReference(Constants.FIREBASE_DRIVERS);
            Query query = driverRef.orderByChild(Constants.FIREBASE_DRIVER_PHONE)
                    .equalTo(mFirebaseUser.getPhoneNumber())
                    .limitToFirst(1);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();
                    if (dataSnapshot != null && dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                        //Found user with same number
                        //Log.d(Constants.TAG,"dataSnapshot  " +dataSnapshot);

                        for(DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {
                            //Log.d(Constants.TAG, "childDataSnapshot  " + childDataSnapshot);
                            mDriver = childDataSnapshot.getValue(Driver.class);
                        }

                        if (mDriver != null) {
                            isNewUser = false;
                            preFillData();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Firebase Error finding User details: " + databaseError.getMessage());
                    Utils.hideProgressDialog();
                }
            });
        } else {
            showSnackBar("No firebase user available ");
        }
    }

    private void preFillData() {
        showSnackBar(getString(R.string.update_profile_details), false);

        //Display data we got from Firebase. This data was earlier entered by this user itself
        if(!TextUtils.isEmpty(mDriver.getName())) {
            mEtNameView.setText(mDriver.getName());
            mEtNameView.setSelection(mDriver.getName().length());
        }

        mEtPhoneView.setText(mFirebaseUser.getPhoneNumber());
        mEtVehicleView.setText(mDriver.getVehicle());
        mEtVehicleNoView.setText(mDriver.getVehicleNo());
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.profile_avatar);
        requestOptions.error(R.drawable.profile_avatar);

        boolean isFinishing = isFinishing();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isFinishing = isFinishing() || isDestroyed();
        }

        if (!isFinishing) {
            Glide.with(this)
                    .load(mDriver.getImageUrl())
                    .apply(requestOptions)
                    .into(mIvUserImageView);
        }
    }

    private void createHyperTrackUser() {
        Utils.showProgressDialog(mActivity, false);

        final UserParams userParams = new UserParams();
        userParams.setUniqueId(mFirebaseUser.getUid()); //set Firebase Id while creating user/ fetching user
        userParams.setPhone(mEtPhoneView.getText().toString());
        userParams.setName(mEtNameView.getText().toString());
        //userParams.setPhoto();

        final HyperTrackCallback callback = new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse successResponse) {
                //Utils.hideProgressDialog();

                User hyperTrackUser = (User) successResponse.getResponseObject();
                updateFirebase(hyperTrackUser);
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                Utils.hideProgressDialog();
                Log.e(Constants.TAG, "Error::  " + errorResponse.getErrorMessage());
            }
        };

        if(isNewUser) {
            //Create NEW HyperTrack User
            HyperTrack.getOrCreateUser(userParams, callback);

        } else {

            //Fetch already created HyperTrack User
            HyperTrack.getOrCreateUser(userParams, new HyperTrackCallback() {
                @Override
                public void onSuccess(@NonNull SuccessResponse successResponse) {
                    //Utils.hideProgressDialog();

                    //Edit HyperTrack user details
                    HyperTrack.updateUser(userParams, callback);
                }

                @Override
                public void onError(@NonNull ErrorResponse errorResponse) {
                    Utils.hideProgressDialog();
                    Log.e(Constants.TAG, "Error::  " + errorResponse.getErrorMessage());
                }
            });
        }
    }

    private void updateFirebase(User hyperTrackUser) {
        Utils.showProgressDialog(mActivity, false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference driverRef = database.getReference(Constants.FIREBASE_DRIVERS);

        Driver driver = new Driver();
        driver.setId(hyperTrackUser.getId()); //key //this should be firbase id, so that next time you can fetch driver with this
        driver.setName(hyperTrackUser.getName());
        String phoneNumber = hyperTrackUser.getPhone().replace(" ", "");
        driver.setPhone(phoneNumber);
        driver.setImageUrl(hyperTrackUser.getPhoto());
        driver.setVehicle(mEtVehicleView.getText().toString());
        driver.setVehicleNo(mEtVehicleNoView.getText().toString());
        driver.setRating(4.0f); //hard coded

        SharedValues.saveValue(GetHyperTrackUserActivity.this, Constants.DRIVER_ID, driver.getId());
        driverRef.child(driver.getId()).setValue(driver, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                Utils.hideProgressDialog();
                if (databaseError != null) {
                    Log.d(Constants.TAG,"Driver could not be saved on Firebase" + databaseError.getMessage());
                    logOutFirebaseUser();

                } else {
                    Log.d(Constants.TAG,"Driver saved successfully on Firebase");
                    findNewTrip(null);
                }
            }
        });
    }
}

