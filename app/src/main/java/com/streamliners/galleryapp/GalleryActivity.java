package com.streamliners.galleryapp;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GalleryActivity extends AppCompatActivity {
    // Binding of the layout
    private ActivityGalleryBinding mainBinding;
    // List of the items
    private List<Item> listOfItems = new ArrayList<>();
    // Shared preferences
    private SharedPreferences preferences;

    // Dialog box is showed or not
    public boolean isDialogBoxShowed;
    // Selected item position in the list
    private int selectedItemPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the main binding
        mainBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        preferences = getPreferences(MODE_PRIVATE);
        getDataFromSharedPreferences();

        // Check whether the list is empty or not
        if (listOfItems.isEmpty()) {
            // Show the no items text view
            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
        }
    }

    // Menu methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    // Contextual menu methods

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Check the option selected
        if (item.getItemId() == R.id.add_image) {
            showAddImageDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            editItemInList(selectedItemPosition);
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            deleteItemFromList(selectedItemPosition);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // Actions methods

    /**
     * To edit the item from the list
     * @param position position defined of the item
     */
    private void editItemInList(int position) {
        // Get the item of the position
        Item item = listOfItems.get(position);

        // Fetching data using the helper class
        new ItemHelper()
                .fetchData(this, item.url, new ItemHelper.OnCompleteListener() {
                    @Override
                    public void onFetched(String url, Set<Integer> colors, List<String> labels) {
                        showEditImageDialog(position, url, colors, labels);
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
     * To delete the item from the list
     * @param position position defined of the item
     */
    private void deleteItemFromList(int position) {
        // Set the visibility of the card to GONE
        mainBinding.list.getChildAt(position).setVisibility(View.GONE);
        listOfItems.set(position, null);

        // Checking items in the list
        boolean isEmpty = true;
        for (Item item : listOfItems) {
            if (item != null) {
                isEmpty = false;
                break;
            }
        }

        // If the list is empty then set the no item text view to visible
        if (isEmpty) {
            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
        }

        // Showing the deleted toast
        Toast.makeText(this, "Item deleted from list", Toast.LENGTH_SHORT).show();
    }

    // Show dialog methods

    /**
     * To show the dialog to add image
     */
    private void showAddImageDialog() {
        // Check for the orientation
        if (this.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            // Change the dialog box appearance to true
            isDialogBoxShowed = true;
            // To set the screen orientation in portrait mode only
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        new AddImageDialog()
                .showDialog(this, new AddImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageAdded(Item item) {
                        // Set the dialog box appearance to false
                        isDialogBoxShowed = false;
                        // Adding the item in the list
                        listOfItems.add(item);
                        // Make the no item text view invisible
                        mainBinding.noItemTextView.setVisibility(View.GONE);
                        // Inflate the layout for the
                        inflateViewForItem(item, mainBinding.list.getChildCount());
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
     * To show dialog to edit the image
     */
    private void showEditImageDialog(int position, String url, Set<Integer> colors, List<String> labels) {
        new EditImageDialog()
                .showDialog(this, url, colors, labels, new EditImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageEdited(Item item) {
                        // Update the list and remove the card item from the layout
                        listOfItems.set(position, item);
                        mainBinding.list.removeViewAt(position);

                        // Inflate the view to the specified position
                        inflateViewForItem(item, position);
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

    // Utility methods

    /**
     * To inflate the view for the item to the specified position
     * @param item item to be placed in the view
     * @param position position of the item in the list
     */
    private void inflateViewForItem(Item item, int position) {
        // Inflate layout of the card
        ItemCardBinding binding = ItemCardBinding.inflate(getLayoutInflater());

        // Binding the data
        Glide.with(this)
                .load(item.url)
                .into(binding.imageView);
        binding.labelView.setText(item.label);
        binding.labelView.setBackgroundColor(item.color);

        // Register the view for the context menu add to the list
        registerViewForContextMenu(binding, position);

        // Add the card in the list
        mainBinding.list.addView(binding.getRoot(), position);
    }

    /**
     * To register the view for contextual menu
     * @param binding binding to be registered
     * @param position position of the binding
     */
    private void registerViewForContextMenu(ItemCardBinding binding, int position) {
        // Set the on long pressed listener to the image view
        binding.imageView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                // Inflate the menu for the view
                MenuInflater inflater = getMenuInflater();
                inflater.inflate(R.menu.contextual_menu, menu);

                // Set the selected position
                selectedItemPosition = position;
            }
        });
    }

    /**
     * To restore the data using shared preferences
     */
    private void getDataFromSharedPreferences() {
        // Count of the items
        int countOfItems = preferences.getInt(Constants.COUNT_OF_ITEMS, 0);

        // To add all the items in the shared preferences
        for (int i = 1; i <= countOfItems; i++) {
            // Make a new item
            Item item = getItemFromJson(preferences.getString(Constants.ITEM + i, ""));

            // Add the item in the list and inflate the item in the view
            listOfItems.add(item);
            inflateViewForItem(item, mainBinding.list.getChildCount());
        }

        // To set the dialog box status
        if (preferences.getBoolean(Constants.DIALOG_BOX_STATUS, false)) {
            showAddImageDialog();
        }
    }

    /**
     * To get the JSON for the item
     * @param item item to be converted into JSON
     * @return the json string
     */
    private String getJsonFromItem(Item item) {
        Gson json = new Gson();

        return json.toJson(item);
    }

    /**
     * To get the item from JSON
     * @param string JSON string of the item
     * @return converted item
     */
    private Item getItemFromJson(String string) {
        Gson json = new Gson();

        return json.fromJson(string, Item.class);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Putting all the objects in the shared preferences
        int itemCount = 0;
        for (Item item : listOfItems) {
            // Check for the item
            if (item != null) {
                // incrementing the index
                itemCount++;

                // Saving the item in the shared preferences
                preferences.edit()
                        .putString(Constants.ITEM + itemCount, getJsonFromItem(item))
                        .apply();
            }
        }
        preferences.edit()
                .putInt(Constants.COUNT_OF_ITEMS, itemCount)
                .putBoolean(Constants.DIALOG_BOX_STATUS, isDialogBoxShowed)
                .apply();
    }
}