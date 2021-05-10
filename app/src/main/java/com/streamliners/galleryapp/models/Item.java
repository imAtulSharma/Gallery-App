package com.streamliners.galleryapp.models;

import android.graphics.Bitmap;

/**
 * Represents the item for the gallery activity
 */
public class Item {

    public Bitmap image;
    public int color;
    public String label;

    /**
     * To construct the object
     * @param image image received
     * @param color color chose
     * @param label label chose
     */
    public Item(Bitmap image, int color, String label) {
        this.image = image;
        this.color = color;
        this.label = label;
    }

    /**
     * Empty constructor
     */
    public Item() {
    }
}
