package com.k10v.goaltracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class TaskPeer extends BasePeer {

    public static final String TABLE = "tasks";

    public static final String KEY_ID = "_id";
    public static final String KEY_TITLE = "title";
    public static final String KEY_START_VALUE = "start_value";
    public static final String KEY_TARGET_VALUE = "target_value";

    public TaskPeer(GoalTrackerDbAdapter dbAdapter, SQLiteDatabase mDb) {
        super(dbAdapter, mDb);
    }

    /**
     * Create a new task. If the task is successfully created return the new
     * rowId for that task, otherwise return a -1 to indicate failure.
     *
     * @param title
     * @param startValue
     * @param targetValue
     * @return rowId or -1 if failed
     */
    public long createTask(String title, double startValue, Double targetValue) {

        ContentValues values = createContentValues(title, startValue,
                targetValue);

        return mDb.insert(TABLE, null, values);
    }

    /**
     * Update the task with given ID using the details provided.
     *
     * @param rowId
     * @param title
     * @param startValue
     * @param targetValue
     * @return true if the task was successfully updated, false otherwise
     */
    public boolean updateTask(long rowId, String title, double startValue,
            Double targetValue) {

        ContentValues values = createContentValues(title, startValue,
                targetValue);

        return mDb.update(TABLE, values, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Creates ContentValues object and fills it with the given values.
     *
     * @param title
     * @param startValue
     * @param targetValue
     * @return ContentValues object filled with the given values
     */
    private ContentValues createContentValues(String title, double startValue,
            Double targetValue) {

        ContentValues values = new ContentValues();

        values.put(KEY_TITLE, title);
        values.put(KEY_START_VALUE, startValue);
        values.put(KEY_TARGET_VALUE, targetValue);

        return values;
    }

    /**
     * Delete the task with the given rowId
     *
     * @param rowId id of task to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTask(long rowId) {
        dbAdapter.getReportPeer().deleteReportsByTask(rowId);
        return mDb.delete(TABLE, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all tasks in the database
     *
     * @return Cursor over all tasks
     */
    public Cursor fetchAllTasks() {
        return mDb.query(TABLE, getFields(), null, null, null, null, KEY_TITLE);
    }

    /**
     * Return a Cursor positioned at the task that matches the given rowId
     *
     * @param rowId id of task to retrieve
     * @return Cursor positioned to matching task, if found
     * @throws SQLException if task could not be found/retrieved
     */
    public Cursor fetchTask(long rowId) throws SQLException {
        Cursor mCursor = mDb.query(true, TABLE, getFields(),
                KEY_ID + "=" + rowId,
                null, null, null, null, null);
        if (mCursor != null) {
            mCursor.moveToFirst();
        }
        return mCursor;
    }

    /**
     * Returns list of all fields names in the table
     *
     * @return array with fields names
     */
    protected String[] getFields()
    {
        return new String[] { KEY_ID, KEY_TITLE, KEY_START_VALUE,
                KEY_TARGET_VALUE };
    }
}
