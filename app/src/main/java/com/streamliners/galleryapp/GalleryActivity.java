package com.streamliners.galleryapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.streamliners.galleryapp.adapters.ItemAdapter;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.helpers.ItemHelper;
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
    // adapter for the list view
    private ItemAdapter adapter;

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

        preferences = getPreferences(MODE_PRIVATE);
        getDataFromSharedPreferences();

        // Setup the recycler view for the items list
        setupRecyclerView();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Checking the result status
        if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            // Get URI from the intent
            Uri selectedImageUri = data.getData();

            // To show the dialog
            showImageDialog(mainBinding.list.getChildCount(), selectedImageUri.toString());
        } else if (requestCode == RC_PHOTO_CAPTURE && resultCode == RESULT_OK) {
            // To show the dialog
            showImageDialog(mainBinding.list.getChildCount(), imageUri.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
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

    // Contextual menu methods

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            Toast.makeText(this, "edit " + listOfItems.get(adapter.index).label, Toast.LENGTH_SHORT).show();
//            editItemInList(selectedItemPosition);
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            deleteItemFromList(adapter.index);
            return true;
        } else if (item.getItemId() == R.id.share_item) {
            Toast.makeText(this, "share " + listOfItems.get(adapter.index).label, Toast.LENGTH_SHORT).show();
//            shareItem(adapter.index);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    // Contextual actions methods

    /**
     * To edit the item from the list
     * @param position position defined of the item
     */
    private void editItemInList(int position) {
        // Show dialog for image editing
        showImageDialog(position, listOfItems.get(position).url);
    }

    /**
     * To delete the item from the list
     * @param position position defined of the item
     */
    private void deleteItemFromList(int position) {
        // Remove the item from the list and notify the adapter
        listOfItems.remove(position);
        adapter.notifyItemRemoved(position);

        // Showing the deleted toast
        Toast.makeText(this, "Item Deleted!", Toast.LENGTH_SHORT).show();
    }

    /**
     * To share the bitmap of the particular item card
     * @param position position defined of the item
     */
    private void shareItem(int position) {
        // Inflate layout for the item to be shared
        ItemCardBinding binding = ItemCardBinding.bind(mainBinding.list.getChildAt(position));

        // Get the screen shot of the card view
        Bitmap icon = getShot(binding.cardView);

        // Calling the intent to share the bitmap
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

    // add image methods

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
        // To set the screen orientation locked
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        new ImageDialog()
                .showDialog(this, new ImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageAddedSuccess(Item item) {
                        // Add in the list and notify the adapter
                        listOfItems.add(item);
                        adapter.notifyDataSetChanged();

                        // To set the screen orientation according to the user
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                    }

                    @Override
                    public void OnError(String error) {
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
     * To show dialog for the image already in storage
     * For: Camera, Device Storage, Edit purpose
     */
    private void showImageDialog(int position, String url) {
        // To set the screen orientation locked
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        // Get the item of the position
        // For contextual options
        Item selectedItem = new Item(url, 0, "");

        new ImageDialog()
                .showDialog(this, selectedItem, new ImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageAddedSuccess(Item item) {
//                        // Try to update the list if not then just add the item
//                        try {
//                            // Update the list and remove the card item from the layout
//                            listOfItems.set(position, item);
//                            mainBinding.list.removeViewAt(position);
//                        } catch(Exception e) {
//                            listOfItems.add(position, item);
//                        }
//                        // Inflate the view to the specified position
//                        inflateViewForItem(item, position);

                        // Add in the list and notify the adapter
                        listOfItems.add(item);
                        adapter.notifyDataSetChanged();

                        // To set the screen orientation according to the user
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                    }

                    @Override
                    public void OnError(String error) {
                        new MaterialAlertDialogBuilder(GalleryActivity.this)
                                .setTitle("Error")
                                .setMessage(error)
                                .show();

                        // To set the screen orientation according to the user
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                    }
                });
    }

    // Floating action buttons methods

    /**
     * To setup the Floating Action Buttons
     */
    private void setupFab() {
        // For the main FAB
        mainBinding.fabMain.setOnClickListener(view -> {
            if (flag) {
                expandFab();
            } else {
                collapseFab();
            }
        });

        // For camera button
        mainBinding.fabCamera.setOnClickListener(view -> {
            collapseFab();
            addImageFromCamera();
        });

        // For gallery button
        mainBinding.fabGallery.setOnClickListener(view -> {
            collapseFab();
            addImageFromGallery();
        });

        // For network button
        mainBinding.fabNetwork.setOnClickListener(view -> {
            collapseFab();
            addImageFromNetwork();
        });
    }

    /**
     * To expand the Floating Action Button Menu
     */
    private void expandFab() {
        // Showing rectangle and set listener
        mainBinding.rectangle.setVisibility(View.VISIBLE);
        mainBinding.rectangle.animate().alpha(0.3f);
        mainBinding.rectangle.setOnClickListener(v -> {
            collapseFab();
        });

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
    }

    /**
     * To collapse the Floating Action Button Menu
     */
    private void collapseFab() {
        // Hide the rectangle
        mainBinding.rectangle.animate().alpha(0);
        mainBinding.rectangle.setVisibility(View.GONE);

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

    // Utility methods

    /**
     * To setup recycler for the list of items
     */
    private void setupRecyclerView() {
        // Initializing adapter for the list view
        adapter = new ItemAdapter(this, listOfItems, new ItemAdapter.OnListSizeChangeListener() {
            @Override
            public void onListSizeChanges(int size) {
                if (size == 0) {
                    mainBinding.noItemTextView.setVisibility(View.VISIBLE);
                    return;
                }
                mainBinding.noItemTextView.setVisibility(View.GONE);
            }
        });

        // Setup the layout manager for the recycler view
        mainBinding.list.setLayoutManager(new LinearLayoutManager(this));

        // Set the adapter to the list view
        mainBinding.list.setAdapter(adapter);
    }

    /**
     * To get the screen shot of the complete view
     * @param view view for which the screen shot has to taken
     * @return bitmap image of the complete view
     */
    public Bitmap getShot(View view) {
        view.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(view.getDrawingCache());
        view.setDrawingCacheEnabled(false);
        return bitmap;
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
//            inflateViewForItem(item, mainBinding.list.getChildCount());
        }
    }

    // GSON parsing methods

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
                .apply();
    }
}