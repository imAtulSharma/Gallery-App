package com.streamliners.galleryapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ActivityDemoBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;

import java.util.List;
import java.util.Set;

public class DemoActivity extends AppCompatActivity {
    ActivityDemoBinding mainBinding;
    DialogAddImageBinding dialogBinding;
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainBinding = ActivityDemoBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        setTitle("Demo");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_image) {
            addImage();
            return true;
        }
        return false;
    }

    /**
     * To add the image in the list (main activity)
     */
    private void addImage() {
        // defining the dialog binding
        dialogBinding = DialogAddImageBinding.inflate(getLayoutInflater());

        // To show the dialog
        dialog = new MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setView(dialogBinding.getRoot())
                .show();

        // setup the error hider for the text fields
        setupHideError();

        // listener to the fetch image button
        dialogBinding.buttonFetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // getting width and height from the text fields
                String width = dialogBinding.widthTextView.getEditText().getText().toString().trim();
                String height = dialogBinding.heightTextView.getEditText().getText().toString().trim();

                // showing error(s) if there is error
                if (width.isEmpty()) {
                    dialogBinding.widthTextView.setError("Enter width");
                    return;
                } else if (height.isEmpty()) {
                    dialogBinding.heightTextView.setError("Enter height");
                    return;
                }

                // fetching image with the given width and height
                fetchImage(Integer.parseInt(width), Integer.parseInt(height));
            }
        });
    }

    /**
     * To fetch image
     * @param width width of the image
     * @param height height of the image
     */
    private void fetchImage(int width, int height) {
        // make the input dialog gone and progress indicator visible
        dialogBinding.inputDimesionsRoot.setVisibility(View.GONE);
        dialogBinding.progressIndicatorRoot.setVisibility(View.VISIBLE);

        // loading the image from the given url
        loadImage(String.format("https://picsum.photos/%s/%s", width, height));
    }

    private void loadImage(String url) {
        Glide.with(this)
                .asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .load(url)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        dialogBinding.linearProgressIndicator.setVisibility(View.GONE);
                        dialogBinding.progressSubtitle.setText(e.toString());

                        return true;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                        // hiding the loader and showing the image
                        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
                        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

                        dialogBinding.imageView.setImageBitmap(resource);

                        return true;
                    }
                })
//                .into(mainBinding.imageView);
                .into(dialogBinding.imageView);
//                .into(new CustomTarget<Bitmap>() {
//                    @Override
//                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
//                        // hiding the loader and showing the image
//                        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
//                        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);
//
//                        dialogBinding.imageView.setImageBitmap(resource);
//                    }
//
//                    @Override
//                    public void onLoadCleared(@Nullable Drawable placeholder) {
//
//                    }
//                });
    }

    /**
     * To hide the error when text change of the width and height fields
     */
    private void setupHideError() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialogBinding.widthTextView.setError(null);
                dialogBinding.heightTextView.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // add the text watcher to the text fields
        dialogBinding.widthTextView.getEditText().addTextChangedListener(textWatcher);
        dialogBinding.heightTextView.getEditText().addTextChangedListener(textWatcher);
    }
}