package com.hypertrack.driverapp.models;

import com.google.firebase.database.PropertyName;

/**
 * Created by pkharche on 16/04/18.
 */
public class Address {
    Coordinate coordinate;

    String displayAddress;

    public Coordinate getCoordinate() {
        return coordinate;
    }

    @PropertyName("display_address")
    public String getDisplayAddress() {
        return displayAddress;
    }

    @PropertyName("display_address")
    public void setDisplayAddress(String displayAddress) {
        this.displayAddress = displayAddress;
    }
}
