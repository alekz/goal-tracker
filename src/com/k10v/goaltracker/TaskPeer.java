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
     * @param title the title of the task
     * @param startValue the start value of the task
     * @param targetValue the target value of the task
     * @return rowId or -1 if failed
     */
    public long createTask(String title, float startValue, float targetValue) {
        ContentValues initialValues = new ContentValues();
        initialValues.put(KEY_TITLE, title);
        initialValues.put(KEY_START_VALUE, startValue);
        initialValues.put(KEY_TARGET_VALUE, targetValue);

        return mDb.insert(TABLE, null, initialValues);
    }

    /**
     * Update the task using the details provided. The task to be updated is
     * specified using the rowId, and it is altered to use the title and body
     * values passed in
     * 
     * @param rowId id of task to update
     * @param title value to set task title to
     * @param startValue value to set task's start value to
     * @param targetValue value to set task's target value to
     * @return true if the task was successfully updated, false otherwise
     */
    public boolean updateTask(long rowId, String title, float startValue,
            float targetValue) {
        ContentValues args = new ContentValues();
        args.put(KEY_TITLE, title);
        args.put(KEY_START_VALUE, startValue);
        args.put(KEY_TARGET_VALUE, targetValue);

        return mDb.update(TABLE, args, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Delete the task with the given rowId
     * 
     * @param rowId id of task to delete
     * @return true if deleted, false otherwise
     */
    public boolean deleteTask(long rowId) {
        mDb.delete(ReportPeer.TABLE, ReportPeer.KEY_TASK_ID + "=" + rowId, null);
        return mDb.delete(TABLE, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all tasks in the database
     * 
     * @return Cursor over all tasks
     */
    public Cursor fetchAllTasks() {
        return mDb.query(TABLE, getFields(), null, null, null, null, null);
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
