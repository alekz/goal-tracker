package com.k10v.goaltracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

// TODO: handle empty/invalid form values
// TODO: don't show "Report updated" message if nothing has changed

public class ReportEdit extends Activity {

    static final int DATE_DIALOG_ID = 0;

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mReportCursor;

    private Long mRowId;
    private Long mTaskId;
    private Button mDatePicker;
    private EditText mValueText;

    private Calendar mCalendar;

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
        mDatePicker = (Button) findViewById(R.id.report_date_picker);
        mValueText = (EditText) findViewById(R.id.report_value);

        // Add a click listener to the date picker button
        mDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DATE_DIALOG_ID);
            }
        });

        mCalendar = Calendar.getInstance();

        // Retrieve report ID: first check if it's stored in saved state, if not
        // then check Intent's extras. If ID is empty, we will create a new
        // report rather than edit an existing one
        if (savedInstanceState != null) {

            mRowId = (Long) savedInstanceState.getSerializable(ReportPeer.KEY_ID);
            mTaskId = (Long) savedInstanceState.getSerializable(ReportPeer.KEY_TASK_ID);

        } else {

            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mRowId = extras.getLong(ReportPeer.KEY_ID);
                if (mRowId == 0) {
                    mRowId = null;
                }
                mTaskId = extras.getLong(ReportPeer.KEY_TASK_ID);
            }
        }

        if (mRowId != null) {

            // Load database data for an existing report
            mReportCursor = mDbHelper.getReportPeer().fetchReport(mRowId);
            startManagingCursor(mReportCursor);

            // Load date from saved instance state or from database
            if (savedInstanceState != null) {
                mCalendar = (Calendar) savedInstanceState.getSerializable(ReportPeer.KEY_DATE);
            } else {
                Cursor c = mReportCursor;
                String dateString = c.getString(c.getColumnIndexOrThrow(ReportPeer.KEY_DATE));
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                try {
                    mCalendar.setTime(df.parse(dateString));
                } catch (ParseException e) {
                    // Use default date
                }
            }
        }

        populateFields();

        setupListeners();

        updateDisplay();
    }

    /**
     * Called when activity state should be temporarily saved, for example when
     * activity is about to be killed in order to retrieve system resources
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ReportPeer.KEY_ID, mRowId);
        outState.putSerializable(ReportPeer.KEY_DATE, mCalendar);
        // We don't need to save form values because this is handled
        // automatically for every View object
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

        case DATE_DIALOG_ID:

            DatePickerDialog.OnDateSetListener dateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            mCalendar.set(year, monthOfYear, dayOfMonth);
                            updateDisplay();
                        }
                    };

            return new DatePickerDialog(this, dateSetListener,
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH));
        }
        return null;
    }

    /**
     * Fill form with data from the database
     */
    private void populateFields() {

        if (mReportCursor == null) {
            return;
        }

        Cursor c = mReportCursor;
        mValueText.setText(c.getString(c.getColumnIndexOrThrow(ReportPeer.KEY_VALUE)));
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
     * Update display date
     */
    private void updateDisplay() {
        String dateString = android.text.format.DateFormat.format("E, MMM d, yyyy", mCalendar).toString();
        mDatePicker.setText(dateString);
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
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(mCalendar.getTime());
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
