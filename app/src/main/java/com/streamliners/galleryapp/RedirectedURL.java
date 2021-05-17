package com.streamliners.galleryapp;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents class for redirected URL(s)
 */
public class RedirectedURL extends AsyncTask<String, Void, String> {
    // Listener for the operation
    private OnCompleteListener mListener;

    /**
     * To fetch the redirected URL
     * @param listener listener for the callbacks
     * @return same object
     */
    public RedirectedURL fetchRedirectedURL(OnCompleteListener listener) {
        mListener = listener;
        return this;
    }

    // AsyncTask overridden methods

    @Override
    protected String doInBackground(String... strings) {
        return getRedirectUrl(strings[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        mListener.onFetched(s);
    }


    // Utility methods

    /**
     * To get the redirected URL for the specified URL
     * @param url specific URL for which the redirected URL is to be get
     * @return the redirected URL
     */
    private String getRedirectUrl(String url) {
        URL urlTmp = null;
        String redUrl;
        HttpURLConnection connection = null;

        try {
            urlTmp = new URL(url);
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }

        try {
            connection = (HttpURLConnection) urlTmp.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }

        redUrl = connection.getURL().toString();
        connection.disconnect();

        return redUrl;
    }

    /**
     * For the callback
     */
    interface OnCompleteListener {
        /**
         * when the redirected URL is fetched successfully
         * @param redirectedUrl the redirected url
         */
        void onFetched(String redirectedUrl);
    }
}
