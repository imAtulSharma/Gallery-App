package com.streamliners.galleryapp;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
 * Helper class to fetch data
 */
public class ItemHelper {
    private OnCompleteListener mListener;
    private Context mContext;

    private Bitmap mBitmap;
    private Set<Integer> mColors;
    private List<String> mLabels = new ArrayList<>();

    private String rectangularImageUrl = "https://picsum.photos/%d/%d",
            squareImageUrl = "https://picsum.photos/%d";

    /**
     * To fetch data for the rectangular image
     * @param context context of the activity
     * @param x width of the image
     * @param y height of the image
     * @param listener listener for the call backs
     */
    public void fetchData(Context context, int x, int y, OnCompleteListener listener) {
        this.mListener = listener;
        this.mContext = context;

        fetchImage(String.format(rectangularImageUrl, x, y));
    }

    /**
     * To fetch data for the square image
     * @param context context of the activity
     * @param x width and height of the image
     * @param listener listener for the call backs
     */
    public void fetchData(Context context, int x, OnCompleteListener listener) {
        this.mListener = listener;
        this.mContext = context;

        fetchImage(String.format(squareImageUrl, x));
    }

    /**
     * To fetch random image from the internet
     * @param url url from which the image is to be fetched
     */
    private void fetchImage(String url) {
        Glide.with(mContext)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                        mBitmap = bitmap;
                        extractPaletteFromBitmap();
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);

                        mListener.onError("Image load failed");
                    }
                });
    }

    /**
     * To extract palette from the bitmap
     */
    private void extractPaletteFromBitmap() {
        Palette.from(mBitmap).generate(new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette palette) {
                mColors = getColorFromPalette(palette);

                getLabelsFromImage();
            }
        });
    }

    /**
     * To get the labels from the given image
     */
    private void getLabelsFromImage() {
        InputImage image = InputImage.fromBitmap(mBitmap, 0);

        // To use default options:
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(@NonNull List<ImageLabel> imageLabels) {
                        for (ImageLabel imageLabel : imageLabels) {
                            mLabels.add(imageLabel.getText());
                        }

                        mListener.onFetched(mBitmap, mColors, mLabels);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
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

        colors.add(palette.getVibrantColor(0));
        colors.add(palette.getLightVibrantColor(0));
        colors.add(palette.getDarkVibrantColor(0));

        colors.add(palette.getMutedColor(0));
        colors.add(palette.getLightMutedColor(0));
        colors.add(palette.getDarkMutedColor(0));

        colors.remove(0);

        return colors;
    }

    /**
     * Interface for the call backs when the requested data get the result
     */
    interface OnCompleteListener {
        void onFetched(Bitmap bitmap, Set<Integer> colors, List<String> labels);
        void onError(String error);
    }
}
