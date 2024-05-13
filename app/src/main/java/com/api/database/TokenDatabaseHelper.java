package com.api.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class TokenDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TokenDB";
    private static final int DATABASE_VERSION = 1;

    // Table name
    private static final String TABLE_TOKENS = "tokens";

    // Column names
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_ACCESS_TOKEN = "access_token";
    private static final String COLUMN_REFRESH_TOKEN = "refresh_token";
    private static final String COLUMN_EXPIRY_TIME = "expiry_time";

    // Create table query
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_TOKENS + "("
            + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + COLUMN_ACCESS_TOKEN + " TEXT,"
            + COLUMN_REFRESH_TOKEN + " TEXT," + COLUMN_EXPIRY_TIME + " INTEGER" + ")";

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
    public void insertTokens(String accessToken, String refreshToken, long expiryTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCESS_TOKEN, accessToken);
        values.put(COLUMN_REFRESH_TOKEN, refreshToken);
        values.put(COLUMN_EXPIRY_TIME, expiryTime);
        db.insert(TABLE_TOKENS, null, values);
        db.close();
    }

    // Method to retrieve tokens from the database
    public String[] getTokenDetails() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_TOKENS, new String[]{COLUMN_ACCESS_TOKEN, COLUMN_REFRESH_TOKEN}, null, null, null, null, null);
        if (cursor!= null && cursor.moveToFirst()) {
            @SuppressLint("Range") String accessToken = cursor.getString(cursor.getColumnIndex(COLUMN_ACCESS_TOKEN));
            @SuppressLint("Range") String refreshToken = cursor.getString(cursor.getColumnIndex(COLUMN_REFRESH_TOKEN));
            cursor.close();
            db.close();
            return new String[]{accessToken, refreshToken};
        } else {
            return null;
        }
    }

    // Method to check if the access token has expired
    public boolean isAccessTokenExpired(long expiryTime) {
        long currentTime = System.currentTimeMillis();
        return currentTime >= expiryTime;
    }

}

