package com.streamliners.galleryapp.helpers;

import android.graphics.Bitmap;

import androidx.palette.graphics.Palette;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class to fetch data from the machine learning model
 */
public class MachineLearningModelHelper {
    // Listener for the process
    private OnCompleteListener mListener;

    // set of major color in the image
    private Set<Integer> mColors;
    // list of label of the image
    private final List<String> mLabels = new ArrayList<>();

    /**
     * To get the data
     * @param bitmap Image in bitmap format (We need bitmap image for our machine learning model for colors and labels)
     * @param listener listener to be called when work is done
     */
    public void getData(Bitmap bitmap, OnCompleteListener listener) {
        this.mListener = listener;

        extractPaletteFromBitmap(bitmap);
    }

    /**
     * To extract palette from the bitmap
     */
    private void extractPaletteFromBitmap(Bitmap bitmap) {
        Palette.from(bitmap).generate(palette -> {
            if (palette == null) {
                mListener.onError("Color palette is null");
                return;
            }

            // set the colors set with the filtered colors
            mColors = getColorFromPalette(palette);

            // to get labels for the image
            getLabelsFromImage(bitmap);
        });
    }

    /**
     * To get the labels from the given image
     */
    private void getLabelsFromImage(Bitmap bitmap) {
        // creating object
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        // To use default options:
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // to process the image with our model
        labeler.process(image)
                .addOnSuccessListener(imageLabels -> {
                    for (ImageLabel imageLabel : imageLabels) {
                        mLabels.add(imageLabel.getText());
                    }

                    // callback when all the data is fetched
                    mListener.onSuccess(mColors, mLabels);
                })
                .addOnFailureListener(e -> {
                    // giving callback for the error
                    mListener.onError(e.toString());
                });
    }

    /**
     * To get the colors from the palette
     * @param palette palette from which the colors have to be extracted
     * @return  set of all the colors extracted from the palette
     */
    private Set<Integer> getColorFromPalette(Palette palette) {
        Set<Integer> colors = new HashSet<>();

        // adding the vibrant colors and default color is black
        colors.add(palette.getVibrantColor(0));
        colors.add(palette.getLightVibrantColor(0));
        colors.add(palette.getDarkVibrantColor(0));

        // adding the muted colors and default color is black
        colors.add(palette.getMutedColor(0));
        colors.add(palette.getLightMutedColor(0));
        colors.add(palette.getDarkMutedColor(0));

        // removing the black color
        colors.remove(0);

        // returning the set of colors
        return colors;
    }

    /**
     * Interface for the callbacks when the requested data get the result
     */
    public interface OnCompleteListener {
        /**
         * when the data is successfully fetched
         * @param colors list of major colors in the image
         * @param labels list of labels of the image
         */
        void onSuccess(Set<Integer> colors, List<String> labels);

        /**
         * when error occurred due to any specific reason
         * @param error error which is occurred
         */
        void onError(String error);
    }
}
