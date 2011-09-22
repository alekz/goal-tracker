package com.k10v.goaltracker;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// TODO: handle empty/invalid form values
// TODO: don't show "Report updated" message if nothing has changed

public class ReportEdit extends Activity {

    private GoalTrackerDbAdapter mDbHelper;

    private Long mRowId;
    private Long mTaskId;
    private EditText mDateText;
    private EditText mValueText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Prepare DB adapter
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();

        // Setup the View
        setContentView(R.layout.report_edit);
        setTitle(R.string.title_edit_report);

        // Find form inputs
        mDateText = (EditText) findViewById(R.id.report_date);
        mValueText = (EditText) findViewById(R.id.report_value);

        // Retrieve report ID: first check if it's stored in saved state, if not
        // then check Intent's extras. If ID is empty, we will create a new
        // report rather than edit an existing one
        if (savedInstanceState != null) {
            mRowId = (Long) savedInstanceState
                    .getSerializable(ReportPeer.KEY_ID);
            mTaskId = (Long) savedInstanceState
                    .getSerializable(ReportPeer.KEY_TASK_ID);
        } else {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mRowId = extras.getLong(ReportPeer.KEY_ID);
                mTaskId = extras.getLong(ReportPeer.KEY_TASK_ID);
            }
        }
        if (mRowId == 0) {
            mRowId = null;
        }

        populateFields();

        setupListeners();
    }

    /**
     * Called when activity state should be temporarily saved, for example when
     * activity is about to be killed in order to retrieve system resources
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ReportPeer.KEY_ID, mRowId);
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

        Cursor c = mDbHelper.getReportPeer().fetchReport(mRowId);
        startManagingCursor(c);
        mDateText.setText(c.getString(
                c.getColumnIndexOrThrow(ReportPeer.KEY_DATE)));
        mValueText.setText(c.getString(
                c.getColumnIndexOrThrow(ReportPeer.KEY_VALUE)));
    }

    /**
     * Set up listeners for form buttons
     */
    private void setupListeners() {

        // "Save" button
        Button confirmButton = (Button) findViewById(R.id.button_save_report);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                saveForm();
                finish(); // eventually calls onPause() and onStop()
            }
        });

        // "Cancel" button
        Button cancelButton = (Button) findViewById(R.id.button_cancel_edit_report);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish(); // eventually calls onPause() and onStop()
            }
        });

    }

    /**
     * Save the form when "Back" key pressed
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveForm();
    }

    /**
     * Save form data to the database
     */
    private void saveForm() {

        // Get field values from the form elements
        String date = mDateText.getText().toString();
        Double value = Double.valueOf(mValueText.getText().toString());

        // Create or update the report
        boolean isNewReport = (mRowId == null);
        if (isNewReport) {
            long id = mDbHelper.getReportPeer().createReport(
                    mTaskId, date, value);
            if (id > 0) {
                mRowId = id;
            }
        } else {
            mDbHelper.getReportPeer().updateReport(mRowId, date, value);
        }

        // Show toast notification
        int toastMessageId = isNewReport ?
                R.string.message_report_created :
                R.string.message_report_updated;
        Toast toast = Toast.makeText(getApplicationContext(), toastMessageId,
                Toast.LENGTH_SHORT);
        toast.show();
    }
}
