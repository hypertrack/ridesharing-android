package com.hypertrack.uber_consumer.utils;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.hypertrack.uber_consumer.widgets.CustomProgressDialog;

public class Utils {

    public static CustomProgressDialog sProgressDialog;
    public final static String TAG = "Utils";

    public static void hideProgressDialog() {
        if (sProgressDialog != null && sProgressDialog.isShowing()) {
            try {
                sProgressDialog.dismiss();
                sProgressDialog = null;
            } catch (Exception e) {
                sProgressDialog = null;
                Log.e(Constants.TAG, "Utils:: hideProgressDialog: ", e);
            }
        }
    }

    public static void showProgressDialog(Activity context, boolean isCancelable) {
        hideProgressDialog();
        if (context != null) {
            try {
                sProgressDialog = new CustomProgressDialog(context);
                sProgressDialog.setCanceledOnTouchOutside(true);
                sProgressDialog.setCancelable(isCancelable);
                sProgressDialog.show();
            } catch (Exception e) {
                Log.e(Constants.TAG, "Utils:: showProgressDialog: ", e);
            }
        }
    }

    public static boolean isInternetConnected(Context ctx) {
        if (ctx != null) {
            ConnectivityManager connectivityMgr = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityMgr != null) {
                NetworkInfo networkInfo = connectivityMgr.getActiveNetworkInfo();
                if (networkInfo != null && networkInfo.isConnected()) {
                    return true;
                }
            }
        }
        return false;
    }
}