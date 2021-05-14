package com.streamliners.galleryapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ChipColorBinding;
import com.streamliners.galleryapp.databinding.ChipLabelBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;
import com.streamliners.galleryapp.models.Item;

import java.util.List;
import java.util.Set;

public class AddImageDialog implements ItemHelper.OnCompleteListener {
    private Context mContext;
    private OnCompleteListener mListener;
    private DialogAddImageBinding dialogBinding;
    private LayoutInflater inflater;

    private String url;

    private boolean isCustomLabel;
    AlertDialog alertDialog;

    /**
     * To inflate dialog's layout
     * @param context context of the main activity
     */
    public void show(Context context, OnCompleteListener listener) {
        this.mContext = context;
        this.mListener = listener;

        // Checking for the activity from its context and to inflate dialog's layout
        if (mContext instanceof GalleryActivity) {
            inflater = ((GalleryActivity) mContext).getLayoutInflater();
            dialogBinding = DialogAddImageBinding.inflate(inflater);
        } else {
            alertDialog.dismiss();
            mListener.OnError("Cast Exception");
            return;
        }

        // creating and showing the dialog box
        alertDialog = new MaterialAlertDialogBuilder(mContext, R.style.CustomDialogTheme)
                .setCancelable(false)
                .setView(dialogBinding.getRoot())
                .show();

        // Handle events
        handleDimensionsInput();
    }

    /**
     * To handle the dimensions work
     */
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

                // Guard Code
                // showing error(s) if there is no dimensions input
                if (width.isEmpty() && height.isEmpty()) {
                    dialogBinding.widthTextView.setError("Enter at least on parameter");
                    return;
                }

                // Hiding keyboard
                hideKeyboard();

                // make the input dialog gone and progress indicator visible
                dialogBinding.inputDimensionsRoot.setVisibility(View.GONE);
                dialogBinding.progressIndicatorRoot.setVisibility(View.VISIBLE);

                // For square image
                if (width.isEmpty()) {
                    fetchRandomImage(Integer.parseInt(height));
                } else if (height.isEmpty()) {
                    fetchRandomImage(Integer.parseInt(width));
                }
                // For Rectangular image
                else{
                    fetchRandomImage(Integer.parseInt(width), Integer.parseInt(height));
                }
            }
        });
    }

    /**
     * To hide the keyboard after taking input
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(dialogBinding.title.getWindowToken(), 0);
    }

    /**
     * To fetch any rectangle image from internet
     * @param width width of the image
     * @param height height of the image
     */
    private void fetchRandomImage(int width, int height) {
        new ItemHelper()
                .fetchData(mContext, width, height, this);
    }

    /**
     * To fetch any square image
     * @param side
     */
    private void fetchRandomImage(int side) {
        new ItemHelper()
                .fetchData(mContext, side, this);
    }

    /**
     * To show the image data in the dialog box
     * @param url url of the image in the cache
     * @param colors major colors in the image
     * @param labels labels of the image
     */
    private void showData(String url, Set<Integer> colors, List<String> labels) {
        // set the url of the image
        this.url = url;

        // make the progress indicator gone and image contents visible
        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

        // set the image to the view
        Glide.with(mContext)
                .load(url)
                .into(dialogBinding.imageView);

        inflateColorChips(colors);
        inflateLabelChips(labels);
        handleCustomLabelInput();
        handleAddImageEvent();
    }

    /**
     * To handle the situation when the image is added
     */
    private void handleAddImageEvent() {
        dialogBinding.buttonAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int colorChipId = dialogBinding.colorChips.getCheckedChipId();
                int labelChipId = dialogBinding.labelChips.getCheckedChipId();

                // Guard Code
                if (colorChipId == -1 || labelChipId == -1) {
                    Toast.makeText(mContext, "Please choose color or label", Toast.LENGTH_SHORT).show();
                    return;
                }

                String label;

                if (isCustomLabel) {
                    label = dialogBinding.customLabelInput.getEditText().getText().toString().trim();
                    if (label.isEmpty()) {
                        dialogBinding.customLabelInput.setError("Please enter custom label");
                        return;
                    }
                } else {
                    // Get label
                    label = ((Chip) dialogBinding.labelChips.findViewById(labelChipId)).getText().toString();
                }

                // Get color
                int color = ((Chip) dialogBinding.colorChips.findViewById(colorChipId)).
                        getChipBackgroundColor().getDefaultColor();

                mListener.OnImageAdded(new Item(url, color, label));

                alertDialog.dismiss();

                // To set the screen orientation according to the sensor
                ((GalleryActivity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
            }
        });
    }

    /**
     * To handle the custom label input
     */
    private void handleCustomLabelInput() {
        ChipLabelBinding binding = ChipLabelBinding.inflate(inflater);
        binding.getRoot().setText("Custom");
        dialogBinding.labelChips.addView(binding.getRoot());

        binding.getRoot().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                dialogBinding.customLabelInput.setError(null);
                dialogBinding.customLabelInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                isCustomLabel = isChecked;
            }
        });
    }

    /**
     * To inflate the labels in the chips
     * @param labels labels of the image
     */
    private void inflateLabelChips(List<String> labels) {
        for (String label : labels) {
            ChipLabelBinding binding = ChipLabelBinding.inflate(inflater);
            binding.getRoot().setText(label);
            dialogBinding.labelChips.addView(binding.getRoot());
        }
    }

    /**
     * To inflate the colors in the chips
     * @param colors major colors of the image
     */
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
                dialogBinding.customLabelInput.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // add the text watcher to the text fields
        dialogBinding.widthTextView.getEditText().addTextChangedListener(textWatcher);
        dialogBinding.heightTextView.getEditText().addTextChangedListener(textWatcher);
        dialogBinding.customLabelInput.getEditText().addTextChangedListener(textWatcher);
    }

    @Override
    public void onFetched(String url, Set<Integer> colors, List<String> labels) {
        showData(url, colors, labels);
    }

    @Override
    public void onError(String error) {
        // To show the error and hide the loader
        dialogBinding.linearProgressIndicator.setVisibility(View.GONE);
        dialogBinding.progressSubtitle.setText(error);

        // To make the dialog box cancelable
        alertDialog.setCancelable(true);

        ((GalleryActivity) mContext).isDialogBoxShowed = false;

        // To set the screen orientation according to the sensor
        ((GalleryActivity) mContext).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }

    /**
     * callbacks for the dialog box completion
     */
    interface OnCompleteListener {
        void OnImageAdded(Item item);
        void OnError(String error);
    }
}
