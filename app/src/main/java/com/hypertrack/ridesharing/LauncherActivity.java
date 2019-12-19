package com.hypertrack.ridesharing;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import com.daimajia.androidanimations.library.Techniques;
import com.daimajia.androidanimations.library.YoYo;
import com.hypertrack.ridesharing.components.MainActivity;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class LauncherActivity extends Activity{

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 1000;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private View mControlsView;
    private View text;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Show the system bar
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

            // Delayed display of UI elements
            mControlsView.setVisibility(View.VISIBLE);
            YoYo.with(Techniques.ZoomInRight)
                    .duration(1200)
                    .pivot(YoYo.CENTER_PIVOT, YoYo.CENTER_PIVOT)
                    .interpolate(new AccelerateDecelerateInterpolator())
                    .playOn(text);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        if (MySharedPreferences.get(this).contains(MySharedPreferences.USER_KEY)) {
            start(MainActivity.class);
        } else {
            mControlsView = findViewById(R.id.fullscreen_content_controls);
            text = findViewById(R.id.text);
            mContentView = findViewById(R.id.fullscreen_content);


            findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    start(RegistrationActivity.class);
                }
            });

            // Schedule a runnable to display UI elements after a delay
            mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        hide();
    }

    private void hide() {
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void start(Class<?> cls) {
        // Some preparation before start main functional

        startActivity(new Intent(this, cls));
        finish();
    }
}
