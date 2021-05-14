package com.streamliners.galleryapp.models;

import android.graphics.Bitmap;

/**
 * Represents the item for the gallery activity
 */
public class Item {

    public String url;
    public int color;
    public String label;

    /**
     * To construct the object
     * @param url url of the image received
     * @param color color chose
     * @param label label chose
     */
    public Item(String url, int color, String label) {
        this.url = url;
        this.color = color;
        this.label = label;
    }
}
