package com.k10v.goaltracker;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.SimpleCursorAdapter;

public class ReportList extends ListActivity {

    private static final String TAG = "ReportList";

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mReportsCursor;
    private long mTaskId;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.report_list);

        // Prepare DB adapter
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();

        // Retrieve task ID: first check if it's stored in saved state, if not
        // then check Intent's extras. If ID is empty, it is an error.
        if (savedInstanceState != null) {
            mTaskId = (Long) savedInstanceState.getSerializable(
                    ReportPeer.KEY_TASK_ID);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mTaskId = extras.getLong(ReportPeer.KEY_TASK_ID);
            }
        }
        if (mTaskId == 0) {
            // Error
            Log.e(TAG, "Task ID is empty");
            finish();
        }

        fillReportsList();

        registerForContextMenu(getListView());
    }

    /**
     * Fills/reloads the list of reports.
     */
    private void fillReportsList() {
        mReportsCursor = mDbHelper.getReportPeer().fetchReportsByTask(mTaskId);
        startManagingCursor(mReportsCursor);

        String[] from = new String[] { ReportPeer.KEY_DATE };
        int[] to = new int[] { R.id.report_row_date };

        SimpleCursorAdapter reports = new SimpleCursorAdapter(this,
                R.layout.report_row, mReportsCursor, from, to);
        setListAdapter(reports);
    }
}
