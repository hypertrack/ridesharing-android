package com.hypertrack.uber_consumer.activities;

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
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.models.User;
import com.hypertrack.lib.models.UserParams;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.utils.Constants;
import com.hypertrack.uber_consumer.utils.SharedValues;
import com.hypertrack.uber_consumer.utils.Utils;

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

    @BindView(R.id.bt_submit)
    TextView mBtSubmit;

    private FirebaseUser mFirebaseUser = null;
    private com.hypertrack.uber_consumer.models.User mUser = null;
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
    }

    private void validateInputs() {
        if(mEtNameView.getText() != null && mEtNameView.getText().toString().trim().length() > 0 &&
                mEtPhoneView.getText() != null && mEtPhoneView.getText().toString().trim().length() > 0) {

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
            showSnackBar(getString(R.string.fetching_profile_details), false);
            Utils.showProgressDialog(mActivity, false);

            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference(Constants.FIREBASE_USERS);
            Query query = userRef.orderByChild(Constants.FIREBASE_USER_PHONE)
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
                            mUser = childDataSnapshot.getValue(com.hypertrack.uber_consumer.models.User.class);
                        }

                        if (mUser != null) {
                            isNewUser = false;
                            preFillData();
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Utils.hideProgressDialog();
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Firebase Error finding User details: " + databaseError.getMessage());
                }
            });
        } else {
            showSnackBar("No firebase user available ");
        }
    }

    private void preFillData() {
        showSnackBar(getString(R.string.update_profile_details), false);

        //Display data we got from Firebase. This data was earlier entered by this user itself
        if(!TextUtils.isEmpty(mUser.getName())) {
            mEtNameView.setText(mUser.getName());
            mEtNameView.setSelection(mUser.getName().length());
        }
        mEtPhoneView.setText(mFirebaseUser.getPhoneNumber());
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.placeholder(R.drawable.profile_avatar);
        requestOptions.error(R.drawable.profile_avatar);

        boolean isFinishing = isFinishing();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            isFinishing = isFinishing() || isDestroyed();
        }

        if (!isFinishing) {
            Glide.with(this)
                    .load(mUser.getImageUrl())
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
                    Utils.hideProgressDialog();

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
        DatabaseReference driverRef = database.getReference(Constants.FIREBASE_USERS);

        com.hypertrack.uber_consumer.models.User user = new com.hypertrack.uber_consumer.models.User();
        user.setId(hyperTrackUser.getId()); //this should be firbase id, so that next time you can fetch driver with this
        user.setName(hyperTrackUser.getName());
        String phoneNumber = hyperTrackUser.getPhone().replace(" ", "");
        user.setPhone(phoneNumber);
        user.setImageUrl(hyperTrackUser.getPhoto());
        user.setRating(3.5f); //hard coded

        SharedValues.saveValue(GetHyperTrackUserActivity.this, Constants.USER_ID, user.getId());
        driverRef.child(user.getId()).setValue(user, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                Utils.hideProgressDialog();
                if (databaseError != null) {
                    Log.d(Constants.TAG,"User could not be saved on Firebase" + databaseError.getMessage());
                    logOutFirebaseUser();

                } else {
                    Log.d(Constants.TAG,"User saved successfully on Firebase");
                    launchActivity(GenerateRideActivity.class);
                }
            }
        });
    }
}

