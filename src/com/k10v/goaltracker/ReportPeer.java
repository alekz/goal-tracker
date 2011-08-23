package com.k10v.goaltracker;

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
