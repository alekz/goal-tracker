package com.k10v.goaltracker;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ReportList extends ListActivity {

    private static final int ACTIVITY_CREATE_REPORT = 0;
    private static final int ACTIVITY_EDIT_REPORT = 1;

    private static final int DIALOG_CONFIRM_DELETE_REPORT_ID = 1;

    private static final String TAG = "ReportList";

    public static final int MENU_ID_CREATE_REPORT = Menu.FIRST;
    public static final int MENU_ID_EDIT_REPORT = Menu.FIRST + 1;
    public static final int MENU_ID_DELETE_REPORT = Menu.FIRST + 2;

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mTaskCursor;
    private Cursor mReportsCursor;
    private long mTaskId;

    /**
     * Used for "Delete Report" operation.
     */
    private long rowIdToDelete;

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

        // Retrieve task details from the database and use task title as an
        // activity title
        mTaskCursor = mDbHelper.getTaskPeer().fetchTask(mTaskId);
        setTitle(mTaskCursor.getString(mTaskCursor.getColumnIndex(TaskPeer.KEY_TITLE)));

        fillReportsList();

        registerForContextMenu(getListView());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(TaskPeer.KEY_ID, mTaskId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDbHelper.close();
    }

    /**
     * Create main menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ID_CREATE_REPORT, 0, R.string.menu_create_report)
                .setIcon(android.R.drawable.ic_menu_add);
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

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Create context menu.
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {

        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mReportsCursor;
        c.moveToPosition(info.position);
        String title = c.getString(c.getColumnIndexOrThrow(ReportPeer.KEY_DATE));
        menu.setHeaderTitle(title);

        // Add menu items
        menu.add(0, MENU_ID_EDIT_REPORT, 1, R.string.context_menu_edit_report);
        menu.add(0, MENU_ID_DELETE_REPORT, 2, R.string.context_menu_delete_report);
    }

    /**
     * Context menu item clicked.
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {

        // Get ID of the selected record
        AdapterContextMenuInfo info =
                (AdapterContextMenuInfo) item.getMenuInfo();
        long rowId = info.id;

        switch (item.getItemId()) {

        case MENU_ID_EDIT_REPORT:
            runEditReport(rowId);
            return true;

        case MENU_ID_DELETE_REPORT:
            runDeleteReport(rowId);
            return true;

        }

        return super.onContextItemSelected(item);
    }

    /**
     * Item in the list clicked.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        runEditReport(id);
    }

    /**
     * Process result from other activity (create/edit report etc.)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        fillReportsList();
    }

    /**
     * Creates dialogs (confirmations etc.)
     */
    @Override
    protected Dialog onCreateDialog(int id) {

        switch (id) {

        // Confirmation dialog for "Delete Task"
        case DIALOG_CONFIRM_DELETE_REPORT_ID:
            DeleteReportDialogBuilder builder = new DeleteReportDialogBuilder(this, mDbHelper, rowIdToDelete) {
                @Override
                public void afterDeleteReport() {
                    fillReportsList();
                }
            };
            return builder.create();

        }

        return null;
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
     * Runs "Edit Report" activity for the report with given ID
     *
     * @param rowId ID of the report to edit
     */
    private void runEditReport(long rowId) {
        Intent i = new Intent(this, ReportEdit.class);
        i.putExtra(ReportPeer.KEY_ID, rowId);
        startActivityForResult(i, ACTIVITY_EDIT_REPORT);
    }

    /**
     * Launches confirmation dialog asking is user really wants to delete the
     * report, then deletes it if user confirms
     *
     * @param rowId ID of the report to delete
     */
    private void runDeleteReport(long rowId) {
        rowIdToDelete = rowId;
        showDialog(DIALOG_CONFIRM_DELETE_REPORT_ID);
    }

    /**
     * Fills/reloads the list of reports.
     */
    private void fillReportsList() {
        mReportsCursor = mDbHelper.getReportPeer().
                fetchReportsByTask(mTaskId, true);
        startManagingCursor(mReportsCursor);

        String[] from = new String[] {
                ReportPeer.KEY_DATE,
                ReportPeer.KEY_VALUE
        };
        int[] to = new int[] {
                R.id.report_row_date,
                R.id.report_row_value
        };

        SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(
                this, R.layout.report_row, mReportsCursor, from, to);

        final Context context = this;

        cursorAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

                if (columnIndex == cursor.getColumnIndexOrThrow(ReportPeer.KEY_DATE)) {
                    TextView tv = (TextView) view;
                    String sqlDateString = cursor.getString(columnIndex);
                    tv.setText(Util.formatDate(sqlDateString, context));
                    return true;
                }

                if (columnIndex == cursor.getColumnIndexOrThrow(ReportPeer.KEY_VALUE)) {
                    TextView tv = (TextView) view;
                    tv.setText(Util.formatNumber(cursor.getFloat(columnIndex)));
                    return true;
                }

                return false;
            }
        });
        setListAdapter(cursorAdapter);
    }
}
