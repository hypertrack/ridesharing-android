package com.hypertrack.ridesharing;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hypertrack.ridesharing.models.Car;
import com.hypertrack.ridesharing.models.User;

public class NameRegistrationFragment extends Fragment {

    private EditText usernameEditText;
    private EditText phoneEditText;
    private EditText carModelEditText;
    private EditText driverLicenceEditText;

    private User user;

    public static Fragment newInstance(@NonNull User user) {
        Fragment fragment = new NameRegistrationFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(MySharedPreferences.USER_KEY, user);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            user = getArguments().getParcelable(MySharedPreferences.USER_KEY);
        }
        if (user == null) {
            user = new User();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_name_registration, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View driver = view.findViewById(R.id.driver);
        if (User.USER_ROLE_DRIVER.equals(user.role)) {
            driver.setVisibility(View.VISIBLE);
        }

        usernameEditText = view.findViewById(R.id.username);
        phoneEditText = view.findViewById(R.id.phone);
        carModelEditText = view.findViewById(R.id.car_model);
        driverLicenceEditText = view.findViewById(R.id.driver_licence);

        view.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {

            @SuppressWarnings("ConstantConditions")
            @Override
            public void onClick(View view) {
                user.name = usernameEditText.getText().toString();
                user.phoneNumber = phoneEditText.getText().toString();
                if (User.USER_ROLE_DRIVER.equals(user.role)) {
                    user.car = new Car();
                    user.car.model = carModelEditText.getText().toString();
                    user.car.licensePlate = driverLicenceEditText.getText().toString();
                }
                if (!TextUtils.isEmpty(user.name) && !TextUtils.isEmpty(user.phoneNumber)) {
                    ((RegistrationActivity) getActivity()).next(user);
                }
            }
        });
    }
}
