package com.streamliners.galleryapp;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
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

/**
 * Represents the class to show a add image dialog
 */
public class AddImageDialog implements ItemHelper.OnCompleteListener {
    // Context of the main activity
    private Context mContext;
    // Listener to call for image addition
    private OnCompleteListener mListener;
    // Binding of the dialog view
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
     * To inflate dialog's layout and then show the dialog inflated
     * @param context context of the main activity
     * @param listener listener for the callbacks
     */
    public void showDialog(Context context, OnCompleteListener listener) {
        this.mContext = context;
        this.mListener = listener;

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

        // Creating and showing the dialog box and set to non cancellable
        alertDialog = new MaterialAlertDialogBuilder(mContext, R.style.CustomDialogTheme)
                .setCancelable(false)
                .setView(dialogBinding.getRoot())
                .show();

        // To handle events
        handleDimensionsInput();
    }

    /**
     * To inflate dialog's layout for editing purpose and then show the dialog inflated
     * @param url url of the image in the cache
     * @param colors major colors in the image
     * @param labels labels of the image
     */
    private void showData(String url, Set<Integer> colors, List<String> labels) {
        // Set the url of the image
        this.url = url;

        // Make the progress indicator gone and image contents visible
        dialogBinding.progressIndicatorRoot.setVisibility(View.GONE);
        dialogBinding.addImageRoot.setVisibility(View.VISIBLE);

        // Set the image to the image view in binding
        Glide.with(mContext)
                .load(url)
                .into(dialogBinding.imageView);

        // Inflating all the other stuffs in the binding
        inflateColorChips(colors);
        inflateLabelChips(labels);
        
        // Handling events
        handleCustomLabelInput();
        handleAddImageEvent();
    }
    
    // Handling Events methods
    
    /**
     * To handle the dimensions work
     */
    private void handleDimensionsInput() {
        // Listener to the fetch image button
        dialogBinding.buttonFetchImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Setup the error hider for the text fields
                setupHideError();

                // getting width and height from the text fields
                String width = dialogBinding.widthTextView.getEditText().getText().toString().trim();
                String height = dialogBinding.heightTextView.getEditText().getText().toString().trim();

                // Guard Code
                // showing error(s) if there is no dimensions input
                if (width.isEmpty() && height.isEmpty()) {
                    // set the error to the text field
                    dialogBinding.widthTextView.setError("Enter at least on parameter");
                    return;
                }

                // Hiding keyboard
                hideKeyboard();

                // make the input dialog gone and progress indicator visible
                dialogBinding.inputDimensionsRoot.setVisibility(View.GONE);
                dialogBinding.progressIndicatorRoot.setVisibility(View.VISIBLE);

                // To fetch square image
                if (width.isEmpty()) {
                    fetchRandomImage(Integer.parseInt(height));
                } else if (height.isEmpty()) {
                    fetchRandomImage(Integer.parseInt(width));
                }
                // To fetch Rectangular image
                else{
                    fetchRandomImage(Integer.parseInt(width), Integer.parseInt(height));
                }
            }
        });
    }

    /**
     * To handle the situation when the image is added
     */
    private void handleAddImageEvent() {
        // Set the listener for the button
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
                mListener.OnImageAdded(new Item(url, color, label));

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

    // Image fetching methods
    
    /**
     * To fetch any rectangle image
     * @param width width of the image
     * @param height height of the image
     */
    private void fetchRandomImage(int width, int height) {
        new ItemHelper()
                .fetchData(mContext, width, height, this);
    }

    /**
     * To fetch any square image
     * @param side side of the square image
     */
    private void fetchRandomImage(int side) {
        new ItemHelper()
                .fetchData(mContext, side, this);
    }

    // Utility methods
    
    /**
     * To hide the keyboard after taking input
     */
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(dialogBinding.title.getWindowToken(), 0);
    }

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

    // Implementing methods of callbacks for image fetching

    @Override
    public void onFetched(String url, Set<Integer> colors, List<String> labels) {
        // To show the dialog box with the data
        showData(url, colors, labels);
    }

    @Override
    public void onError(String error) {
        // Callback when all the parameter are accepted
        mListener.OnError(error);

        // Dismiss the dialog box
        alertDialog.dismiss();
    }

    /**
     * Callbacks for the dialog box completion
     */
    interface OnCompleteListener {
        /**
         * When image has to be added in the list successfully
         * @param item item of the image
         */
        void OnImageAdded(Item item);

        /**
         * When error occurs for the specified reasons
         * @param error error occurred
         */
        void OnError(String error);
    }
}
