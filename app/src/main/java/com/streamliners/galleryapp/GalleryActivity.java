package com.streamliners.galleryapp;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    ActivityGalleryBinding mainBinding;
    List<Item> listOfItems = new ArrayList<>();
    SharedPreferences preferences;
    public boolean isDialogBoxShowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        preferences = getPreferences(MODE_PRIVATE);
        getDataFromSharedPreferences();
    }

    // Action menu methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_image) {
            showAddImageDialog();
            return true;
        }
        return false;
    }

    /**
     * To show the dialog to add image
     */
    private void showAddImageDialog() {
        // Check for the orientation
        if (this.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            isDialogBoxShowed = true;
            // To set the screen orientation in portrait mode only
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        new AddImageDialog()
                .show(this, new AddImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageAdded(Item item) {
                        isDialogBoxShowed = false;
                        listOfItems.add(item);
                        inflateViewForItem(item);
                    }

                    @Override
                    public void OnError(String error) {
                        isDialogBoxShowed = false;
                        new MaterialAlertDialogBuilder(GalleryActivity.this)
                                .setTitle("Error")
                                .setMessage(error)
                                .show();
                    }
                });
    }

    /**
     * To inflate the view for the incoming item
     * @param item item to be placed in the view
     */
    private void inflateViewForItem(Item item) {
        // inflate layout
        ItemCardBinding binding = ItemCardBinding.inflate(getLayoutInflater());

        // Binding the data
        binding.imageView.setImageBitmap(item.image);
        binding.labelView.setText(item.label);
        binding.labelView.setBackgroundColor(item.color);

        // add to the list
        mainBinding.list.addView(binding.getRoot());
    }

    /**
     * to restore the data using shared preferences
     */
    private void getDataFromSharedPreferences() {
        // Count of the items
        int countOfItems = preferences.getInt(Constants.COUNT_OF_ITEMS, 0);

        // To add all the items in the shared preferences
        for (int i = 1; i <= countOfItems; i++) {
            // make a new item
            Item item = new Item(getBitmapFromString(preferences.getString(Constants.ITEM_IMAGE + i, "")),
                    preferences.getInt(Constants.ITEM_COLOR + i, 0),
                    preferences.getString(Constants.ITEM_LABEL + i, ""));

            // Add the item in the list and inflate the item in the view
            listOfItems.add(item);
            inflateViewForItem(item);
        }

        // To set the dialog box status
        if (preferences.getBoolean(Constants.DIALOG_BOX_STATUS, false)) {
            showAddImageDialog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Count of the items in the list
        int countOfItems = listOfItems.size();
        preferences.edit()
                .putInt(Constants.COUNT_OF_ITEMS, countOfItems)
                .putBoolean(Constants.DIALOG_BOX_STATUS, isDialogBoxShowed)
                .apply();

        // Putting all the objects in the shared preferences
        int itemCount = 1;
        for (Item item : listOfItems) {
            preferences.edit()
                    .putInt(Constants.ITEM_COLOR + itemCount, item.color)
                    .putString(Constants.ITEM_LABEL + itemCount, item.label)
                    .putString(Constants.ITEM_IMAGE + itemCount, getStringFromBitmap(item.image))
                    .apply();

            // incrementing the index
            itemCount++;
        }
    }

    /**
     * converts Bitmap picture into string which can be
     * @param bitmapPicture bitmap image to be converted in string
     */
    private String getStringFromBitmap(Bitmap bitmapPicture) {
        final int COMPRESSION_QUALITY = 100;
        String encodedImage;
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY,
                byteArrayBitmapStream);
        byte[] b = byteArrayBitmapStream.toByteArray();
        encodedImage = Base64.encodeToString(b, Base64.DEFAULT);
        return encodedImage;
    }

    /**
     * converts the String into Bitmap image
     */
    private Bitmap getBitmapFromString(String stringPicture) {
        byte[] decodedString = Base64.decode(stringPicture, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
    }
}