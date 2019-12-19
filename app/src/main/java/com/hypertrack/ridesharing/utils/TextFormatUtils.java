package com.hypertrack.ridesharing.utils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;

import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.models.Order;

import java.util.concurrent.TimeUnit;

public class TextFormatUtils {

    public static String getRelativeDateTimeString(Context context, long time) {
        return DateUtils.getRelativeDateTimeString(context, time,
                TimeUnit.MINUTES.toMillis(1), TimeUnit.DAYS.toMillis(7),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_ABBREV_ALL).toString();
    }

    public static Spanned getDestinationName(Context context, Order order) {
        boolean isPickup = Order.PICKING_UP.equals(order.status)
                || Order.REACHED_PICKUP.equals(order.status);
        String address = isPickup ? order.pickup.address : order.dropoff.address;
        String text = isPickup ? context.getString(R.string.address_pickup) : context.getString(R.string.address_dropoff);
        return Html.fromHtml(String.format(text, address));
    }
}
