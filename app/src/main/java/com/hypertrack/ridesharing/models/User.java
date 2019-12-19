package com.hypertrack.ridesharing.models;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.gms.maps.model.Marker;
import com.hypertrack.ridesharing.MyDeviceUpdatesHandler;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements Parcelable {
    public static final String USER_ROLE_DRIVER = "driver";
    public static final String USER_ROLE_RIDER = "rider";

    //    id: String
//    role: String (Enum driver | rider)
//    name: String
//    phone_number: String
//    device_id: String
//    car: Car
    public String id;
    public String role;
    public String name;
    @JsonProperty("phone_number")
    public String phoneNumber;
    @JsonProperty("device_id")
    public String deviceId;
    public Car car;

    @JsonIgnore
    public Location location;
    @JsonIgnore
    public Marker marker;

    @JsonIgnore
    public MyDeviceUpdatesHandler deviceUpdatesHandler;


    public User() {
    }

    public User(String id) {
        this.id = id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.role);
        dest.writeString(this.name);
        dest.writeString(this.phoneNumber);
        dest.writeString(this.deviceId);
    }

    protected User(Parcel in) {
        this.id = in.readString();
        this.role = in.readString();
        this.name = in.readString();
        this.phoneNumber = in.readString();
        this.deviceId = in.readString();
    }

    public static final Parcelable.Creator<User> CREATOR = new Parcelable.Creator<User>() {
        @Override
        public User createFromParcel(Parcel source) {
            return new User(source);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };
}
