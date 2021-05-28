package com.streamliners.galleryapp.helpers;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents class for redirected URL(s)
 */
public class RedirectedUrlHelper extends AsyncTask<String, Void, String> {
    // Listener for the process
    private OnCompleteListener mListener;

    /**
     * To fetch the redirected URL
     * @param listener listener for the callbacks
     * @return same object
     */
    public RedirectedUrlHelper fetchRedirectedURL(OnCompleteListener listener) {
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
        URL urlTmp;
        String redUrl;
        HttpURLConnection connection = null;

        try {
            urlTmp = new URL(url);
        } catch (MalformedURLException malformedURLException) {
            return url;
        }

        try {
            connection = (HttpURLConnection) urlTmp.openConnection();
            // To unfollow the redirection link
            connection.setInstanceFollowRedirects(false);
        } catch (IOException ioException) {
            mListener.onError(ioException.toString());
        }
        try {
            assert connection != null;
            connection.getResponseCode();
        } catch (IOException ioException) {
            mListener.onError(ioException.toString());
        }

        redUrl = connection.getHeaderField(6);
        connection.disconnect();

        return redUrl;
    }

    /**
     * For the callback
     */
    public interface OnCompleteListener {
        /**
         * when the redirected URL is fetched successfully
         * @param redirectedUrl the redirected url
         */
        void onFetched(String redirectedUrl);

        /**
         * When error occurred
         */
        void onError(String error);
    }
}
