package com.k10v.goaltracker;

import android.app.ListActivity;
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

// TODO: ask confirmation when deleting task

public class Main extends ListActivity {

    private static final int ACTIVITY_CREATE_TASK = 0;
    private static final int ACTIVITY_EDIT_TASK = 1;

    public static final int MENU_ID_ADD_TASK = Menu.FIRST;
    public static final int MENU_ID_EDIT_TASK = Menu.FIRST + 1;
    public static final int MENU_ID_DELETE_TASK = Menu.FIRST + 2;

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mTasksCursor;

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
     * Deletes task with given ID and reloads the list of tasks.
     *
     * @param rowId ID of the task to delete
     */
    private void runDeleteTask(long rowId) {

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
