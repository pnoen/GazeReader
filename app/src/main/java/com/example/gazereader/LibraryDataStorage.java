package com.example.gazereader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LibraryDataStorage {
    private static final String TAG = LibraryDataStorage.class.getSimpleName();
    private static final String LIBRARY_DATA = "libraryData";
    private List<String> books = new ArrayList<>();
    static private LibraryDataStorage mInstance = null;
    private final WeakReference<Context> mContext;

    static public LibraryDataStorage makeNewInstance(Context context) {
        mInstance = new LibraryDataStorage(context);
        return mInstance;
    }

    static public LibraryDataStorage getInstance() {
        return mInstance;
    }

    public LibraryDataStorage(Context context) {
        this.mContext = new WeakReference<>(context);
        createKey();
    }

    public void createKey() {
        books.add("a_room_with_a_view.epub");
        books.add("adventures_of_roderick.epub");
        books.add("blue_castle.epub");
        books.add("brothers_karamazov.epub");
        books.add("enchanted_april.epub");
        books.add("middlemarch.epub");
        books.add("picture_of_dorian_gray.epub");
        books.add("romeo_and_juliet.epub");
    }

    public void saveBookData(String book, int[] bookData) {
        int bookInd = books.indexOf(book);
        if (bookData != null && bookData.length > 0 && bookInd != -1) {
            SharedPreferences.Editor editor = mContext.get().getSharedPreferences(TAG, Context.MODE_PRIVATE).edit();
            Log.i("LIBRARY DATA", LIBRARY_DATA + bookInd + " " + Arrays.toString(bookData));
            editor.putString(LIBRARY_DATA + bookInd, Arrays.toString(bookData));
            editor.apply();
        } else {
            Log.e(TAG, "Abnormal book data");
        }
    }

    public int[] loadCalibrationData(String book) {
        int bookInd = books.indexOf(book);
        if (bookInd == -1) {
            return null;
        }

        SharedPreferences prefs = mContext.get().getSharedPreferences(TAG, Context.MODE_PRIVATE);
        String saveData = prefs.getString(LIBRARY_DATA + bookInd, null);

        if (saveData != null) {
            try {
                String[] split = saveData.substring(1, saveData.length() - 1).split(", ");
                int[] array = new int[split.length];
                for (int i = 0; i < split.length; i++) {
                    array[i] = Integer.parseInt(split[i]);
                }
                return array;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Maybe unmatched type of book data");
            }
        }
        return null;
    }
}
