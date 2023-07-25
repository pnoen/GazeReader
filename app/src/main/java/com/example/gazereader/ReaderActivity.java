package com.example.gazereader;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import io.documentnode.epub4j.domain.Author;
import io.documentnode.epub4j.domain.Book;
import io.documentnode.epub4j.domain.Resource;
import io.documentnode.epub4j.domain.Spine;
import io.documentnode.epub4j.epub.EpubReader;

import com.example.gazereader.view.GazePathView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReaderActivity extends AppCompatActivity {
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
        setContentView(R.layout.activity_reader);
        gazeTrackerManager = GazeTrackerManager.getInstance();
        Log.i(TAG, "gazeTracker version: " + GazeTracker.getVersionName());

        libraryDataStorage = LibraryDataStorage.getInstance();

        setEpubReader();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        gazeTrackerManager.setGazeTrackerCallbacks(gazeCallback);
        initView();
        loadTextViews();
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
    private Button btnZoomOut;
    private Button btnZoomIn;
    private Button btnBookmark;
    private ScrollView scrollView;
    private Button btnScrollUp;
    private Button btnScrollDown;
    private TextView bookTitle;
    private TextView bookAuthor;
    private TextView bookText;

    private void initView() {
        gazePathView = findViewById(R.id.gazePathView);
        viewWarningTracking = findViewById(R.id.view_warning_tracking);

        btnHome = findViewById(R.id.btn_home);
        btnHome.setOnClickListener(onClickListenerHomeBtn);

        btnZoomOut = findViewById(R.id.btn_zoom_out);
        btnZoomOut.setOnClickListener(onClickListenerZoomBtn);

        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomIn.setOnClickListener(onClickListenerZoomBtn);

        btnBookmark = findViewById(R.id.btn_bookmark);
        btnBookmark.setOnClickListener(onClickListenerBookmarkBtn);

        scrollView = findViewById(R.id.main_scrollview);

        btnScrollUp = findViewById(R.id.btn_scroll_up);
        btnScrollUp.setOnClickListener(onClickListenerScroll);

        btnScrollDown = findViewById(R.id.btn_scroll_down);
        btnScrollDown.setOnClickListener(onClickListenerScroll);

        bookTitle = findViewById(R.id.book_title);
        bookAuthor = findViewById(R.id.book_author);
        bookText = findViewById(R.id.book_text);
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

    private View.OnClickListener onClickListenerHomeBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnHome) {
                showLibraryPage();
            }
        }
    };

    private View.OnClickListener onClickListenerZoomBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnZoomOut) {
                if (zoomLevel > -3) {
                    zoomLevel--;
                    setPageZoomLevel();
                }
            }
            else if (v == btnZoomIn) {
                if (zoomLevel < 3) {
                    zoomLevel++;
                    setPageZoomLevel();
                }
            }
        }
    };

    private View.OnClickListener onClickListenerBookmarkBtn = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnBookmark) {
//                Log.i("BUTTON PRESSED", "Bookmark");
                int[] bookData = new int[3];
                bookData[0] = pageCounter;
                bookData[1] = scrollView.getScrollY();
                bookData[2] = zoomLevel;
                libraryDataStorage.saveBookData(bookFile, bookData);
            }
        }
    };

    private View.OnClickListener onClickListenerScroll = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnScrollUp) {
                if (!scrollView.canScrollVertically(-1)) {
//                    Log.i("SCROLL", "Reached the top");
                    if (pageCounter > firstPage) {
                        pageCounter--;
                        setPage();
                    }
                }
                else {
                    scrollView.smoothScrollBy(0, -1200);
                }

            }
            else if (v == btnScrollDown) {
                if (!scrollView.canScrollVertically(1)) {
//                    Log.i("SCROLL", "Reached the bottom");
                    if (pageCounter < pages.size() - 2) {
                        pageCounter++;
                        scrollView.post(() -> {
                            scrollView.scrollTo(scrollView.getScrollX(), 0);
                        });
                        setPage();
                    }
                }
                else {
                    scrollView.smoothScrollBy(0, 1200);
                }
            }
        }
    };

    private void showLibraryPage() {
        Intent intent = new Intent(getApplicationContext(), LibraryActivity.class);
        startActivity(intent);
    }

    private String bookFile = null;
    private EpubReader epubReader;
    private Book book;
    private static int firstPage = 3; // removes the table of contents
    private int pageCounter = 3;
    private int zoomLevel = 0;
    private int[] bookData;

    private void setEpubReader() {
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            bookFile = extras.getString("book");
            bookData = extras.getIntArray("bookData");
            if (bookData != null) {
                pageCounter = bookData[0];
                zoomLevel = bookData[2];
            }
        }

        epubReader = new EpubReader();
        AssetManager assetManager = getApplicationContext().getAssets();
        try {
            InputStream inputStream = assetManager.open(bookFile);
            book = epubReader.readEpub(inputStream);
            Log.i("TRY", "book");

        } catch (IOException e) {
            Log.i("EPUB", "Not found " + bookFile);
            showLibraryPage();
        }
    }

    List<String> pages = new ArrayList<>();

    private void loadTextViews() {
        List<String> titles = book.getMetadata().getTitles();
        bookTitle.setText(titles.get(0));

        List<Author> authors = book.getMetadata().getAuthors();
        Author author = authors.get(0);
        String authorStr = author.getFirstname() + " " + author.getLastname();
        bookAuthor.setText(authorStr);

        setPageZoomLevel();

        pages = getBookContent();
        setPage();
        if (bookData != null) {
            scrollView.post(new Runnable() {
                @Override
                public void run() {
                    scrollView.scrollTo(scrollView.getScrollX(), bookData[1]);
                }
            });
        }
    }

//    https://stackoverflow.com/questions/34294104/i-am-using-the-epublib-and-i-am-trying-to-get-the-entire-chapter-of-a-book-at-a
    private List<String> getBookContent() {
        List<String> pages = new ArrayList<>();
        StringBuilder stringBuilder = new StringBuilder();
        Spine spine = book.getSpine();
        for (int i = 0; i < spine.size(); i++) {
            Resource resource = spine.getResource(i);
            try {
                InputStream inputStream = resource.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line = bufferedReader.readLine();;
                while (line != null) {
                    if (line.contains("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>")) {
                        stringBuilder.delete(0, stringBuilder.length());
                    }

                    if (line.contains("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd")) {
                        line = line.substring(line.indexOf(">") + 1);
                    }

                    if ((line.contains("{") && line.contains("}")) || ((line.contains("/*")) && line.contains("*/")) || (line.contains("<!--") && line.contains("-->"))) {
                        line = "";
                    }

                    if (!line.trim().equals("")) {
                        stringBuilder.append(Html.fromHtml(line));
                        if (stringBuilder.length() > 0) {
                            if (stringBuilder.substring(stringBuilder.length()-1).equals(" ")) {
                                stringBuilder.append("\n\n");
                            }
                            else if (!stringBuilder.substring(stringBuilder.length()-2).equals("\n")) {
                                stringBuilder.append(" ");
                            }
                        }
                    }

                    if (line.contains("</html>")) {
                        pages.add(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                Log.e("IOException", e.getMessage());
            }
        }
        return pages;
    }

    private void setPage() {
        if (pageCounter == firstPage) {
            bookTitle.setVisibility(View.VISIBLE);
            bookAuthor.setVisibility(View.VISIBLE);
        }
        else {
            bookTitle.setVisibility(View.GONE);
            bookAuthor.setVisibility(View.GONE);
        }
        bookText.setText(pages.get(pageCounter));
    }

    private void setPageZoomLevel() {
        bookTitle.setTextSize(24 + zoomLevel);
        bookAuthor.setTextSize(20 + zoomLevel);
        bookText.setTextSize(14 + zoomLevel);
    }
}
