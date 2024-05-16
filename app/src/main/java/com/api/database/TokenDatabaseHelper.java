package com.api.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TokenDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TokenDB";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_TOKENS = "tokens";

    // Column names
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_SERVER_IP = "server_ip";
    private static final String COLUMN_ACCESS_TOKEN = "access_token";
    private static final String COLUMN_REFRESH_TOKEN = "refresh_token";
    private static final String COLUMN_EXPIRY_TIME = "expiry_time";

    // Create table query
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_TOKENS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_SERVER_IP + " TEXT,"
            + COLUMN_ACCESS_TOKEN + " TEXT," + COLUMN_REFRESH_TOKEN + " TEXT,"
            + COLUMN_EXPIRY_TIME + " TEXT" + ")";

    public TokenDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TOKENS);
        onCreate(db);
    }

    // Method to insert tokens into the database
    public void insertTokens(String server_ip, String accessToken, String refreshToken, String expiryTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SERVER_IP, server_ip);
        values.put(COLUMN_ACCESS_TOKEN, accessToken);
        values.put(COLUMN_REFRESH_TOKEN, refreshToken);
        values.put(COLUMN_EXPIRY_TIME, expiryTime);
        // Insert the new row
        long newRowId = db.insert(TABLE_TOKENS, null, values);

        // Check if the insertion was successful
        if (newRowId!= -1) {
            // Insertion was successful
            Log.d("zaneto", "new row inserted in rowID " + newRowId);
        } else {
            // Insertion failed
            Log.d("zaneto", "insertion to db failed");
        }
        selectAllRows(db);
        db.close();
    }

    public void selectAllRows(SQLiteDatabase db) {
        // on below line we are creating a cursor with query to
        // read data from database.
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_TOKENS, null);
        // moving our cursor to first position.
        if (cursor.moveToFirst()) {
            do {
                // on below line we are retrieving tokens from ever row
                String server_ip = cursor.getString(1);
                String access_t = cursor.getString(2);
                String refresh_t = cursor.getString(3);
                String expiry = cursor.getString(4);
//                Log.d("zaneto", String.valueOf(new Date()));
                Log.d("zaneto", "row " + cursor.getPosition() + " serverIP: " + server_ip +
                    " access token: " + access_t + " refresh token: " + refresh_t + " expiry : " + expiry);
            } while (cursor.moveToNext());
            // moving our cursor to next.
        }
        // at last closing our cursor
        // and returning our array list.
        cursor.close();
    }

    public void updateRow(int index, String accessToken, String refreshToken, String expiryTime){
        // calling a method to get writable database.
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(COLUMN_ACCESS_TOKEN, accessToken);
        values.put(COLUMN_REFRESH_TOKEN, refreshToken);
        values.put(COLUMN_EXPIRY_TIME,expiryTime);

        // on below line we are calling a update method to update our database and passing our values.
        // and we are comparing it with the index in arguments to find the row
        db.update(TABLE_TOKENS, values, "_id=?", new String[]{String.valueOf(index)});
        db.close();
    }

    public void deleteAllRows() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TOKENS,null,null);
        db.close();
    }

    // Method to check if the access token has expired
//    public boolean isAccessTokenExpired(String expiryTime) {
//        long currentTime = System.currentTimeMillis();
//        return currentTime >= expiryTime;
//    }

}

