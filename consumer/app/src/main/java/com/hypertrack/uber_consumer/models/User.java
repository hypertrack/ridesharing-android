package com.hypertrack.uber_consumer.models;

import com.google.firebase.database.PropertyName;

/**
 * Created by pkharche on 16/04/18.
 */
public class User {

    Coordinate coordinate;

    boolean isOnRide;
    String rideId;
    String id;
    String name;
    String phone;
    Float  rating;

    String imageUrl;

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    @PropertyName("is_on_ride")
    public boolean isOnRide() {
        return isOnRide;
    }

    @PropertyName("is_on_ride")
    public void setOnRide(boolean onRide) {
        isOnRide = onRide;
    }

    @PropertyName("ride_id")
    public String getRideId() {
        return rideId;
    }

    @PropertyName("ride_id")
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Float getRating() {
        return rating;
    }

    public void setRating(Float rating) {
        this.rating = rating;
    }

    @PropertyName("image_url")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("image_url")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
