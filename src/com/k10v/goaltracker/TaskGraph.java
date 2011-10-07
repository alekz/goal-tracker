package com.k10v.goaltracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class TaskGraph extends Activity {

    private static final String TAG = "TaskGraph";

    private static final int ACTIVITY_CREATE_REPORT = 0;
    private static final int ACTIVITY_VIEW_TASK_REPORTS = 1;
    private static final int ACTIVITY_EDIT_TASK = 2;

    private static final int DIALOG_CONFIRM_DELETE_TASK_ID = 1;

    public static final int MENU_ID_CREATE_REPORT = Menu.FIRST;
    public static final int MENU_ID_VIEW_TASK_REPORTS = Menu.FIRST + 1;
    public static final int MENU_ID_EDIT_TASK = Menu.FIRST + 2;
    public static final int MENU_ID_DELETE_TASK = Menu.FIRST + 3;

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
            mTaskId = (Long) savedInstanceState.getSerializable(TaskPeer.KEY_ID);
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
        menu.add(0, MENU_ID_VIEW_TASK_REPORTS, 1, R.string.menu_view_task_reports);
        menu.add(0, MENU_ID_EDIT_TASK, 2, R.string.menu_edit_task);
        menu.add(0, MENU_ID_DELETE_TASK, 3, R.string.menu_delete_task);
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
            runViewTaskReports();
            return true;

        case MENU_ID_EDIT_TASK:
            runEditTask();
            return true;

        case MENU_ID_DELETE_TASK:
            runDeleteTask();
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
     * Create dialogs (confirmations etc.)
     */
    @Override
    protected Dialog onCreateDialog(int id) {

        Dialog dialog;

        switch (id) {

        // Confirmation dialog for "Delete Task"
        case DIALOG_CONFIRM_DELETE_TASK_ID:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder
                    // Title
                    .setMessage(R.string.message_confirm_delete_task)

                    // "Yes" button
                    .setPositiveButton(R.string.button_yes,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    doDeleteTask();
                                }
                            })

                    // "No" button
                    .setNegativeButton(R.string.button_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

            dialog = builder.create();
            break;

        default:
            dialog = null;
            break;

        }

        return dialog;
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
     */
    private void runViewTaskReports() {
        Intent i = new Intent(this, ReportList.class);
        i.putExtra(TaskPeer.KEY_ID, mTaskId);
        startActivityForResult(i, ACTIVITY_VIEW_TASK_REPORTS);
    }

    /**
     * Runs "Edit Task" activity
     */
    private void runEditTask() {
        Intent i = new Intent(this, TaskEdit.class);
        i.putExtra(TaskPeer.KEY_ID, mTaskId);
        startActivityForResult(i, ACTIVITY_EDIT_TASK);
    }

    /**
     * Launches confirmation dialog asking is user really wants to delete the
     * task, then deletes it if user confirms
     */
    private void runDeleteTask() {
        showDialog(DIALOG_CONFIRM_DELETE_TASK_ID);
    }

    /**
     * Actually deletes task and closes the activity. Shouldn't be called
     * directly, use runDeleteTask() instead.
     */
    private void doDeleteTask() {

        // Delete the task
        mDbHelper.getTaskPeer().deleteTask(mTaskId);

        // Show message
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.message_task_deleted, Toast.LENGTH_SHORT);
        toast.show();

        // Close the activity
        finish();

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
