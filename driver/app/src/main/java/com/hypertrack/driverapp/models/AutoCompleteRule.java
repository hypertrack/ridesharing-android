package com.hypertrack.driverapp.models;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * Created by pkharche on 11/04/18.
 */
public class AutoCompleteRule implements Serializable{

    @SerializedName("type")
    String type;
    @SerializedName("radius")
    int radius;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }
}
