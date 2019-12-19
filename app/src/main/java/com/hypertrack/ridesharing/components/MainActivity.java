package com.hypertrack.ridesharing.components;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypertrack.ridesharing.MySharedPreferences;
import com.hypertrack.ridesharing.PermissionsFragment;
import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.components.driver.DriverMapFragment;
import com.hypertrack.ridesharing.components.rider.RiderMapFragment;
import com.hypertrack.ridesharing.models.User;

public class MainActivity extends FragmentActivity {

    public static final int REQUEST_ACCESS_FINE_LOCATION = 10;

    public static final String GOOGLE_API_KEY = "AIzaSyBKZejrZNZpLlemrH28Nc46XzHsRSVRxKI";

    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ObjectMapper mapper = new ObjectMapper();
        String json = MySharedPreferences.get(this).getString(MySharedPreferences.USER_KEY, "");
        try {
            user = mapper.readValue(json, User.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (user == null) return;

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            startMap();
        } else {
            startPermissionsRequest();
        }
    }

    private void startMap() {
        Fragment fragment = user.role.equals("driver") ?
                DriverMapFragment.newInstance() : RiderMapFragment.newInstance();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commit();
    }

    private void startPermissionsRequest() {
        Fragment fragment = new PermissionsFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {

            if ((permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    || User.USER_ROLE_RIDER.equals(user.role)) {
                startMap();
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.parse("package:" + getPackageName());
                                intent.setData(uri);
                                startActivityForResult(intent, REQUEST_ACCESS_FINE_LOCATION);
                            }
                        })
                        .setTitle(R.string.app_settings)
                        .setMessage(R.string.you_can_allow)
                        .create();
                alertDialog.show();
            }
        }
    }
}
