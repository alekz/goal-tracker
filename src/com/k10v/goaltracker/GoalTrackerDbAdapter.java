package com.k10v.goaltracker;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class GoalTrackerDbAdapter {

    private static final String TAG = "GoalTrackerDbAdapter";
    private DatabaseHelper mDbHelper;
    private SQLiteDatabase mDb;
    private final Context mCtx;

    private static final int DATABASE_VERSION = 0;

    private static final String DATABASE_NAME = "goaltracker";

    // private static final String TABLE_TASKS = "tasks";
    // private static final String TABLE_REPORTS = "reports";

    // public static final String KEY_TASK_ID = "id";
    // public static final String KEY_TASK_TITLE = "title";

    // public static final String KEY_REPORT_ID = "id";
    // public static final String KEY_REPORT_TASK_ID = "task_id";
    // public static final String KEY_REPORT_DATE = "date";
    // public static final String KEY_REPORT_RELATIVE = "relative";
    // public static final String KEY_REPORT_VALUE = "value";

    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {

            // == Tasks ==
            db.execSQL("CREATE TABLE tasks ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "title TEXT NOT NULL, "
                    + "start_value NUMERIC NOT NULL DEFAULT 0"
                    + ")");
            db.execSQL("CREATE INDEX idx_tasks_title ON tasks (title)");

            // == Reports ==
            db.execSQL("CREATE TABLE reports ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "task_id INTEGER NOT NULL, "
                    + "date TEXT NOT NULL, "
                    + "relative INTEGER NOT NULL DEFAULT 0, "
                    + "value NUMERIC NOT NULL"
                    + ")");
            db.execSQL("CREATE UNIQUE INDEX idx_reports_task_date ON reports ("
                    + "task_id, date)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS tasks");
            db.execSQL("DROP TABLE IF EXISTS reports");
            onCreate(db);
        }
    }

    /**
     * Constructor - takes the context to allow the database to be
     * opened/created
     * 
     * @param ctx the Context within which to work
     */
    public GoalTrackerDbAdapter(Context ctx) {
        this.mCtx = ctx;
    }

    /**
     * Open the goaltracker database. If it cannot be opened, try to create a
     * new instance of the database. If it cannot be created, throw an exception
     * to signal the failure
     * 
     * @return this
     * @throws SQLException if the database could be neither opened or created
     */
    public GoalTrackerDbAdapter open() throws SQLException {
        mDbHelper = new DatabaseHelper(mCtx);
        mDb = mDbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        mDbHelper.close();
    }
}
