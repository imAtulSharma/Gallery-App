package com.streamliners.galleryapp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ChipColorBinding;
import com.streamliners.galleryapp.databinding.ChipLabelBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;

import java.util.List;
import java.util.Set;

public class AddImageDialog {
    private Context mContext;
    private DialogAddImageBinding dialogBinding;
    private LayoutInflater inflater;

    /**
     * To inflate dialog's layout
     * @param context context of the main activity
     */
    public void show(Context context) {
        this.mContext = context;

        // Checking for the activity from its context and to inflate dialog's layout
        if (mContext instanceof MainActivity) {
            inflater = ((MainActivity) mContext).getLayoutInflater();
            dialogBinding = DialogAddImageBinding.inflate(inflater);
        } else {
            //TODO: handle error
            return;
        }

        // creating and showing the dialog box
        new MaterialAlertDialogBuilder(mContext, R.style.CustomDialogTheme)
                .setView(dialogBinding.getRoot())
                .show();

        // Handle events
        handleDimensionsInput();
    }

    private void handleDimensionsInput() {
        // listener to the fetch image button
        dialogBinding.buttonFetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // setup the error hider for the text fields
                setupHideError();

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

                // make the input dialog gone and progress indicator visible
                dialogBinding.inputDimesionsRoot.setVisibility(View.GONE);
                dialogBinding.progressIndicatorRoot.setVisibility(View.VISIBLE);

                // fetching image with the given width and height
                fetchRandomImage(Integer.parseInt(width), Integer.parseInt(height));
            }
        });
    }

    private void fetchRandomImage(int width, int height) {
        new ItemHelper()
                .fetchData(mContext, width, height, new ItemHelper.OnCompleteListener() {
                    @Override
                    public void onFetched(Bitmap bitmap, Set<Integer> colors, List<String> labels) {
                        showData(bitmap, colors, labels);
                    }

                    @Override
                    public void onError(String error) {
                        dialogBinding.linearProgressIndicator.setVisibility(View.GONE);
                        dialogBinding.progressSubtitle.setText(error);
                    }
                });
    }

    private void showData(Bitmap bitmap, Set<Integer> colors, List<String> labels) {
        // make the progress indicator gone and image contents visible
        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

        // set the image to the view
        dialogBinding.imageView.setImageBitmap(bitmap);

        inflateColorChips(colors);
        inflateLabelChips(labels);
    }

    private void inflateLabelChips(List<String> labels) {
        for (String label : labels) {
            ChipLabelBinding binding = ChipLabelBinding.inflate(inflater);
            binding.getRoot().setText(label);
            dialogBinding.labelChips.addView(binding.getRoot());
        }
    }

    private void inflateColorChips(Set<Integer> colors) {
        for (Integer color : colors) {
            ChipColorBinding binding = ChipColorBinding.inflate(inflater);
            binding.getRoot().setChipBackgroundColor(ColorStateList.valueOf(color));
            dialogBinding.colorChips.addView(binding.getRoot());
        }
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
