package com.k10v.goaltracker;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class TaskGraph extends Activity {

    private static final String TAG = "TaskGraph";

    private static final int ACTIVITY_CREATE_REPORT = 0;
    private static final int ACTIVITY_VIEW_TASK_REPORTS = 1;

    public static final int MENU_ID_CREATE_REPORT = Menu.FIRST;
    public static final int MENU_ID_VIEW_TASK_REPORTS = Menu.FIRST + 1;

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
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
     * Create main menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ID_CREATE_REPORT, 0, R.string.menu_create_report);
        menu.add(0, MENU_ID_VIEW_TASK_REPORTS, 0, R.string.menu_view_task_reports);
        return result;
    }

    /**
     * Main menu item clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case MENU_ID_CREATE_REPORT:
            runCreateReport();
            return true;

        case MENU_ID_VIEW_TASK_REPORTS:
            runViewTaskReports(mTaskId);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Process result from other activity (create report etc.)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        drawGraph();
    }

    /**
     * Runs "Create Report" activity
     */
    private void runCreateReport() {
        Intent i = new Intent(this, ReportEdit.class);
        i.putExtra(ReportPeer.KEY_TASK_ID, mTaskId);
        startActivityForResult(i, ACTIVITY_CREATE_REPORT);
    }

    /**
     * Displays list of task reports
     *
     * @param rowId
     */
    private void runViewTaskReports(long rowId) {
        Intent i = new Intent(this, ReportList.class);
        i.putExtra(TaskPeer.KEY_ID, rowId);
        startActivityForResult(i, ACTIVITY_VIEW_TASK_REPORTS);
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
