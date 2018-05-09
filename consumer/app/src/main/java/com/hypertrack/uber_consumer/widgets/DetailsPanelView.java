package com.hypertrack.uber_consumer.widgets;

import android.os.Bundle;

import com.hypertrack.lib.tracking.MapProvider.MapFragmentView;
import com.hypertrack.lib.tracking.UseCase.OrderTracking.OrderTrackingView;
import com.hypertrack.lib.tracking.model.ActionSummaryModel;
import com.hypertrack.lib.tracking.model.UserProfileModel;

import java.util.List;

/**
 * Created by pkharche on 09/04/18.
 */
public class DetailsPanelView extends OrderTrackingView {

    public static DetailsPanelView getInstance() { //for now same as OrderTrackingView
        DetailsPanelView orderTrackingView = new DetailsPanelView();
        Bundle bundle = new Bundle();
        bundle.putInt("type", MapFragmentView.Type.ORDER_TRACKING);
        orderTrackingView.setArguments(bundle);
        return orderTrackingView;
    }

    @Override
    public void updateUserProfileView(List<UserProfileModel> userProfileModels) { }

    @Override
    public void clearView() { }

    @Override
    public void updateActionSummary(ActionSummaryModel actionSummaryModel) { }

}
