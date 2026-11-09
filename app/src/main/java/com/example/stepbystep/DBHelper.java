package com.example.stepbystep;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * SQLite Database Helper for storing activity tracker sessions.
 * Manages database creation, CRUD operations, and statistical calculations.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "StepByStep.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "activity_history";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DATE_TIME = "date_time";
    public static final String COLUMN_STEPS = "steps";
    public static final String COLUMN_SHAKES = "shakes";
    public static final String COLUMN_DURATION = "duration_seconds";
    public static final String COLUMN_AVG_STEPS = "avg_steps_per_min";
    public static final String COLUMN_AVG_SHAKES = "avg_shakes_per_min";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATE_TIME + " TEXT, " +
                    COLUMN_STEPS + " INTEGER, " +
                    COLUMN_SHAKES + " INTEGER, " +
                    COLUMN_DURATION + " INTEGER, " +
                    COLUMN_AVG_STEPS + " REAL, " +
                    COLUMN_AVG_SHAKES + " REAL" +
                    ");";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    /**
     * Inserts a new Activity session into SQLite.
     * @param item ActivityItem to insert
     * @return inserted row ID
     */
    public long insertSession(ActivityItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DATE_TIME, item.getDateTime());
        values.put(COLUMN_STEPS, item.getSteps());
        values.put(COLUMN_SHAKES, item.getShakes());
        values.put(COLUMN_DURATION, item.getDurationSeconds());
        values.put(COLUMN_AVG_STEPS, item.getAvgStepsPerMin());
        values.put(COLUMN_AVG_SHAKES, item.getAvgShakesPerMin());

        long id = db.insert(TABLE_NAME, null, values);
        item.setId(id);
        db.close();
        return id;
    }

    /**
     * Queries all sessions from SQLite ordered by newest first (id DESC).
     * @return List of ActivityItem
     */
    public List<ActivityItem> getAllSessions() {
        List<ActivityItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_ID + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String dateTime = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE_TIME));
                int steps = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STEPS));
                int shakes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_SHAKES));
                long duration = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DURATION));
                double avgSteps = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AVG_STEPS));
                double avgShakes = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_AVG_SHAKES));

                ActivityItem item = new ActivityItem(id, dateTime, steps, shakes, duration, avgSteps, avgShakes);
                list.add(item);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return list;
    }

    /**
     * Deletes a session by ID.
     * @param id Session ID
     * @return Number of deleted rows
     */
    public int deleteSession(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_NAME, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * Calculates total accumulated steps across all sessions.
     */
    public int getTotalSteps() {
        int total = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_STEPS + ") FROM " + TABLE_NAME, null);
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    /**
     * Calculates total accumulated shakes across all sessions.
     */
    public int getTotalShakes() {
        int total = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(" + COLUMN_SHAKES + ") FROM " + TABLE_NAME, null);
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return total;
    }

    /**
     * Gets the all-time maximum steps in a single session.
     */
    public int getMaxSteps() {
        int max = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_STEPS + ") FROM " + TABLE_NAME, null);
        if (cursor != null && cursor.moveToFirst()) {
            max = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return max;
    }

    /**
     * Gets the all-time maximum shakes in a single session.
     */
    public int getMaxShakes() {
        int max = 0;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(" + COLUMN_SHAKES + ") FROM " + TABLE_NAME, null);
        if (cursor != null && cursor.moveToFirst()) {
            max = cursor.getInt(0);
            cursor.close();
        }
        db.close();
        return max;
    }
}
