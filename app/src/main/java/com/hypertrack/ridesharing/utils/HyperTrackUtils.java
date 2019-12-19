package com.hypertrack.ridesharing.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;


public class HyperTrackUtils {
    private static String HYPERTRACK_PUB_KEY;

    public static String getPubKey(Context context) {
        if (TextUtils.isEmpty(HYPERTRACK_PUB_KEY)) {
            try {
                ApplicationInfo app = context.getPackageManager()
                        .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                HYPERTRACK_PUB_KEY = app.metaData.getString("com.hypertrack.sdk.PUB_KEY");
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        if (TextUtils.isEmpty(HYPERTRACK_PUB_KEY)) {
            throw new IllegalArgumentException("There is not HyperTrack PUB_KEY in manifest");
        }
        return HYPERTRACK_PUB_KEY;
    }
}
