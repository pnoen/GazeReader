package com.example.gazereader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import com.example.gazereader.view.GazePathView;

public class LibraryActivity extends AppCompatActivity {
    private static final String TAG = LibraryActivity.class.getSimpleName();
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
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

    private Button btnSettings;
    private View viewWarningTracking;
    private Button btnMiddlemarch;
    private Button btnRomeoAndJuliet;
    private Button btnRoomWithView;
    private Button btnMobyDick;
    private Button btnEnchantedApril;
    private Button btnCranford;
    private Button btnExpeditionOfHumphry;
    private Button btnAdventuresOfRoderick;
    private ScrollView scrollView;
    private Button btnLibraryScrollUp;
    private Button btnLibraryScrollDown;

    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);
        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(onClickListenerSettings);

        btnMiddlemarch = findViewById(R.id.btn_middlemarch);
        btnMiddlemarch.setOnClickListener(onClickListenerBookBtn);

        btnRomeoAndJuliet = findViewById(R.id.btn_romeo_and_juliet);
        btnRomeoAndJuliet.setOnClickListener(onClickListenerBookBtn);

        btnRoomWithView = findViewById(R.id.btn_room_with_view);
        btnRoomWithView.setOnClickListener(onClickListenerBookBtn);

        btnMobyDick = findViewById(R.id.btn_moby_dick);
        btnMobyDick.setOnClickListener(onClickListenerBookBtn);

        btnEnchantedApril = findViewById(R.id.btn_enchanted_april);
        btnEnchantedApril.setOnClickListener(onClickListenerBookBtn);

        btnCranford = findViewById(R.id.btn_cranford);
        btnCranford.setOnClickListener(onClickListenerBookBtn);

        btnExpeditionOfHumphry = findViewById(R.id.btn_expedition_of_humphry);
        btnExpeditionOfHumphry.setOnClickListener(onClickListenerBookBtn);

        btnAdventuresOfRoderick = findViewById(R.id.btn_adventures_of_roderick);
        btnAdventuresOfRoderick.setOnClickListener(onClickListenerBookBtn);

        scrollView = findViewById(R.id.main_scrollview);

        btnLibraryScrollUp = findViewById(R.id.btn_library_scroll_up);
        btnLibraryScrollUp.setOnClickListener(onClickListenerScroll);

        btnLibraryScrollDown = findViewById(R.id.btn_library_scroll_down);
        btnLibraryScrollDown.setOnClickListener(onClickListenerScroll);
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
//                    Log.i(TAG, "x: " + filtered[0] + " y: " + filtered[1]);
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

    private View.OnClickListener onClickListenerSettings = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnSettings) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }
        }
    };

    private View.OnClickListener onClickListenerBookBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnMiddlemarch) {
                showBook("middlemarch.epub");
            }
            else if (v == btnRomeoAndJuliet) {
                showBook("romeo_and_juliet.epub");
            }
            else if (v == btnRoomWithView) {
                showBook("a_room_with_a_view.epub");
            }
            else if (v == btnMobyDick) {
                Log.i("BOOK", "HERE");
                showBook("moby_dick.epub");
            }
            else if (v == btnEnchantedApril) {
                showBook("enchanted_april.epub");
            }
            else if (v == btnCranford) {
                showBook("cranford.epub");
            }
            else if (v == btnExpeditionOfHumphry) {
                showBook("expedition_of_humphry.epub");
            }
            else if (v == btnAdventuresOfRoderick) {
                showBook("adventures_of_roderick.epub");
            }
        }
    };

    private void showBook(String book) {
        Intent intent = new Intent(getApplicationContext(), ReaderActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    private View.OnClickListener onClickListenerScroll = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnLibraryScrollUp) {
                int scrollValueY = scrollView.getScrollY();
                int scrollValueX = scrollView.getScrollX();
                scrollView.smoothScrollTo(scrollValueX, scrollValueY - 1000);
            }
            else if (v == btnLibraryScrollDown) {
                int scrollValueY = scrollView.getScrollY();
                int scrollValueX = scrollView.getScrollX();
                scrollView.smoothScrollTo(scrollValueX, scrollValueY + 1000);
            }
        }
    };
}
