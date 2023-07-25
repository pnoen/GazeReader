package com.example.gazereader;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

import com.example.gazereader.view.CalibrationViewer;
import com.example.gazereader.view.GazePathView;


public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = SettingsActivity.class.getSimpleName();
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);
    private Handler backgroundHandler;
    private HandlerThread backgroundThread = new HandlerThread("background");
    private LibraryDataStorage libraryDataStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        gazeTrackerManager = GazeTrackerManager.getInstance();
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());
        initView();
        initHandler();

        libraryDataStorage = LibraryDataStorage.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, calibrationCallback, statusCallback);
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
        gazeTrackerManager.removeCallbacks(gazeCallback, calibrationCallback, statusCallback);
        Log.i(TAG, "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseHandler();
        viewLayoutChecker.releaseChecker();
    }

    // handler
    private void initHandler() {
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void releaseHandler() {
        backgroundThread.quitSafely();
    }

    private Button btnHome;
    private Button btnCalibrate;
    private Button btnReset;
    private View viewWarningTracking;
    private CalibrationViewer viewCalibration;
    private View confirmationPopup;

    private CalibrationModeType calibrationType = CalibrationModeType.SIX_POINT;
    private AccuracyCriteria criteria = AccuracyCriteria.HIGH;
    private boolean isUseGazeFilter = true;

    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);
        viewWarningTracking = findViewById(R.id.view_warning_tracking);
        viewCalibration = findViewById(R.id.view_calibration);

        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(onClickListenerHome);

        btnCalibrate = findViewById(R.id.btn_calibrate);
        btnCalibrate.setOnClickListener(onClickListenerCalibrate);

        btnReset = findViewById(R.id.btn_reset);
        btnReset.setOnClickListener(onClickListenerReset);

        confirmationPopup = findViewById(R.id.confirmation_popup);
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
//            if (gazeInfo.trackingState == TrackingState.SUCCESS) {
//                hideTrackingWarning();
//                if (!gazeTrackerManager.isCalibrating()) {
//                    if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
//                        float[] filtered = oneEuroFilterManager.getFilteredValues();
////                    Log.i(TAG, "x: " + filtered[0] + " y: " + filtered[1]);
//                        gazePathView.onGaze(filtered[0], filtered[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
//                    }
//                }
//            } else {
//                showTrackingWarning();
//            }
            processOnGaze(gazeInfo);
        }
    };

    private void processOnGaze(GazeInfo gazeInfo) {
        if (gazeInfo.trackingState == TrackingState.SUCCESS) {
            hideTrackingWarning();
            if (!gazeTrackerManager.isCalibrating()) {
                float[] filtered_gaze = filterGaze(gazeInfo);
//                Log.i(TAG, "x: " + filtered_gaze[0] + " y: " + filtered_gaze[1]);
//                showGazePoint(filtered_gaze[0], filtered_gaze[1], gazeInfo.screenState);
                gazePathView.onGaze(filtered_gaze[0], filtered_gaze[1], gazeInfo.eyeMovementState == EyeMovementState.FIXATION);
            }
        } else {
            showTrackingWarning();
        }
    }

    private float[] filterGaze(GazeInfo gazeInfo) {
        if (isUseGazeFilter) {
            if (oneEuroFilterManager.filterValues(gazeInfo.timestamp, gazeInfo.x, gazeInfo.y)) {
                return oneEuroFilterManager.getFilteredValues();
            }
        }
        return new float[]{gazeInfo.x, gazeInfo.y};
    }

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

    private View.OnClickListener onClickListenerHome = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnHome) {
                showLibraryPage();
            }
        }
    };

    private void showLibraryPage() {
        Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
        startActivity(intent);
    }

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SettingsActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setCalibrationPoint(final float x, final float y) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.VISIBLE);
                viewCalibration.changeDraw(true, "Look at the red circles");
                viewCalibration.setPointPosition(x, y);
                viewCalibration.setPointAnimationPower(0);
            }
        });
    }

    private void setCalibrationProgress(final float progress) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setPointAnimationPower(progress);
            }
        });
    }

    private void hideCalibrationView() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                viewCalibration.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void setViewAtGazeTrackerState() {
        Log.i(TAG, "gaze : " + isTrackerValid() + ", tracking " + isTracking());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnCalibrate.setEnabled(isTracking());
//                btnSetCalibration.setEnabled(isTrackerValid());
                if (!isTracking()) {
                    hideCalibrationView();
                }
            }
        });
    }

    private boolean isTrackerValid() {
        return gazeTrackerManager.hasGazeTracker();
    }

    private boolean isTracking() {
        return gazeTrackerManager.isTracking();
    }

    private CalibrationCallback calibrationCallback = new CalibrationCallback() {
        @Override
        public void onCalibrationProgress(float progress) {
            setCalibrationProgress(progress);
        }

        @Override
        public void onCalibrationNextPoint(final float x, final float y) {
            setCalibrationPoint(x, y);
            // Give time to eyes find calibration coordinates, then collect data samples
            backgroundHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startCollectSamples();
                }
            }, 1000);
        }

        @Override
        public void onCalibrationFinished(double[] calibrationData) {
            // When calibration is finished, calibration data is stored to SharedPreference
            hideCalibrationView();
            showToast("Calibration complete", true);
        }
    };

    private StatusCallback statusCallback = new StatusCallback() {
        @Override
        public void onStarted() {
            // isTracking true
            // When if camera stream starting
            setViewAtGazeTrackerState();
        }

        @Override
        public void onStopped(StatusErrorType error) {
            // isTracking false
            // When if camera stream stopping
            setViewAtGazeTrackerState();

            if (error != StatusErrorType.ERROR_NONE) {
                switch (error) {
                    case ERROR_CAMERA_START:
                        // When if camera stream can't start
                        showToast("ERROR_CAMERA_START ", false);
                        break;
                    case ERROR_CAMERA_INTERRUPT:
                        // When if camera stream interrupted
                        showToast("ERROR_CAMERA_INTERRUPT ", false);
                        break;
                }
            }
        }
    };

    private boolean startCalibration() {
        boolean isSuccess = gazeTrackerManager.startCalibration(calibrationType, criteria);
        if (!isSuccess) {
            showToast("Calibration start failed", false);
        }
        setViewAtGazeTrackerState();
        return isSuccess;
    }

    // Collect the data samples used for calibration
    private boolean startCollectSamples() {
        boolean isSuccess = gazeTrackerManager.startCollectingCalibrationSamples();
        setViewAtGazeTrackerState();
        return isSuccess;
    }

    private View.OnClickListener onClickListenerCalibrate = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCalibrate) {
                startCalibration();
            }
        }
    };

    private View.OnClickListener onClickListenerReset = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnReset) {
                showResetConfirmation();
            }
        }
    };

    private void showResetConfirmation() {
        TextView popupTitle = findViewById(R.id.popup_title);
        popupTitle.setText("Reset library");

        TextView popupMessage = findViewById(R.id.popup_message);
        popupMessage.setText("This will remove all bookmarks and zoom levels in the library.");

        TextView popupMessage2 = findViewById(R.id.popup_message2);
        popupMessage2.setVisibility(View.VISIBLE);
        popupMessage2.setText("Do you want to continue?");

        Button popupNegativeButton = findViewById(R.id.btn_negative);
        popupNegativeButton.setText("No");
        popupNegativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonsState(true);
                confirmationPopup.setVisibility(View.INVISIBLE);
            }
        });

        Button popupPositiveButton = findViewById(R.id.btn_postive);
        popupPositiveButton.setText("Yes");
        popupPositiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeButtonsState(true);
                libraryDataStorage.resetLibraryData();
                showToast("Library reset", true);
                confirmationPopup.setVisibility(View.INVISIBLE);
            }
        });

        changeButtonsState(false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                confirmationPopup.setVisibility(View.VISIBLE);
            }
        });
    }

    private void changeButtonsState(boolean state) {
        btnHome.setEnabled(state);
        btnCalibrate.setEnabled(state);
        btnReset.setEnabled(state);
    }
}