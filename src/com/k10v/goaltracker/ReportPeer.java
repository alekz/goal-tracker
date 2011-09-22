package com.k10v.goaltracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

public class ReportPeer extends BasePeer {

    public static final String TABLE = "reports";

    public static final String KEY_ID = "_id";
    public static final String KEY_TASK_ID = "task_id";
    public static final String KEY_DATE = "date";
    public static final String KEY_RELATIVE = "relative";
    public static final String KEY_VALUE = "value";

    public ReportPeer(GoalTrackerDbAdapter dbAdapter, SQLiteDatabase mDb) {
        super(dbAdapter, mDb);
    }

    /**
     * Create a new report. If the report is successfully created return the new
     * rowId for that report, otherwise return a -1 to indicate failure.
     *
     * @param taskId
     * @param date
     * @param value
     * @param relative
     * @return rowId or -1 if failed
     */
    public long createReport(long taskId, String date, double value,
            boolean relative) {

        ContentValues values = new ContentValues();

        values.put(KEY_TASK_ID, taskId);
        values.put(KEY_DATE, date);
        values.put(KEY_VALUE, value);
        values.put(KEY_RELATIVE, relative);

        return mDb.insert(TABLE, null, values);
    }

    /**
     * Create a new report
     * 
     * @param taskId
     * @param date
     * @param value
     * @return
     */
    public long createReport(long taskId, String date, double value) {
        return createReport(taskId, date, value, false);
    }

    /**
     * Update the report with given ID using the details provided
     *
     * @param rowId
     * @param date
     * @param value
     * @param relative
     * @return true if the report was successfully updated, false otherwise
     */
    public boolean updateReport(long rowId, String date, double value,
            boolean relative) {

        ContentValues values = new ContentValues();

        values.put(KEY_DATE, date);
        values.put(KEY_VALUE, value);
        values.put(KEY_RELATIVE, relative);

        return mDb.update(TABLE, values, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Update the report with given ID using the details provided
     * 
     * @param rowId
     * @param date
     * @param value
     * @return true if the report was successfully updated, false otherwise
     */
    public boolean updateReport(long rowId, String date, double value) {
        return updateReport(rowId, date, value, false);
    }

    /**
     * Delete the report with the given rowId
     * 
     * @param rowId
     * @return true if deleted, false otherwise
     */
    public boolean deleteReport(long rowId) {
        return mDb.delete(TABLE, KEY_ID + "=" + rowId, null) > 0;
    }

    /**
     * Delete all reports for the given task
     *
     * @param taskId
     * @return true if deleted, false otherwise
     */
    public boolean deleteReportsByTask(long taskId) {
        return mDb.delete(TABLE, KEY_TASK_ID + "=" + taskId, null) > 0;
    }

    /**
     * Return a Cursor over the list of all reports of the given task, reports
     * are sorted by date
     * 
     * @param taskId
     * @param reverseOrder
     * @return Cursor over all reports of the given task
     */
    public Cursor fetchReportsByTask(long taskId, boolean reverseOrder) {
        String sortDirection = reverseOrder ? "DESC" : "ASC";
        return mDb.query(TABLE, getFields(), KEY_TASK_ID + "=" + taskId, null,
                null, null, KEY_DATE + " " + sortDirection);
    }

    public Cursor fetchReportsByTask(long taskId) {
        return fetchReportsByTask(taskId, false);
    }

    /**
     * Return a Cursor positioned at the report that matches the given rowId
     * 
     * @param rowId id of report to retrieve
     * @return Cursor positioned to matching report, if found
     * @throws SQLException if report could not be found/retrieved
     */
    public Cursor fetchReport(long rowId) throws SQLException {
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
        return new String[] { KEY_ID, KEY_TASK_ID, KEY_DATE, KEY_RELATIVE,
                KEY_VALUE };
    }
}
