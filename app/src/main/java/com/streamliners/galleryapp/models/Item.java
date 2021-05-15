package com.streamliners.galleryapp.models;

/**
 * Represents the item for the gallery activity
 */
public class Item {
    // Url of the image
    public String url;
    // Background color of the text field
    public int color;
    // Label of the image
    public String label;

    /**
     * To construct the object with...
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
