package com.streamliners.galleryapp;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.streamliners.galleryapp.adapters.ItemAdapter;
import com.streamliners.galleryapp.constants.Constants;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.databinding.ItemCardBinding;
import com.streamliners.galleryapp.models.Item;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
            assert data != null;
            Uri selectedImageUri = data.getData();

            // To show the dialog
            showImageDialog(new Item(selectedImageUri.toString(), 0, null));
        } else if (requestCode == RC_PHOTO_CAPTURE && resultCode == RESULT_OK) {
            // To show the dialog
            showImageDialog(new Item(imageUri.toString(), 0, null));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "permission Denied...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Options Menu methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.menu_options, menu);

        // Get the search view
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.sort_alphabetically) {
            adapter.sortAlphabetically();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Contextual menu methods

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            editItemInList(adapter.index);
            return true;
        } else if (item.getItemId() == R.id.delete_item) {
            deleteItemFromList(adapter.index);
            return true;
        } else if (item.getItemId() == R.id.share_item) {
            Toast.makeText(this, "Share Image", Toast.LENGTH_SHORT).show();
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
        showImageDialog(listOfItems.get(position));
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
    private void showImageDialog(Item selectedItem) {
        // To set the screen orientation locked
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);

        new ImageDialog()
                .showDialog(this, selectedItem, new ImageDialog.OnCompleteListener() {
                    @Override
                    public void OnImageAddedSuccess(Item item) {
                        if (selectedItem.label == null) {
                            // Add in the list and notify the adapter
                            listOfItems.add(item);
                            adapter.notifyDataSetChanged();

                            // Showing the adding toast
                            Toast.makeText(GalleryActivity.this, "Item Added!", Toast.LENGTH_SHORT).show();
                        } else {
                            listOfItems.set(adapter.index, item);
                            adapter.notifyItemChanged(adapter.index);

                            // Showing the editing toast
                            Toast.makeText(GalleryActivity.this, "Item Edited!", Toast.LENGTH_SHORT).show();
                        }

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
        mainBinding.rectangle.setOnClickListener(v -> collapseFab());

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
        adapter = new ItemAdapter(this, listOfItems, size -> {
            if (size == 0) {
                mainBinding.noItemTextView.setVisibility(View.VISIBLE);
                return;
            }
            mainBinding.noItemTextView.setVisibility(View.INVISIBLE);
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
     * To restore the data using shared preferences
     */
    private void getDataFromSharedPreferences() {
        String json = preferences.getString(Constants.LIST_OF_ITEMS, "[]");

        // Make the list item from the shared preferences
        listOfItems = (new Gson()).fromJson(json, new TypeToken<List<Item>>() {}.getType());
    }

    @Override
    protected void onPause() {
        super.onPause();

        preferences.edit()
                .putString(Constants.LIST_OF_ITEMS, (new Gson()).toJson(listOfItems))
                .apply();
    }
}