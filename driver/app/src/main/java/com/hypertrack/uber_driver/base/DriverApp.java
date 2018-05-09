package com.hypertrack.uber_driver.base;

import android.app.Application;
import android.util.Log;

import com.hypertrack.uber_driver.R;
import com.hypertrack.lib.HyperTrack;

/**
 * Created by pkharche on 06/04/18.
 */
public class DriverApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HyperTrack.initialize(this, getString(R.string.hypertrack_key));
        HyperTrack.enableDebugLogging(Log.VERBOSE);
        //TODO add Fabric/Crashlytics
    }
}
