package com.k10v.goaltracker;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class TaskEdit extends Activity {

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mTaskCursor;

    private Long mRowId;
    private EditText mTitleText;
    private EditText mStartValueText;
    private EditText mTargetValueText;
    private boolean mFormChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Prepare DB adapter
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();

        // Setup the View
        setContentView(R.layout.task_edit);

        // Find form inputs
        mTitleText = (EditText) findViewById(R.id.task_title);
        mStartValueText = (EditText) findViewById(R.id.task_start_value);
        mTargetValueText = (EditText) findViewById(R.id.task_target_value);

        // Retrieve task ID: first check if it's stored in saved state, if not
        // then check Intent's extras. If ID is empty, we will create a new task
        // rather than edit an existing one
        mRowId = (savedInstanceState == null) ? null :
                (Long) savedInstanceState.getSerializable(TaskPeer.KEY_ID);
        if (mRowId == null) {
            Bundle extras = getIntent().getExtras();
            mRowId = (extras == null) ? null : extras.getLong(TaskPeer.KEY_ID);
        }

        // Set title
        if (mRowId == null) {
            setTitle(R.string.title_add_task);
        } else {
            setTitle(R.string.title_edit_task);
        }

        populateFields();

        setupListeners();
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
        outState.putSerializable(TaskPeer.KEY_ID, mRowId);
        // We don't need to save form values because this is handled
        // automatically for every View object
    }

    /**
     * Fill form with data from the database
     */
    private void populateFields() {

        if (mRowId == null) {
            return;
        }

        Cursor c = mDbHelper.getTaskPeer().fetchTask(mRowId);
        startManagingCursor(c);
        mTitleText.setText(c.getString(c.getColumnIndexOrThrow(TaskPeer.KEY_TITLE)));
        mStartValueText.setText(c.getString(c.getColumnIndexOrThrow(TaskPeer.KEY_START_VALUE)));
        mTargetValueText.setText(c.getString(c.getColumnIndexOrThrow(TaskPeer.KEY_TARGET_VALUE)));
        mTaskCursor = c;
    }

    /**
     * Set up listeners for form buttons
     */
    private void setupListeners() {

        // "Save" button
        Button confirmButton = (Button) findViewById(R.id.button_save_task);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                saveForm();
                finish(); // eventually calls onPause() and onStop()
            }
        });

        // "Cancel" button
        Button cancelButton = (Button) findViewById(R.id.button_cancel_edit_task);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish(); // eventually calls onPause() and onStop()
            }
        });

        TextWatcher onFormChangeListener = new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mFormChanged = true;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        mTitleText.addTextChangedListener(onFormChangeListener);
        mStartValueText.addTextChangedListener(onFormChangeListener);
        mTargetValueText.addTextChangedListener(onFormChangeListener);
    }

    /**
     * Save the form when "Back" key pressed
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mFormChanged) {
            saveForm();
        }
    }

    /**
     * Save form data to the database
     */
    private void saveForm() {

        boolean isNewTask = (mRowId == null);

        // Get field values from the form elements (use sensible defaults in
        // case of invalid values)

        // Title
        String title = mTitleText.getText().toString().trim();
        if (title.equals("")) {
            if (isNewTask) {
                title = getString(R.string.task_default_title);
            } else {
                // If the new title is empty, use the old one
                title = mTaskCursor.getString(mTaskCursor.getColumnIndexOrThrow(TaskPeer.KEY_TITLE));
            }
        }

        // Start value
        Double startValue;
        try {
            startValue = Double.valueOf(mStartValueText.getText().toString());
        } catch (NumberFormatException e) {
            startValue = 0.0;
        }

        // Target value
        Double targetValue;
        try {
            targetValue = Double.valueOf(mTargetValueText.getText().toString());
        } catch (NumberFormatException e) {
            targetValue = null;
        }

        // Create/update task
        if (isNewTask) {
            mRowId = mDbHelper.getTaskPeer().createTask(title, startValue, targetValue);
        } else {
            mDbHelper.getTaskPeer().updateTask(mRowId, title, startValue, targetValue);
        }

        // Show toast notification
        int toastMessageId = isNewTask ?
                R.string.message_task_created :
                R.string.message_task_updated;
        Toast toast = Toast.makeText(getApplicationContext(), toastMessageId, Toast.LENGTH_SHORT);
        toast.show();
    }
}
