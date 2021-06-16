package com.streamliners.galleryapp.helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.streamliners.galleryapp.models.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents helper class for the sqlite database
 */
public class databaseHelper extends SQLiteOpenHelper {
    // Database name
    private static final String DATABASE_NAME = "Sql Database";
    // Table name
    private static final String TABLE_NAME = "Items";

    // Keys for storing data
    private static final String KEY_LABEL = "Label";
    private static final String KEY_COLOR = "Color";
    private static final String KEY_IMAGE_URL = "ImageUrl";

    /**
     * Constructor to initialize the helper
     * @param context context of the main activity
     */
    public databaseHelper(Context context) {
        super(context, DATABASE_NAME, null, 3);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + "(" +
                KEY_LABEL + " TEXT, " +
                KEY_COLOR + " INTEGER, " +
                KEY_IMAGE_URL + " TEXT" + ")";

        // Executing the query
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String query = "DROP TABLE IF EXISTS " + TABLE_NAME;
        // Executing the query and then closing database
        db.execSQL(query);
        onCreate(db);
    }

    /**
     * To add item in the database table
     * @param item item to be added
     */
    public void addItem (Item item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(KEY_LABEL, item.label);
        values.put(KEY_COLOR, item.color);
        values.put(KEY_IMAGE_URL, item.url);

        // Inserting the item and then closing database
        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    /**
     * Fetching all items from the database table
     * @return List of all items
     */
    public List<Item> fetchItems() {
        SQLiteDatabase db = getReadableDatabase();
        List<Item> result = new ArrayList<>();

        String query = "SELECT * FROM " + TABLE_NAME;

        // Executing the query and then getting the cursor
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                Item item = new Item(
                        cursor.getString(2),
                        cursor.getInt(1),
                        cursor.getString(0)
                );
                result.add(item);
            } while (cursor.moveToNext());
        }

        // Closing cursor and database
        cursor.close();
        db.close();

        return result;
    }

    /**
     * To clear all item in the database table
     */
    public void clearAllItems() {
        SQLiteDatabase db = getWritableDatabase();
        String query = "DELETE FROM " + TABLE_NAME;

        // Executing the query and then closing database
        db.execSQL(query);
        db.close();
    }
}
