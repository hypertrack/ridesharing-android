package com.hypertrack.uber_consumer.models;

import com.google.firebase.database.PropertyName;

/**
 * Created by pkharche on 16/04/18.
 */
public class HypertrackDetails {
    String collectionId;
    String dropActionId;
    String pickupActionId;
    String dropUniqueId;
    String pickupUniqueId;
    String dropTrackingUrl;
    String pickupTrackingUrl;

    @PropertyName("collection_id")
    public String getCollectionId() {
        return collectionId;
    }

    @PropertyName("collection_id")
    public void setCollectionId(String collectionId) {
        this.collectionId = collectionId;
    }

    @PropertyName("drop_action_id")
    public String getDropActionId() {
        return dropActionId;
    }

    @PropertyName("drop_action_id")
    public void setDropActionId(String dropActionId) {
        this.dropActionId = dropActionId;
    }

    @PropertyName("pickup_action_id")
    public String getPickupActionId() {
        return pickupActionId;
    }

    @PropertyName("pickup_action_id")
    public void setPickupActionId(String pickupActionId) {
        this.pickupActionId = pickupActionId;
    }

    @PropertyName("drop_unique_id")
    public String getDropUniqueId() {
        return dropUniqueId;
    }

    @PropertyName("drop_unique_id")
    public void setDropUniqueId(String dropUniqueId) {
        this.dropUniqueId = dropUniqueId;
    }

    @PropertyName("pickup_unique_id")
    public String getPickupUniqueId() {
        return pickupUniqueId;
    }

    @PropertyName("pickup_unique_id")
    public void setPickupUniqueId(String pickupUniqueId) {
        this.pickupUniqueId = pickupUniqueId;
    }

    @PropertyName("drop_tracking_url")
    public String getDropTrackingUrl() {
        return dropTrackingUrl;
    }

    @PropertyName("drop_tracking_url")
    public void setDropTrackingUrl(String dropTrackingUrl) {
        this.dropTrackingUrl = dropTrackingUrl;
    }

    @PropertyName("pickup_tracking_url")
    public String getPickupTrackingUrl() {
        return pickupTrackingUrl;
    }

    @PropertyName("pickup_tracking_url")
    public void setPickupTrackingUrl(String pickupTrackingUrl) {
        this.pickupTrackingUrl = pickupTrackingUrl;
    }
}
