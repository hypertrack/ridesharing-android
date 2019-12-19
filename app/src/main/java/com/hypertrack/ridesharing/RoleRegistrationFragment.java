package com.hypertrack.ridesharing;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.hypertrack.ridesharing.models.User;

public class RoleRegistrationFragment extends Fragment implements View.OnClickListener {

    private User user;

    public static Fragment newInstance(@NonNull User user) {
        Fragment fragment = new RoleRegistrationFragment();
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
        return inflater.inflate(R.layout.fragment_role_registration, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.driver_role).setOnClickListener(this);
        view.findViewById(R.id.rider_role).setOnClickListener(this);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View view) {
        user.role = view.getId() == R.id.driver_role ? User.USER_ROLE_DRIVER : User.USER_ROLE_RIDER;
        ((RegistrationActivity)getActivity()).next(user);
    }
}
