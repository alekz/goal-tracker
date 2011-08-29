package com.k10v.goaltracker;

import android.app.ListActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SimpleCursorAdapter;

public class Main extends ListActivity {

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

    private void createTask() {
        // ... Create task
        fillTasksList();
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
