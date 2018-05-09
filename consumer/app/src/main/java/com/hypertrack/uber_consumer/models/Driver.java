package com.hypertrack.uber_consumer.models;

import com.google.firebase.database.PropertyName;

/**
 * Created by pkharche on 12/04/18.
 */
public class Driver extends User {
    String vehicle;
    String vehicleNo;

    public String getVehicle() {
        return vehicle;
    }

    @PropertyName("vehicle_no")
    public String getVehicleNo() {
        return vehicleNo;
    }

    @PropertyName("vehicle_no")
    public void setVehicleNo(String vehicleNo) {
        this.vehicleNo = vehicleNo;
    }
}
