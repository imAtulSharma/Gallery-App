package com.streamliners.galleryapp;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GalleryActivity extends AppCompatActivity {
    ActivityGalleryBinding mainBinding;
    List<Item> listOfItems = new ArrayList<>();
    SharedPreferences preferences;
    public boolean isDialogBoxShowed;
    private int selectedItemPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        preferences = getPreferences(MODE_PRIVATE);
        getDataFromSharedPreferences();

        // check whether the list is empty or not
        if (listOfItems.isEmpty()) {
            // show the no items text view
            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
        }
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

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        // check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            editItemInList(selectedItemPosition);
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            deleteItemFromList(selectedItemPosition);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * To edit the item from the list
     * @param position position defined of the item
     */
    private void editItemInList(int position) {
//        int index = listOfItems.size() - initialNumberOfItemsInList + position;
//
//        Item item = listOfItems.get(index);

        Item item = listOfItems.get(position);
        new ItemHelper()
                .fetchData(this, item.image, new ItemHelper.OnCompleteListener() {
                    @Override
                    public void onFetched(Bitmap bitmap, Set<Integer> colors, List<String> labels) {
                        showEditItemDialog(position, bitmap, colors, labels);
                    }

                    @Override
                    public void onError(String error) {
                        new MaterialAlertDialogBuilder(GalleryActivity.this)
                                .setTitle("Error")
                                .setMessage(error)
                                .show();
                    }
                });
    }

    /**
     * To show dialog to edit the image
     */
    private void showEditItemDialog(int position, Bitmap bitmap, Set<Integer> colors, List<String> labels) {
        new EditImageDialog()
                .showData(this, bitmap, colors, labels, new EditImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageEdited(Item item) {
                        // Update the list and remove the card item from the layout
                        listOfItems.set(position, item);
                        mainBinding.list.removeViewAt(position);


                        // inflate layout
                        ItemCardBinding binding = ItemCardBinding.inflate(getLayoutInflater());

                        // Binding the data
                        binding.imageView.setImageBitmap(item.image);
                        binding.labelView.setText(item.label);
                        binding.labelView.setBackgroundColor(item.color);

                        // register the view for the context menu add to the list
                        registerViewForContextMenu(binding, position);

                        // Adding the card item to the previous position again
                        mainBinding.list.addView(binding.getRoot(), position);
                    }

                    @Override
                    public void OnError(String error) {
                        new MaterialAlertDialogBuilder(GalleryActivity.this)
                                .setTitle("Error")
                                .setMessage(error)
                                .show();
                    }
                });
    }

    /**
     * To delete the item from the list
     * @param position position defined of the item
     */
    private void deleteItemFromList(int position) {
//        // current size of the list
//        int noOfItemsInList = listOfItems.size();
//
//        int index = noOfItemsInList - initialNumberOfItemsInList + position;
//
//        // remove the item from both lists
//        listOfItems.remove(index);
//        mainBinding.list.removeViewAt(index);
//
//        // if the list is empty then set the no item text view
//        if (listOfItems.isEmpty()) {
//            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
//        }

        mainBinding.list.getChildAt(position).setVisibility(View.GONE);
        listOfItems.set(position, null);

        Toast.makeText(this, "Item deleted from list", Toast.LENGTH_SHORT).show();
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
                        mainBinding.noItemTextView.setVisibility(View.GONE);
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

        // register the view for the context menu add to the list

        registerViewForContextMenu(binding, mainBinding.list.getChildCount());

        mainBinding.list.addView(binding.getRoot());
    }

    /**
     * To register the view for contextual menu
     * @param binding binding to be registered
     * @param position position of the binding
     */
    private void registerViewForContextMenu(ItemCardBinding binding, int position) {
        binding.imageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.contextual_menu, menu);

                selectedItemPosition = position;
            }
        });
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

        // Putting all the objects in the shared preferences
        int itemCount = 0;
        for (Item item : listOfItems) {
            if (item != null) {
                // incrementing the index
                itemCount++;

                // Saving the item in the shared preferences
                preferences.edit()
                        .putInt(Constants.ITEM_COLOR + itemCount, item.color)
                        .putString(Constants.ITEM_LABEL + itemCount, item.label)
                        .putString(Constants.ITEM_IMAGE + itemCount, getStringFromBitmap(item.image))
                        .apply();
            }
        }
        preferences.edit()
                .putInt(Constants.COUNT_OF_ITEMS, itemCount)
                .putBoolean(Constants.DIALOG_BOX_STATUS, isDialogBoxShowed)
                .apply();
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