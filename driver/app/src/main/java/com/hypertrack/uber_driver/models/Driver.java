package com.hypertrack.uber_driver.models;

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

    public void setVehicle(String vehicle) {
        this.vehicle = vehicle;
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
