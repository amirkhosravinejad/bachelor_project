package com.api.database;

import static org.mockito.Mockito.mock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import junit.framework.TestCase;

import org.junit.Test;

public class TokenDatabaseHelperTest extends TestCase {
    private TokenDatabaseHelper dbhelper;

    public void setUp() {
        Context context = mock(android.content.Context.class);
        try {
            dbhelper = new TokenDatabaseHelper(context);
            dbhelper.setDbReader(mock(SQLiteDatabase.class));
            dbhelper.setDbWriter(mock(SQLiteDatabase.class));
            dbhelper.setCursor(mock(Cursor.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testInsertTokens() {
        ContentValues values = mock(ContentValues.class);
        int last_row = dbhelper.insertTokens(values, "192.168.1.1", "salam",
                "s1", "12987");
        assertNotSame(-1, last_row);
    }

    @Test
    public void testSelectAllRows() {
        assertNotNull(dbhelper.getCursor());

        int res = dbhelper.insertTokens(mock(ContentValues.class), "192.168.1.1", "salam",
                "s1", "12987");
        int rowNumbers = dbhelper.selectAllRows();
        assertTrue(rowNumbers > -1);
    }

    @Test
    public void testUpdateRow() {
    }

    @Test
    public void testFetchLastRow() {
    }
}