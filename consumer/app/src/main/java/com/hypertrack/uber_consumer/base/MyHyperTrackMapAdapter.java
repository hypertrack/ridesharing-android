package com.hypertrack.uber_consumer.base;

import android.content.Context;

import com.google.android.gms.maps.model.PatternItem;
import com.hypertrack.lib.HyperTrackMapAdapter;
import com.hypertrack.lib.internal.transmitter.models.UserActivity;
import com.hypertrack.lib.tracking.model.InfoBoxModel;
import com.hypertrack.uber_consumer.R;

import java.util.List;

/**
 * Created by pkharche on 13/04/18.
 */
public class MyHyperTrackMapAdapter extends HyperTrackMapAdapter {

    private boolean showActionRoute = false;

    public MyHyperTrackMapAdapter(Context mContext) {
        super(mContext);
    }

    @Override
    public int getUserMarkerIconForActionID(Context mContext, InfoBoxModel.Type markerType, UserActivity.ActivityType activityType, String actionID) {
        return R.drawable.icondrive;
    }

    @Override
    public int getSourcePlaceMarkerIconForActionID(Context mContext, String actionID) {
        return R.drawable.iconsource_marker;
    }

    @Override
    public int getExpectedPlaceMarkerIconForActionID(Context mContext, String actionID) {
        return R.drawable.icondestination_marker;
    }

    @Override
    public int getActionSummaryDestinationMarkerIconForActionID(Context mContext, String actionID) {
        return R.drawable.icondestination_marker;
    }

    @Override
    public int getActionSummarySourceMarkerIconForActionID(Context mContext, String actionID) {
        return R.drawable.iconsource_marker;
    }

    @Override
    public List<PatternItem> getRoutePolylinePattern() {
        return null;
    }

    @Override
    public int getRoutePolylineColor() {
        return R.color.text_color;
    }

    public void setShowActionRoute(boolean showActionRoute) {
        this.showActionRoute = showActionRoute;
    }

    //showActionRoute
    @Override
    public boolean showTrailingPolyline() {
        return true;
    }

    @Override
    public int getBoundButtonTintColor() {
        return R.color.text_color;
    }

    @Override
    public int getPulseColor() {
        return R.color.sea_green_color_ring;
    }

    public boolean showBackButton() {
        return false;
    }
}
