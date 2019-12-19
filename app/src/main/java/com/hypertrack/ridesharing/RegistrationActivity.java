package com.hypertrack.ridesharing;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.gson.Gson;
import com.hypertrack.sdk.HyperTrack;
import com.hypertrack.ridesharing.components.MainActivity;
import com.hypertrack.ridesharing.models.User;
import com.hypertrack.ridesharing.utils.HyperTrackUtils;

import java.util.HashMap;
import java.util.Map;

public class RegistrationActivity extends AppCompatActivity {
    private static final String TAG = "RegistrationActivity";

    private Gson gson = new Gson();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        String json = MySharedPreferences.get(this).getString(MySharedPreferences.USER_KEY, "{}");
        User user = gson.fromJson(json, User.class);
        next(user);
    }

    public void next(final User user) {
        if (TextUtils.isEmpty(user.role)) {
            if (getPackageName().contains(User.USER_ROLE_DRIVER)) {
                user.role = User.USER_ROLE_DRIVER;
                next(user);
            } else if (getPackageName().contains(User.USER_ROLE_RIDER)) {
                user.role = User.USER_ROLE_RIDER;
                next(user);
            } else {
                addFragment(RoleRegistrationFragment.newInstance(user));
            }
        } else if (TextUtils.isEmpty(user.name)) {
            addFragment(NameRegistrationFragment.newInstance(user));
        } else if (TextUtils.isEmpty(user.id)) {
            if (User.USER_ROLE_DRIVER.equals(user.role)) {
                HyperTrack hyperTrack = HyperTrack.getInstance(this, HyperTrackUtils.getPubKey(this));
                hyperTrack.setDeviceName(user.name);
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("name", user.name);
                metadata.put("phone_number", user.phoneNumber);
                Map<String, Object> car = new HashMap<>();
                car.put("model", user.car.model);
                car.put("license_plate", user.car.licensePlate);
                metadata.put("car", car);
                hyperTrack.setDeviceMetadata(metadata);

                user.deviceId = hyperTrack.getDeviceID();
            }
            FirebaseFirestoreApi.createUser(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                            user.id = documentReference.getId();
                            next(user);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "Error adding document", e);
                        }
                    });
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(user);
                MySharedPreferences.get(this).edit()
                        .putString(MySharedPreferences.USER_KEY, json)
                        .apply();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }

            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void addFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_frame, fragment);
        transaction.commitAllowingStateLoss();
    }
}
