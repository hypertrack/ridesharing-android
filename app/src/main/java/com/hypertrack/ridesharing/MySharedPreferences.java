package com.hypertrack.ridesharing;

import android.content.Context;

public class MySharedPreferences {
    public static final String USER_KEY = "user";
    public static final String ORDER_KEY = "order";
    public static final String TRIP_KEY = "trip";

    public static android.content.SharedPreferences get(Context context) {
        return context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
    }
}
