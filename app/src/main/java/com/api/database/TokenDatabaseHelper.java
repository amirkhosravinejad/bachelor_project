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
    private SQLiteDatabase dbWriter, dbReader;
    private Cursor cursor;
    public TokenDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void setDbWriter(SQLiteDatabase dbWriter) {
        this.dbWriter = dbWriter;
    }

    public void setDbReader(SQLiteDatabase dbReader) {
        this.dbReader = dbReader;
    }

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    public SQLiteDatabase getDbWriter() {
        return dbWriter;
    }

    public SQLiteDatabase getDbReader() {
        return dbReader;
    }

    public Cursor getCursor() {
        return cursor;
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
    public int insertTokens(ContentValues values, String server_ip, String accessToken,
                            String refreshToken, String expiryTime) {
        values.put(COLUMN_SERVER_IP, server_ip);
        values.put(COLUMN_ACCESS_TOKEN, accessToken);
        values.put(COLUMN_REFRESH_TOKEN, refreshToken);
        values.put(COLUMN_EXPIRY_TIME, expiryTime);
        // Insert the new row
        int newRowId = (int) dbWriter.insert(TABLE_TOKENS, null, values);

        // Check if the insertion was successful
        if (newRowId!= -1) {
            // Insertion was successful
            Log.d("bach-prj", "new row inserted in rowID " + newRowId);
        } else {
            // Insertion failed
            Log.d("bach-prj", "insertion to db failed");
        }
        dbWriter.close();
        return newRowId;
    }

    public int selectAllRows() {
        // on below line we are creating a cursor with query to
        // read data from database.
        cursor = dbReader.rawQuery("SELECT * FROM " + TABLE_TOKENS, null);
        int count = cursor.getCount();
        // moving our cursor to first position.
        if (cursor.moveToFirst()) {
            do {
                // on below line we are retrieving tokens from ever row
                String server_ip = cursor.getString(1);
                String access_t = cursor.getString(2);
                String refresh_t = cursor.getString(3);
                String expiry = cursor.getString(4);

                Log.d("bach-prj", "row " + cursor.getPosition() + " serverIP: " + server_ip +
                    " access token: " + access_t + " refresh token: " + refresh_t + " expiry : " + expiry);
            } while (cursor.moveToNext());
            // moving our cursor to next.
        }
        // at last closing our cursor
        cursor.close();
        return count;
    }

    public void updateRow(ContentValues values, int index, String accessToken, String refreshToken, String expiryTime){
        // calling a method to get writable database.
//        SQLiteDatabase db = this.getWritableDatabase();
//        ContentValues values = new ContentValues();

        // on below line we are passing all values
        // along with its key and value pair.
        values.put(COLUMN_ACCESS_TOKEN, accessToken);
        values.put(COLUMN_REFRESH_TOKEN, refreshToken);
        values.put(COLUMN_EXPIRY_TIME,expiryTime);

        // on below line we are calling a update method to update our database and passing our values.
        // and we are comparing it with the index in arguments to find the row
        dbWriter.update(TABLE_TOKENS, values, "_id=?", new String[]{String.valueOf(index)});
        dbWriter.close();
    }

    public String[] fetchLastRow(TokenDatabaseHelper dbhelper) {
        // an array of string to be returned
        // index 0 is formerServerIP, 1 is access token, 2 is refresh token
        // fourth one is expiry time, and the last is number of rows in table
        String [] serverAndToken = new String[5];
        try {
            SQLiteDatabase db = dbhelper.getReadableDatabase();
            cursor = db.rawQuery("SELECT * FROM tokens", null);
            int count = cursor.getCount();
            if (count == 0) {
                return null;
            }
            cursor.move(count);
            serverAndToken[0] = cursor.getString(1);
            serverAndToken[1] = cursor.getString(2);
            serverAndToken[2] = cursor.getString(3);
            serverAndToken[3] = cursor.getString(4);
            serverAndToken[4] = String.valueOf(count);
            Log.d("bach-prj", "row " + cursor.getPosition() + " serverIP: " + serverAndToken[0]
                    + " access token: " + serverAndToken[1] + " refresh token: "
                    + serverAndToken[2] + " expiry : " + serverAndToken[3]);
            cursor.close();
        } catch (Exception e) {
            Log.e("bach-prj", "exception occured in fetch last row");
        }
        return serverAndToken;
    }

}

