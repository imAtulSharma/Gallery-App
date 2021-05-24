package com.streamliners.galleryapp;

import android.app.Application;
import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Represents application class
 */
public class MyApp extends Application {
    // For loading dialog
    private AlertDialog alertDialog;

    /**
     * To show the loading dialog
     * @param context context of the activity
     */
    public void showLoadingDialog(Context context) {
        alertDialog = new MaterialAlertDialogBuilder(context)
                .setView(R.layout.dialog_loading)
                .setCancelable(false)
                .show();
    }

    /**
     * To dismiss the loading dialog
     */
    public void hideLoadingDialog() {
        alertDialog.dismiss();
    }
}
