package com.streamliners.galleryapp;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ChipColorBinding;
import com.streamliners.galleryapp.databinding.ChipLabelBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;
import com.streamliners.galleryapp.models.Item;

import java.util.List;
import java.util.Set;

public class EditImageDialog {
    // Context of the main activity
    private Context mContext;
    // Listener to call the image updating
    private EditImageDialog.OnCompleteListener mListener;
    // Binding of the dialog box
    private DialogAddImageBinding dialogBinding;
    // Inflater to inflate the layouts
    private LayoutInflater inflater;

    // Url of the image
    private String url;
    // To check whether the custom is set or not
    private boolean isCustomLabel;
    // Object of the alert dialog
    private AlertDialog alertDialog;

    // Showing the data methods

    /**
     * To show the image data in the dialog box
     * @param url url of the image in cache
     * @param colors major colors in the image
     * @param labels labels of the image
     */
    public void showDialog(Context context, String url, Set<Integer> colors, List<String> labels, OnCompleteListener listener) {
        this.mContext = context;
        this.mListener = listener;
        this.url = url;

        // Checking for the activity from its context and to inflate dialog's layout
        if (mContext instanceof GalleryActivity) {
            // Initialising the inflater
            inflater = ((GalleryActivity) mContext).getLayoutInflater();

            // Initialising the dialog binding
            dialogBinding = DialogAddImageBinding.inflate(inflater);
        } else {
            // Dismiss the dialog
            alertDialog.dismiss();

            // Call the listener for error
            mListener.OnError("Cast Exception");

            // And return the function
            return;
        }

        // Creating and showing the dialog box
        alertDialog = new MaterialAlertDialogBuilder(mContext, R.style.CustomDialogTheme)
                .setView(dialogBinding.getRoot())
                .show();

        // Make the dimensions input and progress indicator gone and image contents visible
        dialogBinding.title.setText("Edit Image");
        dialogBinding.inputDimensionsRoot.setVisibility(View.GONE);
        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

        // Get the image to the view as bitmap
        Glide.with(mContext)
                .asBitmap()
                .load(url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Set the image to the dialog
                        dialogBinding.imageView.setImageBitmap(resource);

                        // Inflating chips
                        inflateColorChips(colors);
                        inflateLabelChips(labels);

                        // Setup hiding error
                        setupHideError();

                        // Handling events
                        handleCustomLabelInput();
                        handleAddImageEvent();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    // Handling Events methods

    /**
     * To handle the situation when the image is added
     */
    private void handleAddImageEvent() {
        dialogBinding.buttonAddImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the chip selected IDs
                int colorChipId = dialogBinding.colorChips.getCheckedChipId();
                int labelChipId = dialogBinding.labelChips.getCheckedChipId();

                // Guard Code
                if (colorChipId == -1 || labelChipId == -1) {
                    Toast.makeText(mContext, "Please choose color or label", Toast.LENGTH_SHORT).show();
                    return;
                }

                // label of the image
                String label;

                // Checking for the custom label
                if (isCustomLabel) {
                    // Get the label
                    label = dialogBinding.customLabelInput.getEditText().getText().toString().trim();
                    if (label.isEmpty()) {
                        // Set the error to the text field
                        dialogBinding.customLabelInput.setError("Please enter custom label");
                        return;
                    }
                } else {
                    // Get label from the chip
                    label = ((Chip) dialogBinding.labelChips.findViewById(labelChipId)).getText().toString();
                }

                // Get color from the chip selected
                int color = ((Chip) dialogBinding.colorChips.findViewById(colorChipId)).
                        getChipBackgroundColor().getDefaultColor();

                // Callback when all the parameter are accepted
                mListener.OnImageEdited(new Item(url, color, label));

                // Dismiss the dialog box
                alertDialog.dismiss();
            }
        });
    }

    /**
     * To handle the custom label input
     */
    private void handleCustomLabelInput() {
        // Make chip for the custom label and add to the dialog box
        ChipLabelBinding binding = ChipLabelBinding.inflate(inflater);
        binding.getRoot().setText("Custom");
        dialogBinding.labelChips.addView(binding.getRoot());

        // Set the listener to the chip
        binding.getRoot().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Remove the error
                dialogBinding.customLabelInput.setError(null);
                // Set the custom label text field
                dialogBinding.customLabelInput.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                // Change the variable
                isCustomLabel = isChecked;
            }
        });
    }

    // Utility methods

    /**
     * To hide the error when text change of the width and height fields
     */
    private void setupHideError() {
        // Make an object to watch the text field
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

    // Inflating methods

    /**
     * To inflate the labels in the chips
     * @param labels labels of the image
     */
    private void inflateLabelChips(List<String> labels) {
        // inflate all the chips using loop
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
        // inflate all the chips using loop
        for (Integer color : colors) {
            ChipColorBinding binding = ChipColorBinding.inflate(inflater);
            binding.getRoot().setChipBackgroundColor(ColorStateList.valueOf(color));
            dialogBinding.colorChips.addView(binding.getRoot());
        }
    }

    /**
     * Callbacks for the dialog box completion
     */
    interface OnCompleteListener {
        /**
         * When image has to be added after successful editing in the list
         * @param item item of the image
         */
        void OnImageEdited(Item item);

        /**
         * When error occurs for the specified reasons
         * @param error error occurred
         */
        void OnError(String error);
    }
}
