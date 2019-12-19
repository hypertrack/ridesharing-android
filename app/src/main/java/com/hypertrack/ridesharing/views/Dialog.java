package com.hypertrack.ridesharing.views;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import com.hypertrack.ridesharing.R;

public class Dialog extends android.app.Dialog {

    public Dialog(@NonNull Context context, int layoutResID) {
        super(context, R.style.DialogTheme);
        setContentView(layoutResID);
    }

    @NonNull
    public Dialog setAction(final View.OnClickListener listener) {
        View view = findViewById(R.id.ht_action);
        if (view != null) {
            if (listener != null) {
                view.setVisibility(View.VISIBLE);
                view.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        listener.onClick(view);
                    }
                });
            } else {
                view.setVisibility(View.GONE);
                view.setOnClickListener(null);
            }
        }

        return this;
    }

    @NonNull
    public Dialog setAction(int viewId, final View.OnClickListener listener) {
        View view = findViewById(viewId);
        if (view != null) {
            view.setOnClickListener(listener);
        }

        return this;
    }
}
