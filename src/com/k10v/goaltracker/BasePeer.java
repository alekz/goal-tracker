package com.k10v.goaltracker;

import android.database.sqlite.SQLiteDatabase;

abstract public class BasePeer {

    protected GoalTrackerDbAdapter dbAdapter;
    protected SQLiteDatabase mDb;

    public BasePeer(GoalTrackerDbAdapter dbAdapter, SQLiteDatabase mDb) {
        this.dbAdapter = dbAdapter;
        this.mDb = mDb;
    }
}
