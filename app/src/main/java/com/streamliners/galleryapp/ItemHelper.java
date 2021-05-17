package com.streamliners.galleryapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Helper class to fetch data from internet
 */
public class ItemHelper {
    // context of the main activity
    private Context mContext;
    // Listener to call the data is fetched
    private OnCompleteListener mListener;

    // url of the image
    private String mUrl;
    // image in bitmap format (We need bitmap image for our machine learning model for colors and labels)
    private Bitmap mBitmap;
    // set of major color in the image
    private Set<Integer> mColors;
    // list of label of the image
    private List<String> mLabels = new ArrayList<>();

    /**
     * To fetch image of the rectangular dimensions
     * @param context context of the activity
     * @param width width of the image
     * @param height height of the image
     * @param listener listener for the callbacks
     */
    public void fetchData(Context context, int width, int height, OnCompleteListener listener) {
        this.mListener = listener;
        this.mContext = context;

        // url for the rectangular image
        String rectangularImageUrl = "https://picsum.photos/%d/%d";

        // to fetch image with the given url
        fetchImage(String.format(rectangularImageUrl, width, height));
    }

    /**
     * To fetch image of the square dimensions
     * @param context context of the activity
     * @param side width and height of the image
     * @param listener listener for the callbacks
     */
    public void fetchData(Context context, int side, OnCompleteListener listener) {
        this.mListener = listener;
        this.mContext = context;

        // url for the square image
        String squareImageUrl = "https://picsum.photos/%d";

        // to fetch image with the given url
        fetchImage(String.format(squareImageUrl, side));
    }

    /**
     * To fetch image with the specified url
     * @param context context of the activity
     * @param url url of the image
     * @param listener listener for the callbacks
     */
    public void fetchData(Context context, String url, OnCompleteListener listener) {
        this.mListener = listener;
        this.mContext = context;
        this.mUrl = url;

        // to fetch image with the given url
        fetchImage(url);
    }

    /**
     * To fetch random image from the provided url
     * @param url url from which the image is to be fetched
     */
    private void fetchImage(String url) {
        // Fetching the redirected URL through new object
        new RedirectedURL().fetchRedirectedURL(new RedirectedURL.OnCompleteListener() {
            @Override
            public void onFetched(String redirectedUrl) {
                // setting the url
                mUrl = redirectedUrl;

                // fetching image using glide
                Glide.with(mContext)
                        .asBitmap()
                        .load(url)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                // set the bitmap image
                                mBitmap = bitmap;

                                // to extract colors from the image
                                extractPaletteFromBitmap();
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }

                            @Override
                            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                super.onLoadFailed(errorDrawable);

                                // callback for the error
                                mListener.onError("Image load failed");
                            }
                        });
            }
        }).execute(url);
    }

    /**
     * To extract palette from the bitmap
     */
    private void extractPaletteFromBitmap() {
        Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                // set the colors set with the filtered colors
                mColors = getColorFromPalette(palette);

                // to get labels for the image
                getLabelsFromImage();
            }
        });
    }

    /**
     * To get the labels from the given image
     */
    private void getLabelsFromImage() {
        // creating object
        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        // To use default options:
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        // to process the image with our model
        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(@NonNull List<ImageLabel> imageLabels) {
                        for (ImageLabel imageLabel : imageLabels) {
                            mLabels.add(imageLabel.getText());
                        }

                        // callback when all the data is fetched
                        mListener.onFetched(mUrl, mColors, mLabels);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // giving callback for the error
                        mListener.onError(e.toString());
                    }
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
    interface OnCompleteListener {
        /**
         * when the data is successfully fetched 
         * @param url url of the image
         * @param colors colors in the image
         * @param labels labels of the image
         */
        void onFetched(String url, Set<Integer> colors, List<String> labels);

        /**
         * when error occurred due to any specific reason
         * @param error error which is occurred
         */
        void onError(String error);
    }
}
