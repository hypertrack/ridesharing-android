package com.hypertrack.uber_consumer.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.FirebaseApp;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.utils.Utils;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by pkharche on 06/04/18.
 */
public class LoginIntoFirebaseActivity extends BaseActivity {

    private static final int RC_SIGN_IN = 123;

    @BindView(R.id.bt_login)
    View mBtLogin;

    @OnClick(R.id.bt_login)
    public void loginClicked(View v) {

        // HyperTrack: Check for Location permission
        if (!HyperTrack.checkLocationPermission(mContext)) {
            HyperTrack.requestPermissions(mActivity);
            return;
        }

        // Location Permissions and Settings have been enabled
        // Proceed with your app logic here i.e User Login in this case
        attemptUserLoginViaFirebase();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Utils.hideProgressDialog();
    }

    private void attemptUserLoginViaFirebase() {
        if (isInternetConnected()) {

            Utils.showProgressDialog(mActivity, false);
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.PhoneBuilder().build());

            // Create and launch sign-in intent
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .build(),
                    RC_SIGN_IN);

        } else {
            showNetworkError();
        }
    }

    /**
     * HyperTrack
     * Handle on Grant Location Permissions request accepted/denied result
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == HyperTrack.REQUEST_CODE_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Check if Location Settings are enabled to proceed
                attemptUserLoginViaFirebase();

            } else {
                // Handle Location Permission denied error
                showPermissionDialog();
                //showSnackBar(getString(R.string.location_permission_denied_error));
            }
        }
    }

    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Location permission denied");
        builder.setMessage(getString(R.string.location_permission_denied_error));
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                HyperTrack.requestPermissions(mActivity);
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    /**
     * Handle on Enable Location Services request accepted/denied result
     *
     * @param requestCode
     * @param resultCode
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    // Successfully signed in Firebase, now lets create HyperTrack User
                    Intent i = new Intent(mContext, GetHyperTrackUserActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(i);
                    finish();


                } else {
                    showSnackBar("Firebase authentication failed");
                }
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);

        ButterKnife.bind(this);

        //Firebase Phone number authentication
        FirebaseApp.initializeApp(mContext);
    }
}
