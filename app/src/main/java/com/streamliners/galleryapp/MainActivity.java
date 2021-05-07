package com.streamliners.galleryapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.streamliners.galleryapp.databinding.ActivityMainBinding;
import com.streamliners.galleryapp.databinding.DialogAddImageBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding mainBinding;

    DialogAddImageBinding dialogBinding;
    AlertDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(mainBinding.getRoot());
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

    private void showAddImageDialog() {
        new AddImageDialog()
                .show(this);
    }
}