package com.hypertrack.uber_consumer.models;

/**
 * Created by pkharche on 16/04/18.
 */
public enum StatusEnum {
    //Firebase naming issues so keeping enum values as small case

    trip_not_started("trip_not_started"),
    trip_assigned("trip_assigned"),
    started_to_pick_up_customer("started_to_pick_up_customer"),
    customer_pickup_completed("customer_pickup_completed"),
    started_to_drop_off_customer("started_to_drop_off_customer"),
    trip_completed("trip_completed");

    private String status;

    StatusEnum(String s) {
        this.status = s;
    }

    public String toString() {
        return this.status;
    }

    public String getStatus() {
        return status;
    }
}

