package com.example.gazereader;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.CalibrationCallback;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.callback.InitializationCallback;
import camp.visual.gazetracker.callback.StatusCallback;
import camp.visual.gazetracker.constant.AccuracyCriteria;
import camp.visual.gazetracker.constant.CalibrationModeType;
import camp.visual.gazetracker.constant.InitializationErrorType;
import camp.visual.gazetracker.constant.StatusErrorType;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;

import com.example.gazereader.GazeTrackerManager.LoadCalibrationResult;
import com.example.gazereader.view.CalibrationViewer;
import com.example.gazereader.view.GazePathView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.CAMERA // eye tracking input
    };
    private static final int REQ_PERMISSION = 1000;
    private GazeTrackerManager gazeTrackerManager;
    private ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private HandlerThread backgroundThread = new HandlerThread("background");
    private Handler backgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        gazeTrackerManager = GazeTrackerManager.makeNewInstance(this);
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());

        initView();
        checkPermission();
        initHandler();
        initGaze();
    }

    @Override
    protected void onStart() {
        super.onStart();

        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback, calibrationCallback, statusCallback);
        Log.i(TAG, "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        //To check even after switching screens
        setOffsetOfView();
        gazeTrackerManager.startGazeTracking();
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

    // permission
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Check permission status
            if (!hasPermissions(PERMISSIONS)) {

                requestPermissions(PERMISSIONS, REQ_PERMISSION);
            } else {
                checkPermission(true);
            }
        }else{
            checkPermission(true);
        }
    }
    @RequiresApi(Build.VERSION_CODES.M)
    private boolean hasPermissions(String[] permissions) {
        int result;
        // Check permission status in string array
        for (String perms : permissions) {
            if (perms.equals(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                if (!Settings.canDrawOverlays(this)) {
                    return false;
                }
            }
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {
                // When if unauthorized permission found
                return false;
            }
        }
        // When if all permission allowed
        return true;
    }

    private void checkPermission(boolean isGranted) {
        if (isGranted) {
            permissionGranted();
        } else {
            showToast("not granted permissions", true);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION:
                if (grantResults.length > 0) {
                    boolean cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraPermissionAccepted) {
                        checkPermission(true);
                    } else {
                        checkPermission(false);
                    }
                }
                break;
        }
    }

    private void permissionGranted() {
        setViewAtGazeTrackerState();
    }

    // view
    private View layoutProgress;
    private View viewWarningTracking;
//    private PointView viewPoint;
//    private FaceBoxView viewFaceBox;
    private GazePathView gazePathView;
    private Button btnStartCalibration, btnSetCalibration;
    private Button btnGuiDemo;
    private CalibrationViewer viewCalibration;
//    private LinearLayout linearLayoutView;

    // calibration type
    private CalibrationModeType calibrationType = CalibrationModeType.SIX_POINT;
    private AccuracyCriteria criteria = AccuracyCriteria.HIGH;
    private boolean isUseGazeFilter = true;

//    private AppCompatTextView txtGazeVersion;
    private void initView() {
        layoutProgress = findViewById(R.id.layout_progress);
        layoutProgress.setOnClickListener(null);

        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        btnStartCalibration = findViewById(R.id.btn_start_calibration);
        btnStartCalibration.setOnClickListener(onClickListenerBtn1);

//        btnSetCalibration = findViewById(R.id.btn_set_calibration);
//        btnSetCalibration.setOnClickListener(onClickListenerBtn2);
//        btnSetCalibration.setOnTouchListener(onTouchListener);

        btnGuiDemo = findViewById(R.id.btn_gui_demo);
        btnGuiDemo.setOnClickListener(onClickListenerBtn3);

//        viewPoint = findViewById(R.id.view_point);
//        viewFaceBox = findViewById(R.id.view_face_box);
        viewCalibration = findViewById(R.id.view_calibration);
        gazePathView = findViewById(R.id.gazePathView);

        hideProgress();
        setOffsetOfView();
        setViewAtGazeTrackerState();
    }

    // The gaze or calibration coordinates are delivered only to the absolute coordinates of the entire screen.
    // The coordinate system of the Android view is a relative coordinate system,
    // so the offset of the view to show the coordinates must be obtained and corrected to properly show the information on the screen.
    private void setOffsetOfView() {
        viewLayoutChecker.setOverlayView(gazePathView, new ViewLayoutChecker.ViewLayoutListener() {
//        viewLayoutChecker.setOverlayView(viewPoint, new ViewLayoutChecker.ViewLayoutListener() {
            @Override
            public void getOffset(int x, int y) {
                gazePathView.setOffset(x, y);
//                viewFaceBox.setOffset(x, y);
                viewCalibration.setOffset(x, y);
            }
        });
    }

    private void showProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    private void hideProgress() {
        if (layoutProgress != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    layoutProgress.setVisibility(View.INVISIBLE);
                }
            });
        }
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

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnStartCalibration) {
                startCalibration();
            }
//            else if (v == btnSetCalibration) {
//                setCalibration();
//            }
            else if (v == btnGuiDemo) {
                showGuiDemo();
            }
        }
    };

    private View.OnClickListener onClickListenerBtn1 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            Log.i("Btn 1", "CLICK EVENT");
            startCalibration();
        }
    };

    private View.OnClickListener onClickListenerBtn2 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            Log.i("Btn 2", "CLICK EVENT");
            setCalibration();
        }
    };

    private View.OnClickListener onClickListenerBtn3 = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
//            Log.i("Btn 3", "CLICK EVENT");
            showGuiDemo();
        }
    };

    private void showToast(final String msg, final boolean isShort) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, isShort ? Toast.LENGTH_SHORT : Toast.LENGTH_LONG).show();
            }
        });
    }

//    private void showGazePoint(final float x, final float y, final ScreenState type) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                viewPoint.setType(type == ScreenState.INSIDE_OF_SCREEN ? PointView.TYPE_DEFAULT : PointView.TYPE_OUT_OF_SCREEN);
//                viewPoint.setPosition(x, y);
//            }
//        });
//    }

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
                btnStartCalibration.setEnabled(isTracking());
//                btnSetCalibration.setEnabled(isTrackerValid());
                if (!isTracking()) {
                    hideCalibrationView();
                }
            }
        });
    }

    // gazeTracker
    private boolean isTrackerValid() {
        return gazeTrackerManager.hasGazeTracker();
    }

    private boolean isTracking() {
        return gazeTrackerManager.isTracking();
    }

    private final InitializationCallback initializationCallback = new InitializationCallback() {
        @Override
        public void onInitialized(GazeTracker gazeTracker, InitializationErrorType error) {
            if (gazeTracker != null) {
                initSuccess(gazeTracker);
            } else {
                initFail(error);
            }
        }
    };

    private void initSuccess(GazeTracker gazeTracker) {
        setViewAtGazeTrackerState();
        hideProgress();
        startTracking();
        setCalibration();
    }

    private void initFail(InitializationErrorType error) {
        hideProgress();
    }

    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(2);
    private final GazeCallback gazeCallback = new GazeCallback() {
        @Override
        public void onGaze(GazeInfo gazeInfo) {
            processOnGaze(gazeInfo);
//            Log.i(TAG, "check eyeMovement " + gazeInfo.eyeMovementState);
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
            showToast("calibrationFinished", true);
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

    private void initGaze() {
        showProgress();

        gazeTrackerManager.initGazeTracker(initializationCallback);
    }

    private void startTracking() {
        gazeTrackerManager.startGazeTracking();
    }


    private boolean startCalibration() {
        boolean isSuccess = gazeTrackerManager.startCalibration(calibrationType, criteria);
        if (!isSuccess) {
            showToast("calibration start fail", false);
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

    private void setCalibration() {
        LoadCalibrationResult result = gazeTrackerManager.loadCalibrationData();
        switch (result) {
            case SUCCESS:
                showToast("setCalibrationData success", true);
                break;
            case FAIL_DOING_CALIBRATION:
                showToast("calibrating", true);
                break;
//            case FAIL_NO_CALIBRATION_DATA:
//                showToast("Calibration data is null", true);
//                break;
            case FAIL_HAS_NO_TRACKER:
                showToast("No tracker has initialized", true);
                break;
        }
        setViewAtGazeTrackerState();
    }

    private void showGuiDemo() {
        Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
        startActivity(intent);
    }
}
