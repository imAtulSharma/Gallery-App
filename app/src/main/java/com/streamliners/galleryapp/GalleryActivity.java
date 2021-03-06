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
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.streamliners.galleryapp.adapters.ItemAdapter;
import com.streamliners.galleryapp.databinding.ActivityGalleryBinding;
import com.streamliners.galleryapp.helpers.databaseHelper;
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
    private static final int CAMERA_PERMISSION_CODE = 1000;
    // Request code for the permission
    private static final int SHARE_PERMISSION_CODE = 1001;

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
    // For the touching event
    private ItemTouchHelper itemTouchHelper;
    // For the database helper
    private databaseHelper dbHelper;

    // For the options menu
    private Menu menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the main binding
        mainBinding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());

        // Setup FABs
        setupFab();

        // Initializing the helper
        dbHelper = new databaseHelper(this);
        getDataFromSqliteDatabase();

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
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera or Storage permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == SHARE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                shareImage();
            } else {
                Toast.makeText(this, "Storage permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Options Menu methods

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
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
        } else if (item.getItemId() == R.id.enable_drag_and_drop) {
            adapter.isDragAndDropEnabled = !adapter.isDragAndDropEnabled;
            changeDragAndDrop(adapter.isDragAndDropEnabled);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Contextual menu methods

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        // Check for the menu item selected
        if (item.getItemId() == R.id.edit_item) {
            // Show dialog for image editing
            showImageDialog(adapter.visibleItemsList.get(adapter.index));
            return true;
        } else if (item.getItemId() == R.id.share_item) {
            sharingItem();
            return true;
        }
        return super.onContextItemSelected(item);
    }

    /**
     * To get the required permissions to save the bitmap and then share it
     */
    private void sharingItem() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                String[] permission = {Manifest.permission.WRITE_EXTERNAL_STORAGE};

                requestPermissions(permission, SHARE_PERMISSION_CODE);
            }
            else {
                shareImage();
            }
        }
        else {
            shareImage();
        }
    }

    /**
     * To share the bitmap of the particular item card
     */
    private void shareImage() {
        // Get the screen shot of the card view
        Bitmap icon = getShot(adapter.itemBinding.cardView);

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

                requestPermissions(permission, CAMERA_PERMISSION_CODE);
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
                        // Notify the adapter
                        adapter.add(item);

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
                            // Notify the adapter
                            adapter.add(item);
                        } else {
                            // Notify the adapter
                            adapter.edit(adapter.index, item);
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

        // To setup the touch listener
        changeDragAndDrop(false);
    }

    /**
     * To enable Drag and Drop functionality
     */
    private void changeDragAndDrop(boolean toEnable) {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAbsoluteAdapterPosition();
                int toPosition = target.getAbsoluteAdapterPosition();

                // Swap the items
                adapter.move(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // Remove swiped item from list and notify the RecyclerView
                adapter.delete(viewHolder.getAbsoluteAdapterPosition());
            }
        };

        // Firstly detach the helper if attached
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }

        // Check the feature validity
        if (toEnable) {
            // Changing the User Interface
            menu.getItem(0).setIcon(R.drawable.ic_done);
            menu.getItem(1).setVisible(false);
            menu.getItem(2).setVisible(false);
            setTitle("Drag and Drop");
            mainBinding.fabMain.animate().alpha(0);
            // Because the code will immediately make the button invisible that's why delay
            new Handler().postDelayed(() -> mainBinding.fabMain.setVisibility(View.GONE), 1000);

            // Changing directions
            callback.setDefaultSwipeDirs(0);
            callback.setDefaultDragDirs(ItemTouchHelper.UP | ItemTouchHelper.DOWN);
        } else {
            // Check for the menu
            if (menu != null) {
                // Make User Interface in original state
                menu.getItem(0).setIcon(R.drawable.ic_drag_indicator);
                menu.getItem(1).setVisible(true);
                menu.getItem(2).setVisible(true);
                setTitle("Gallery");
                mainBinding.fabMain.setVisibility(View.VISIBLE);
                mainBinding.fabMain.animate().alpha(1);
            }

            // Changing directions
            callback.setDefaultSwipeDirs(ItemTouchHelper.LEFT);
            callback.setDefaultDragDirs(0);
        }

        // Make a new item touch helper
        itemTouchHelper = new ItemTouchHelper(callback);
        // Attach to the recycler view
        itemTouchHelper.attachToRecyclerView(mainBinding.list);
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
     * To restore the data using Sqlite database
     */
    private void getDataFromSqliteDatabase() {
        // Fetching the list of items from the database
        listOfItems = dbHelper.fetchItems();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Firstly clear all previous items in the database table
        dbHelper.clearAllItems();

        // Adding each item in the database table
        for (Item item : listOfItems) {
            dbHelper.addItem(item);
        }
    }
}