package com.k10v.goaltracker;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

public class Main extends ListActivity {

    private static final int ACTIVITY_CREATE_TASK = 0;
    private static final int ACTIVITY_EDIT_TASK = 1;

    public static final int MENU_ID_ADD_TASK = Menu.FIRST;

    private GoalTrackerDbAdapter mDbHelper;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();
        fillTasksList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_ID_ADD_TASK, 0, R.string.menu_add_task);
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ID_ADD_TASK:
            createTask();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {

        super.onActivityResult(requestCode, resultCode, intent);
        Bundle extras = intent.getExtras();

        switch (requestCode) {

        case ACTIVITY_CREATE_TASK:
            onCreateTask(extras);
            break;

        case ACTIVITY_EDIT_TASK:
            onEditTask(extras);
            break;

        }
    }

    /**
     * Called after new task details are entered
     * 
     * @param extras Extra data returned by Intent
     */
    private void onCreateTask(Bundle extras) {
        String title = extras.getString(TaskPeer.KEY_TITLE);
        float startValue = extras.getFloat(TaskPeer.KEY_START_VALUE);
        // Target value can be null
        Float targetValue = extras.getFloat(TaskPeer.KEY_TARGET_VALUE,
                (Float) null);
        mDbHelper.getTaskPeer().createTask(title, startValue, targetValue);
        fillTasksList();
    }

    /**
     * Called after task details are changed
     * 
     * @param extras Extra data returned by Intent
     */
    private void onEditTask(Bundle extras) {
        Long rowId = extras.getLong(TaskPeer.KEY_ID);
        if (rowId != null) {
            String title = extras.getString(TaskPeer.KEY_TITLE);
            float startValue = extras.getFloat(TaskPeer.KEY_START_VALUE);
            // Target value can be null
            Float targetValue = extras.getFloat(TaskPeer.KEY_TARGET_VALUE,
                    (Float) null);
            mDbHelper.getTaskPeer().updateTask(rowId, title, startValue,
                    targetValue);
        }
        fillTasksList();
    }

    private void createTask() {
        Intent i = new Intent(this, TaskEdit.class);
        startActivityForResult(i, ACTIVITY_CREATE_TASK);
    }

    private void fillTasksList() {
        Cursor c = mDbHelper.getTaskPeer().fetchAllTasks();
        startManagingCursor(c);

        String[] from = new String[] { TaskPeer.KEY_TITLE };
        int[] to = new int[] { R.id.tasks_row_text };

        SimpleCursorAdapter tasks = new SimpleCursorAdapter(this,
                R.layout.tasks_row, c, from, to);
        setListAdapter(tasks);
    }
}
