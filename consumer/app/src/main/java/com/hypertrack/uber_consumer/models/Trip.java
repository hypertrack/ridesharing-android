package com.hypertrack.uber_consumer.models;

import com.google.firebase.database.PropertyName;

/**
 * Created by pkharche on 10/04/18.
 */
public class Trip {

    String id; //firebase id/ key

    Address drop;
    Address pickup;

    HypertrackDetails hypertrack;

    StatusEnum status;

    User user;
    Driver driver;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @PropertyName("drop")
    public Address getDrop() {
        return drop;
    }

    @PropertyName("drop")
    public void setDrop(Address drop) {
        this.drop = drop;
    }

    @PropertyName("pickup")
    public Address getPickup() {
        return pickup;
    }

    @PropertyName("pickup")
    public void setPickup(Address pickup) {
        this.pickup = pickup;
    }

    @PropertyName("hypertrack")
    public HypertrackDetails getHypertrack() {
        return hypertrack;
    }

    @PropertyName("hypertrack")
    public void setHypertrack(HypertrackDetails hypertrack) {
        this.hypertrack = hypertrack;
    }

    @PropertyName("status")
    public StatusEnum getStatus() {
        return status;
    }

    @PropertyName("status")
    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    @PropertyName("user_info")
    public User getUser() {
        return user;
    }

    @PropertyName("user_info")
    public void setUser(User user) {
        this.user = user;
    }

    @PropertyName("driver")
    public Driver getDriver() {
        return driver;
    }

    @PropertyName("driver")
    public void setDriver(Driver driver) {
        this.driver = driver;
    }
}
