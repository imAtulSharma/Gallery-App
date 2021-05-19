package com.streamliners.galleryapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GalleryActivity extends AppCompatActivity {
    // Request code for fetch image from gallery
    private static final int RC_PHOTO_PICKER = 1;
    // Request code for fetch image from camera
    private static final int RC_PHOTO_CAPTURE = 2;
    // Request code for the permission
    private static final int PERMISSION_CODE = 1000;

    // For the image clicked through camera
    private Uri imageUri;
    // For Floating Action Buttons
    private boolean flag = true;

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

        // Setup FABs
        setupFab();

        // To set the dialog box status
        if(savedInstanceState != null) {
            if (savedInstanceState.getBoolean(Constants.DIALOG_BOX_STATUS, false)) {
                addImageFromNetwork();
            }
        }

        preferences = getPreferences(MODE_PRIVATE);
        getDataFromSharedPreferences();

        // Check whether the list is empty or not
        if (listOfItems.isEmpty()) {
            // Show the no items text view
            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Checking the result status
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            // Get URI from the intent
            Uri selectedImageUri = data.getData();

            // Fetching data using the helper class
            new ItemHelper()
                    .fetchData(this, selectedImageUri.toString(), new ItemHelper.OnCompleteListener() {
                        @Override
                        public void onFetched(String url, Set<Integer> colors, List<String> labels) {
                            // To show the dialog
                            showEditImageDialog(mainBinding.list.getChildCount(), url, colors, labels);
                        }

                        @Override
                        public void onError(String error) {
                            new MaterialAlertDialogBuilder(GalleryActivity.this)
                                    .setTitle("Error")
                                    .setMessage(error)
                                    .show();
                        }
                    });
        } else if (requestCode == RC_PHOTO_CAPTURE && resultCode == RESULT_OK) {
            // Fetching data using the helper class
            new ItemHelper()
                    .fetchData(this, imageUri.toString(), new ItemHelper.OnCompleteListener() {
                        @Override
                        public void onFetched(String url, Set<Integer> colors, List<String> labels) {
                            // To show the dialog
                            showEditImageDialog(mainBinding.list.getChildCount(), url, colors, labels);
                        }

                        @Override
                        public void onError(String error) {
                            new MaterialAlertDialogBuilder(GalleryActivity.this)
                                    .setTitle("Error")
                                    .setMessage(error)
                                    .show();
                        }
                    });
        } else {
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera();
                } else {
                    Toast.makeText(this, "permission Denied...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // Menu methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Check the option selected
        if (item.getItemId() == R.id.add_image_from_network) {
            addImageFromNetwork();
            return true;
        } else if (item.getItemId() == R.id.add_image_from_gallery) {
            addImageFromGallery();
            return true;
        } else if (item.getItemId() == R.id.add_image_from_camera) {
            addImageFromCamera();
        }
        return false;
    }

    // Contextual menu methods

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            editItemInList(selectedItemPosition);
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            deleteItemFromList(selectedItemPosition);
            return true;
        } else if (item.getItemId() == R.id.share_item) {
            shareItem(selectedItemPosition);
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

    /**
     * To share the bitmap of the particular item
     * @param position position defined of the item
     */
    private void shareItem(int position) {
        // Get the item of the position
        Item item = listOfItems.get(position);

        // Load the bitmap
        Glide.with(this)
                .asBitmap()
                .load(item.url)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // Calling the intent to share the bitmap
                        Bitmap icon = resource;
                        Intent share = new Intent(Intent.ACTION_SEND);
                        share.setType("image/jpeg");

                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, "title");
                        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values);


                        OutputStream outputStream;
                        try {
                            outputStream = getContentResolver().openOutputStream(uri);
                            icon.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            outputStream.close();
                        } catch (Exception e) {
                            System.err.println(e.toString());
                        }

                        share.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(share, "Share Image"));
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    // add/edit image methods

    /**
     * To add image from the camera
     */
    private void addImageFromCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};

                requestPermissions(permission, PERMISSION_CODE);
            }
            else {
                openCamera();
            }
        }
        else {
            openCamera();
        }
    }

    /**
     * To add image from the gallery
     */
    private void addImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/jpg");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
    }

    /**
     * To show the dialog to add image
     */
    private void addImageFromNetwork() {
        // Check for the orientation
        if (this.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            // Change the dialog box appearance to true
            isDialogBoxShowed = true;
            // To set the screen orientation in portrait mode only
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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

                        // To set the screen orientation according to the user
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                    }

                    @Override
                    public void OnError(String error) {
                        isDialogBoxShowed = false;
                        new MaterialAlertDialogBuilder(GalleryActivity.this)
                                .setTitle("Error")
                                .setMessage(error)
                                .show();

                        // To set the screen orientation according to the user
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
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
                        // Try to update the list if not then just add the item
                        try {
                            // Update the list and remove the card item from the layout
                            listOfItems.set(position, item);
                            mainBinding.list.removeViewAt(position);
                        } catch(Exception e) {
                            listOfItems.add(position, item);
                        }
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
     * To setup the Floating Action Buttons
     */
    private void setupFab() {
        // To setup Action FABs
        setupFabActions();

        // For the main FAB
        mainBinding.fabMain.setOnClickListener(view -> {
            if (flag) {
                // Show all FAB
                mainBinding.fabNetwork.show();
                mainBinding.fabGallery.show();
                mainBinding.fabCamera.show();

                // Make transition through upward
                mainBinding.fabNetwork.animate().translationY(-650);
                mainBinding.fabGallery.animate().translationY(-450);
                mainBinding.fabCamera.animate().translationY(-250);

                // Rotating to 135 degree
                mainBinding.fabMain.animate().rotation(135);

                // Set the flag
                flag = false;
            } else {
                // Hide FAB
                mainBinding.fabNetwork.hide();
                mainBinding.fabGallery.hide();
                mainBinding.fabCamera.hide();

                // Make transition to their original positions
                mainBinding.fabNetwork.animate().translationY(0);
                mainBinding.fabGallery.animate().translationY(0);
                mainBinding.fabCamera.animate().translationY(0);

                // Rotating to the original position
                mainBinding.fabMain.animate().rotation(0);

                // Set the flag
                flag = true;
            }
        });
    }

    /**
     * To setup actions for Floating action button
     */
    private void setupFabActions() {
        // For camera button
        mainBinding.fabCamera.setOnClickListener(view -> {
            addImageFromCamera();
        });

        // For gallery button
        mainBinding.fabGallery.setOnClickListener(view -> {
            addImageFromGallery();
        });

        // For network button
        mainBinding.fabNetwork.setOnClickListener(view -> {
            addImageFromNetwork();
        });
    }

    /**
     * To open the camera to capture photo
     */
    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From the camera");
        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Open camera intent
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, RC_PHOTO_CAPTURE);
    }

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

        // If the list is empty then set the no item text view to visible
        if (listOfItems.isEmpty()) {
            mainBinding.noItemTextView.setVisibility(View.VISIBLE);
        } else {
            mainBinding.noItemTextView.setVisibility(View.GONE);
        }
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(Constants.DIALOG_BOX_STATUS, isDialogBoxShowed);
        super.onSaveInstanceState(outState);
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
                .apply();
    }
}