package com.hypertrack.ridesharing.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.android.gms.maps.model.Marker;
import com.hypertrack.ridesharing.parsers.Parser;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Order implements Parcelable {

    public static final String
            NEW = "NEW",
            ACCEPTED = "ACCEPTED",
            PICKING_UP = "PICKING_UP",
            REACHED_PICKUP = "REACHED_PICKUP",
            STARTED_RIDE = "STARTED_RIDE",
            DROPPING_OFF = "DROPPING_OFF",
            REACHED_DROPOFF = "REACHED_DROPOFF",
            CANCELLED = "CANCELLED",
            COMPLETED = "COMPLETED";

    //    id: String
//    status: String (Enum NEW | PICKING_UP | REACHED_PICKUP | DROPPING_OFF | REACHED_DROPOFF | COMPLETED)
//    rider: User
//    driver: User
//    pickup: Place
//    dropoff: Place
//    created_at: String (ISO8601 yyyy-MM-dd'T'HH:mm:ss'Z')
//    trip_id: String
    public String id;
    public String status;
    @JsonProperty("trip_id")
    public String tripId;
    public Place pickup;
    public Place dropoff;
    public User rider;
    public User driver;
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = Parser.DATE_FORMAT, timezone="UTC")
    @JsonProperty("created_at")
    public Date createdAt;
    @JsonFormat (shape = JsonFormat.Shape.STRING, pattern = Parser.DATE_FORMAT, timezone="UTC")
    @JsonProperty("updated_at")
    public Date updatedAt;

    @JsonIgnore
    public Marker marker;

    public Order() {
    }

    public Order(String id) {
        this.id = id;
    }

    public void update(Order newOrder) {
        status = newOrder.status;
        tripId = newOrder.tripId;
        pickup = newOrder.pickup;
        dropoff = newOrder.dropoff;
        rider = newOrder.rider;
        driver = newOrder.driver;
        createdAt = newOrder.createdAt;
        updatedAt = newOrder.updatedAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.id);
        dest.writeString(this.status);
        dest.writeString(this.tripId);
        dest.writeParcelable(this.pickup, flags);
        dest.writeParcelable(this.dropoff, flags);
        dest.writeParcelable(this.rider, flags);
        dest.writeParcelable(this.driver, flags);
        dest.writeLong(this.createdAt != null ? this.createdAt.getTime() : -1);
        dest.writeLong(this.updatedAt != null ? this.updatedAt.getTime() : -1);
    }

    protected Order(Parcel in) {
        this.id = in.readString();
        this.status = in.readString();
        this.tripId = in.readString();
        this.pickup = in.readParcelable(Place.class.getClassLoader());
        this.dropoff = in.readParcelable(Place.class.getClassLoader());
        this.rider = in.readParcelable(User.class.getClassLoader());
        this.driver = in.readParcelable(User.class.getClassLoader());
        long tmpCreatedAt = in.readLong();
        this.createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        long tmpUpdatedAt = in.readLong();
        this.updatedAt = tmpUpdatedAt == -1 ? null : new Date(tmpUpdatedAt);
    }

    public static final Creator<Order> CREATOR = new Creator<Order>() {
        @Override
        public Order createFromParcel(Parcel source) {
            return new Order(source);
        }

        @Override
        public Order[] newArray(int size) {
            return new Order[size];
        }
    };
}
