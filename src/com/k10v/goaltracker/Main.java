package com.k10v.goaltracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

// TODO: showDialog() is deprecated

public class Main extends ListActivity {

    private static final int ACTIVITY_CREATE_TASK = 0;
    private static final int ACTIVITY_EDIT_TASK = 1;

    private static final int DIALOG_CONFIRM_DELETE_TASK_ID = 1;

    public static final int MENU_ID_ADD_TASK = Menu.FIRST;
    public static final int MENU_ID_EDIT_TASK = Menu.FIRST + 1;
    public static final int MENU_ID_DELETE_TASK = Menu.FIRST + 2;

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mTasksCursor;

    /**
     * Used for "Delete Task" operation.
     * 
     * TODO: rewrite it in a cleaner way, because this looks like a hack (the
     * reason behind that is that confirmation dialog can't receive parameters)
     */
    private long rowIdToDelete;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();
        fillTasksList();
        registerForContextMenu(getListView());
    }

    /**
     * Create main menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ID_ADD_TASK, 0, R.string.menu_add_task);
        return result;
    }

    /**
     * Main menu item clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case MENU_ID_ADD_TASK:
            runCreateTask();
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

        // Set menu title
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
        Cursor c = mTasksCursor;
        c.moveToPosition(info.position);
        String title = c.getString(c.getColumnIndexOrThrow(TaskPeer.KEY_TITLE));
        menu.setHeaderTitle(title);

        // Add menmu items
        menu.add(0, MENU_ID_EDIT_TASK, 0, R.string.menu_edit_task);
        menu.add(0, MENU_ID_DELETE_TASK, 1, R.string.menu_delete_task);
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

        case MENU_ID_EDIT_TASK:
            runEditTask(rowId);
            return true;

        case MENU_ID_DELETE_TASK:
            runDeleteTask(rowId);
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

        runEditTask(id);
    }

    /**
     * Process result from other activity (create/edit task etc.)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        fillTasksList();
    }


    /**
     * Creates dialogs (confirmations etc.)
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
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    doDeleteTask(rowIdToDelete);
                                    dialog.dismiss();
                                }
                            })

                    // "No" button
                    .setNegativeButton(R.string.button_no,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                        int which) {
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
     * Runs "Create Task" activity
     */
    private void runCreateTask() {
        Intent i = new Intent(this, TaskEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE_TASK);
    }

    /**
     * Runs "Edit Task" activity for the task with given ID
     *
     * @param rowId ID of the task to edit
     */
    private void runEditTask(long rowId) {
        Intent i = new Intent(this, TaskEdit.class);
        i.putExtra(TaskPeer.KEY_ID, rowId);
        startActivityForResult(i, ACTIVITY_EDIT_TASK);
    }

    /**
     * Launches confirmation dialog asking is user really wants to delete the
     * task, then deletes it if user confirms
     * 
     * @param rowId ID of the task to delete
     */
    private void runDeleteTask(long rowId) {
        rowIdToDelete = rowId;
        showDialog(DIALOG_CONFIRM_DELETE_TASK_ID);
    }

    /**
     * Actually deletes task with given ID and reloads the list of tasks.
     * Shouldn't be called directly, use runDeleteTask() instead.
     * 
     * @param rowId ID of the task to delete
     */
    private void doDeleteTask(long rowId) {

        // Delete the task
        mDbHelper.getTaskPeer().deleteTask(rowId);

        // Show message
        Toast toast = Toast.makeText(getApplicationContext(),
                R.string.message_task_deleted, Toast.LENGTH_SHORT);
        toast.show();

        // Update the list
        fillTasksList();

    }

    /**
     * Fills/reloads the list of tasks.
     */
    private void fillTasksList() {
        mTasksCursor = mDbHelper.getTaskPeer().fetchAllTasks();
        startManagingCursor(mTasksCursor);

        String[] from = new String[] { TaskPeer.KEY_TITLE };
        int[] to = new int[] { R.id.tasks_row_text };

        SimpleCursorAdapter tasks = new SimpleCursorAdapter(this,
                R.layout.tasks_row, mTasksCursor, from, to);
        setListAdapter(tasks);
    }
}
