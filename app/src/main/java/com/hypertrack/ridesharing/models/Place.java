package com.hypertrack.ridesharing.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.libraries.places.api.model.AddressComponent;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Place implements Parcelable {
    public double latitude;
    public double longitude;
    public String address;

    @JsonIgnore
    public String preview;
    @JsonIgnore
    public Marker marker;

    @JsonIgnore
    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.latitude);
        dest.writeDouble(this.longitude);
        dest.writeString(this.address);
    }

    public Place() {
    }

    public Place(LatLng latLng) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;
    }

    public Place(com.google.android.libraries.places.api.model.Place place) {
        if (place.getLatLng() != null) {
            this.latitude = place.getLatLng().latitude;
            this.longitude = place.getLatLng().longitude;
        }
        if (place.getAddressComponents() == null || place.getAddressComponents().asList().size() < 4) {
            this.address = place.getAddress();
        } else {
            List<AddressComponent> components = place.getAddressComponents().asList();
            this.address = components.get(0).getName() + " " + components.get(1).getShortName() + ", " + components.get(3).getShortName();
        }
    }

    protected Place(Parcel in) {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
        this.address = in.readString();
    }

    public static final Creator<Place> CREATOR = new Creator<Place>() {
        @Override
        public Place createFromParcel(Parcel source) {
            return new Place(source);
        }

        @Override
        public Place[] newArray(int size) {
            return new Place[size];
        }
    };
}
