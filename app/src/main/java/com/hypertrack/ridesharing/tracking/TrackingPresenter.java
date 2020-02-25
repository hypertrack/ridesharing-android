package com.hypertrack.ridesharing.tracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.google.android.gms.common.wrappers.InstantApps;
import com.hypertrack.sdk.HyperTrack;
import com.hypertrack.sdk.TrackingError;
import com.hypertrack.sdk.TrackingStateObserver;
import com.hypertrack.ridesharing.utils.HyperTrackUtils;

@SuppressWarnings({"unused", "WeakerAccess"})
public class TrackingPresenter implements TrackingStateObserver.OnTrackingStateChangeListener {
    private static final String TAG = "TrackingPresenter";

    public static final int PERMISSIONS_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 616;

    protected final Context mContext;
    private final View mView;

    protected HyperTrack hyperTrack;

    @SuppressWarnings("ConstantConditions")
    public TrackingPresenter(@NonNull Context context, @NonNull View view) {
        mContext = context.getApplicationContext() == null ? context : context.getApplicationContext();
        mView = view;

        hyperTrack = HyperTrack.getInstance(context, HyperTrackUtils.getPubKey(context));
    }

    @SuppressWarnings("ConstantConditions")
    public void requestIgnoreBatteryOptimizations() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Activity.POWER_SERVICE);
        String packageName = mContext.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !pm.isIgnoringBatteryOptimizations(packageName)
                && !InstantApps.isInstantApp(mContext)) {
            mView.requestPermissions(new String[]{Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS},
                    PERMISSIONS_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }
    }

    public void resume() {
        if (hyperTrack.isRunning()) {
            mView.onTrackingStart();
        } else {
            mView.onTrackingStop();
        }
    }

    public void adjustTrackingState() {
        hyperTrack.syncDeviceSettings();
        if (!isGpsProviderEnabled(mContext)) {
            actionLocationSourceSettings();
        }
    }

    public void actionLocationSourceSettings() {
        if (mContext != null && !InstantApps.isInstantApp(mContext)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            mContext.startActivity(intent);
        }
    }

    @SuppressLint({"BatteryLife", "InlinedApi"})
    @SuppressWarnings("ConstantConditions")
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PowerManager pm = (PowerManager) mContext.getSystemService(Activity.POWER_SERVICE);
                String packageName = mContext.getPackageName();
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    Uri uri = Uri.parse("package:" + packageName);
                    intent.setData(uri);
                    mView.startActivityForResult(intent, PERMISSIONS_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                }
            }
        }
    }

    public void destroy() {
        hyperTrack.removeTrackingListener(this);
        hyperTrack.syncDeviceSettings();
    }

    private static boolean isGpsProviderEnabled(Context context) {
        if (context != null) {
            LocationManager cm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return cm != null && cm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        return true;
    }

    @Override
    public void onError(TrackingError trackingError) {
        mView.onError(trackingError);
    }

    @Override
    public void onTrackingStart() {
        mView.onTrackingStart();
    }

    @Override
    public void onTrackingStop() {
        mView.onTrackingStop();
    }

    public interface View extends TrackingStateObserver.OnTrackingStateChangeListener {

        void requestPermissions(String[] permissions, int requestCode);

        void startActivityForResult(Intent intent, int requestCode);
    }

    public static class State {
    }
}
