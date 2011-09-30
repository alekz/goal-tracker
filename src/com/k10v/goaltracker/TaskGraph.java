package com.k10v.goaltracker;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;

public class TaskGraph extends Activity {

    private static final String TAG = "TaskGraph";

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mTaskCursor;
    private Cursor mReportsCursor;
    private long mTaskId;
    private Panel mGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.task_graph);

        // Prepare DB adapter
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();

        // Retrieve task ID: first check if it's stored in saved state, if not
        // then check Intent's extras. If ID is empty, it is an error.
        if (savedInstanceState != null) {
            mTaskId = (Long) savedInstanceState.getSerializable(
                    TaskPeer.KEY_ID);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mTaskId = extras.getLong(TaskPeer.KEY_ID);
            }
        }
        if (mTaskId == 0) {
            // Error
            Log.e(TAG, "Task ID is empty");
            finish();
        }

        // Find graph surface and start drawing the graph
        mGraph = (Panel) findViewById(R.id.graph_surface);
        drawGraph();
    }

    /**
     * Called when activity state should be temporarily saved, for example when
     * activity is about to be killed in order to retrieve system resources
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(TaskPeer.KEY_ID, mTaskId);
    }

    /**
     * Draw the graph.
     */
    private void drawGraph() {

        mTaskCursor = mDbHelper.getTaskPeer().fetchTask(mTaskId);
        startManagingCursor(mTaskCursor);

        mReportsCursor = mDbHelper.getReportPeer().
                fetchReportsByTask(mTaskId);
        startManagingCursor(mReportsCursor);

        mGraph.setData(mTaskCursor, mReportsCursor);
    }
}
