package com.streamliners.galleryapp;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ChipColorBinding;
import com.streamliners.galleryapp.databinding.ChipLabelBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;
import com.streamliners.galleryapp.models.Item;

import java.util.List;
import java.util.Set;

public class EditImageDialog {
    private Context mContext;
    private EditImageDialog.OnCompleteListener mListener;
    private DialogAddImageBinding dialogBinding;
    private LayoutInflater inflater;

    private boolean isCustomLabel;
    AlertDialog alertDialog;
    private Bitmap image;

    /**
     * To show the image data in the dialog box
     * @param bitmap image
     * @param colors major colors in the image
     * @param labels labels of the image
     */
    public void showData(Context context, Bitmap bitmap, Set<Integer> colors, List<String> labels, OnCompleteListener listener) {
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
                .setView(dialogBinding.getRoot())
                .show();

        this.image = bitmap;
        // make the dimensions input and progress indicator gone and image contents visible
        dialogBinding.title.setText("Edit Image");
        dialogBinding.inputDimensionsRoot.setVisibility(View.GONE);
        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

        // set the image to the view
        dialogBinding.imageView.setImageBitmap(bitmap);

        inflateColorChips(colors);
        inflateLabelChips(labels);
        setupHideError();
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

                mListener.OnImageEdited(new Item(image, color, label));

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
        dialogBinding.customLabelInput.getEditText().addTextChangedListener(textWatcher);
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
     * callbacks for the dialog box completion
     */
    interface OnCompleteListener {
        void OnImageEdited(Item item);
        void OnError(String error);
    }
}
