package com.example.gazereader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import com.example.gazereader.view.GazePathView;

public class DemoActivity extends AppCompatActivity {
    private static final String TAG = DemoActivity.class.getSimpleName();
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);
        gazeTrackerManager = GazeTrackerManager.getInstance();
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gazeTrackerManager.startGazeTracking();
        setOffsetOfView();
        Log.i(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
        Log.i(TAG, "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(gazeCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Button btnHome;
    private View viewWarningTracking;

    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);
        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(onClickListener);

    }

    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(gazePathView, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                gazePathView.setOffset(x, y);
            }
        });
    }

    private final GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
                hideTrackingWarning();
                if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                    float[] filtered = oneEuroFilterManager.getFilteredValues();
                    Log.i(TAG, "x: " + filtered[0] + " y: " + filtered[1]);
                    gazePathView.onGaze(filtered[0], filtered[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
                }
            } else {
                showTrackingWarning();
            }
        }
    };

    private void showTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideTrackingWarning() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewWarningTracking.setVisibility(View.INVISIBLE);
            }
        });
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnHome) {
                showHome();
            }
        }
    };

    private void showHome() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }
}
