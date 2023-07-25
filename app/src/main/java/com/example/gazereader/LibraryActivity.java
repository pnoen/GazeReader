package com.example.gazereader;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import camp.visual.gazetracker.GazeTracker;
import camp.visual.gazetracker.callback.GazeCallback;
import camp.visual.gazetracker.filter.OneEuroFilterManager;
import camp.visual.gazetracker.gaze.GazeInfo;
import camp.visual.gazetracker.state.EyeMovementState;
import camp.visual.gazetracker.state.TrackingState;
import camp.visual.gazetracker.util.ViewLayoutChecker;
import com.example.gazereader.view.GazePathView;

import java.util.Arrays;

public class LibraryActivity extends AppCompatActivity {
    private static final String TAG = LibraryActivity.class.getSimpleName();
    private final ViewLayoutChecker viewLayoutChecker = new ViewLayoutChecker();
    private GazePathView gazePathView;
    private GazeTrackerManager gazeTrackerManager;
    private final OneEuroFilterManager oneEuroFilterManager = new OneEuroFilterManager(
            2, 30, 0.5F, 0.001F, 1.0F);
    private LibraryDataStorage libraryDataStorage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);
        gazeTrackerManager = GazeTrackerManager.getInstance();
        libraryDataStorage = LibraryDataStorage.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback);
        initView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gazeTrackerManager.startGazeTracking();
        setOffsetOfView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gazeTrackerManager.stopGazeTracking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        gazeTrackerManager.removeCallbacks(gazeCallback);
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
    private Button btnPictureOfDorianGray;
    private Button btnEnchantedApril;
    private Button btnBlueCastle;
    private Button btnBrothersKaramazov;
    private Button btnAdventuresOfRoderick;
    private ScrollView scrollView;
    private Button btnLibraryScrollUp;
    private Button btnLibraryScrollDown;
    private View confirmationPopup;

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

        btnPictureOfDorianGray = findViewById(R.id.btn_picture_of_dorian_gray);
        btnPictureOfDorianGray.setOnClickListener(onClickListenerBookBtn);

        btnEnchantedApril = findViewById(R.id.btn_enchanted_april);
        btnEnchantedApril.setOnClickListener(onClickListenerBookBtn);

        btnBlueCastle = findViewById(R.id.btn_blue_castle);
        btnBlueCastle.setOnClickListener(onClickListenerBookBtn);

        btnBrothersKaramazov = findViewById(R.id.btn_brothers_karamazov);
        btnBrothersKaramazov.setOnClickListener(onClickListenerBookBtn);

        btnAdventuresOfRoderick = findViewById(R.id.btn_adventures_of_roderick);
        btnAdventuresOfRoderick.setOnClickListener(onClickListenerBookBtn);

        scrollView = findViewById(R.id.main_scrollview);

        btnLibraryScrollUp = findViewById(R.id.btn_library_scroll_up);
        btnLibraryScrollUp.setOnClickListener(onClickListenerScroll);

        btnLibraryScrollDown = findViewById(R.id.btn_library_scroll_down);
        btnLibraryScrollDown.setOnClickListener(onClickListenerScroll);

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
                Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }
        }
    };

    private View.OnClickListener onClickListenerBookBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnMiddlemarch) {
                checkBookmark("middlemarch.epub");
            }
            else if (v == btnRomeoAndJuliet) {
                checkBookmark("romeo_and_juliet.epub");
            }
            else if (v == btnRoomWithView) {
                checkBookmark("a_room_with_a_view.epub");
            }
            else if (v == btnPictureOfDorianGray) {
                checkBookmark("picture_of_dorian_gray.epub");
            }
            else if (v == btnEnchantedApril) {
                checkBookmark("enchanted_april.epub");
            }
            else if (v == btnBlueCastle) {
                checkBookmark("blue_castle.epub");
            }
            else if (v == btnBrothersKaramazov) {
                checkBookmark("brothers_karamazov.epub");
            }
            else if (v == btnAdventuresOfRoderick) {
                checkBookmark("adventures_of_roderick.epub");
            }
        }
    };

    private void checkBookmark(String book) {
        int[] data = libraryDataStorage.loadCalibrationData(book);
        if (data != null) {
            if (data[0] != 3 || data[1] != 0 || data[2] != 0) {
//                Log.i("BOOK DATA FOUND", Arrays.toString(data));

                TextView popupTitle = findViewById(R.id.popup_title);
                popupTitle.setText("Bookmark found");

                TextView popupMessage = findViewById(R.id.popup_message);
                popupMessage.setText("Where would you like to start reading from?");

                Button popupNegativeButton = findViewById(R.id.btn_negative);
                popupNegativeButton.setText("Beginning");
                popupNegativeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeButtonsState(true);
                        showBook(book);
                    }
                });

                Button popupPositiveButton = findViewById(R.id.btn_postive);
                popupPositiveButton.setText("Bookmark");
                popupPositiveButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        changeButtonsState(true);
                        showBookBookmarked(book, data);
                    }
                });

                changeButtonsState(false);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        confirmationPopup.setVisibility(View.VISIBLE);
                    }
                });
                return;
            }
        }

        showBook(book);
    }

    private void showBook(String book) {
        Intent intent = new Intent(getApplicationContext(), ReaderActivity.class);
        intent.putExtra("book", book);
        startActivity(intent);
    }

    private void showBookBookmarked(String book, int[] bookData) {
        Intent intent = new Intent(getApplicationContext(), ReaderActivity.class);
        intent.putExtra("book", book);
        intent.putExtra("bookData", bookData);
        startActivity(intent);
    }

    private View.OnClickListener onClickListenerScroll = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnLibraryScrollUp) {
                scrollView.smoothScrollBy(0, -1000);
            }
            else if (v == btnLibraryScrollDown) {
                scrollView.smoothScrollBy(0, 1000);
            }
        }
    };

    private void changeButtonsState(boolean state) {
        btnSettings.setEnabled(state);
        btnMiddlemarch.setEnabled(state);
        btnRomeoAndJuliet.setEnabled(state);
        btnRoomWithView.setEnabled(state);
        btnPictureOfDorianGray.setEnabled(state);
        btnEnchantedApril.setEnabled(state);
        btnBlueCastle.setEnabled(state);
        btnBrothersKaramazov.setEnabled(state);
        btnAdventuresOfRoderick.setEnabled(state);
        btnLibraryScrollUp.setEnabled(state);
        btnLibraryScrollDown.setEnabled(state);
    }
}
