package com.streamliners.galleryapp.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Helper class to fetch data for an item
 */
public class ItemHelper {
    // context of the main activity
    private Context mContext;
    // Listener to call the data is fetched
    private OnCompleteListener mListener;

    // url of the image
    private String mUrl;

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
        fetchImage(String.format(Locale.getDefault(), rectangularImageUrl, width, height));
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
        fetchImage(String.format(Locale.getDefault(), squareImageUrl, side));
    }

    /**
     * To fetch random image from the provided url
     * @param url url from which the image is to be fetched
     */
    private void fetchImage(String url) {
        // Fetching the redirected URL through new object
        new RedirectedUrlHelper()
                .fetchRedirectedURL(new RedirectedUrlHelper.OnCompleteListener() {
            @Override
            public void onFetched(String redirectedUrl) {
                // setting the url
                mUrl = redirectedUrl;

                // fetching image using glide
                Glide.with(mContext)
                        .asBitmap()
                        .load(mUrl)
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                                new MachineLearningModelHelper()
                                        .getData(bitmap, new MachineLearningModelHelper.OnCompleteListener() {
                                            @Override
                                            public void onSuccess(Set<Integer> colors, List<String> labels) {
                                                mListener.onSuccess(mUrl, colors, labels);
                                            }

                                            @Override
                                            public void onError(String error) {
                                                mListener.onError(error);
                                            }
                                        });
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

            @Override
            public void onError(String error) {
                mListener.onError(error);
            }
        }).execute(url);
    }

    /**
     * Interface for the callbacks when the requested data get the result
     */
    public interface OnCompleteListener {
        /**
         * when the data is successfully fetched 
         * @param url url of the image
         * @param colors colors in the image
         * @param labels labels of the image
         */
        void onSuccess(String url, Set<Integer> colors, List<String> labels);

        /**
         * when error occurred due to any specific reason
         * @param error error which is occurred
         */
        void onError(String error);
    }
}
