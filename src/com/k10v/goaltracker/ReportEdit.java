package com.k10v.goaltracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ReportEdit extends Activity {

    private static final int DIALOG_SELECT_DATE_ID = 0;
    private static final int DIALOG_CONFIRM_DELETE_REPORT_ID = 1;

    public static final int MENU_ID_DELETE_REPORT = Menu.FIRST;

    private GoalTrackerDbAdapter mDbHelper;
    private Cursor mReportCursor;

    private Long mRowId;
    private Long mTaskId;
    private Button mDatePicker;
    private EditText mValueText;
    private TextView mDateError;
    private Button mSaveButton;

    private Calendar mCalendar;

    private boolean mFormChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // Prepare DB adapter
        mDbHelper = new GoalTrackerDbAdapter(this);
        mDbHelper.open();

        // Setup the View
        setContentView(R.layout.report_edit);

        // Find form inputs
        mDatePicker = (Button) findViewById(R.id.report_date_picker);
        mValueText = (EditText) findViewById(R.id.report_value);
        mDateError = (TextView) findViewById(R.id.report_error_date);
        mSaveButton = (Button) findViewById(R.id.button_save_report);

        // Add a click listener to the date picker button
        mDatePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_SELECT_DATE_ID);
            }
        });

        mCalendar = Calendar.getInstance(); // Is set to current date by default

        // Retrieve report data: first check if it's stored in saved state, if
        // not then check Intent's extras. If ID is empty, we will create a new
        // report rather than edit an existing one
        if (savedInstanceState != null) {

            mRowId = (Long) savedInstanceState.getSerializable(ReportPeer.KEY_ID);
            mTaskId = (Long) savedInstanceState.getSerializable(ReportPeer.KEY_TASK_ID);
            mCalendar = (Calendar) savedInstanceState.getSerializable(ReportPeer.KEY_DATE);

        } else {

            Bundle extras = getIntent().getExtras();
            if (extras != null) {

                mRowId = extras.getLong(ReportPeer.KEY_ID);

                if (mRowId == 0) {

                    // Add new report (task ID must be provided in this case)

                    mRowId = null;
                    mTaskId = extras.getLong(ReportPeer.KEY_TASK_ID);

                } else {

                    // Edit existing report

                    // Load database data for an existing report
                    Cursor c = mReportCursor = mDbHelper.getReportPeer().fetchReport(mRowId);
                    startManagingCursor(mReportCursor);
                    mTaskId = c.getLong(c.getColumnIndexOrThrow(ReportPeer.KEY_TASK_ID));

                    // Convert date from SQL format to a Calendar object
                    String dateString = c.getString(c.getColumnIndexOrThrow(ReportPeer.KEY_DATE));
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    try {
                        mCalendar.setTime(df.parse(dateString));
                    } catch (ParseException e) {
                        // Use default date
                    }

                }

            }
        }

        // Set title
        if (mRowId == null) {
            setTitle(R.string.title_add_report);
        } else {
            setTitle(R.string.title_edit_report);
        }

        populateFields();

        setupListeners();

        updateDisplay();
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
        if (mRowId != null) {
            menu.add(0, MENU_ID_DELETE_REPORT, 0, R.string.menu_delete_report)
                    .setIcon(android.R.drawable.ic_menu_delete);
        }
        return result;
    }

    /**
     * Main menu item clicked
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case MENU_ID_DELETE_REPORT:
            runDeleteReport();
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called when activity state should be temporarily saved, for example when
     * activity is about to be killed in order to retrieve system resources
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(ReportPeer.KEY_ID, mRowId);
        outState.putSerializable(ReportPeer.KEY_TASK_ID, mTaskId);
        outState.putSerializable(ReportPeer.KEY_DATE, mCalendar);
        // We don't need to save form values because this is handled
        // automatically for every View object
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {

        case DIALOG_SELECT_DATE_ID:

            DatePickerDialog.OnDateSetListener dateSetListener =
                new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            mFormChanged = true;
                            mCalendar.set(year, monthOfYear, dayOfMonth);
                            updateDisplay();
                        }
                    };

            return new DatePickerDialog(this, dateSetListener,
                    mCalendar.get(Calendar.YEAR),
                    mCalendar.get(Calendar.MONTH),
                    mCalendar.get(Calendar.DAY_OF_MONTH));

        case DIALOG_CONFIRM_DELETE_REPORT_ID:
            DeleteReportDialogBuilder builder = new DeleteReportDialogBuilder(this, mDbHelper, mRowId) {
                @Override
                public void afterDeleteReport() {
                    finish();
                }
            };
            return builder.create();

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

        mValueText.addTextChangedListener(onFormChangeListener);
    }

    /**
     * Update display date
     */
    private void updateDisplay() {
        String dateString = android.text.format.DateFormat.format("E, MMM d, yyyy", mCalendar).toString();
        mDatePicker.setText(dateString);

        // Checks if report with given date already exists; if so, displays
        // error message and disables "Save" button
        if (reportWithCurrentDateExists()) {
            mDateError.setVisibility(View.VISIBLE);
            mSaveButton.setEnabled(false);
        } else {
            mDateError.setVisibility(View.GONE);
            mSaveButton.setEnabled(true);
        }
    }

    /**
     * Check whether a report with the current date already exists
     *
     * @return True if report with the current date exists, false otherwise
     */
    private boolean reportWithCurrentDateExists() {
        boolean exists = false;
        String sqlDateString = android.text.format.DateFormat.format("yyyy-MM-dd", mCalendar).toString();
        Cursor reportCursor = mDbHelper.getReportPeer().fetchReportByTaskIdAndDate(mTaskId, sqlDateString);
        startManagingCursor(reportCursor);
        if (reportCursor != null && reportCursor.getCount() > 0) {
            long rowId = reportCursor.getLong(reportCursor.getColumnIndexOrThrow(ReportPeer.KEY_ID));
            if (mRowId == null || mRowId != rowId) {
                exists = true;
            }
        }
        if (reportCursor != null) {
            reportCursor.close();
        }
        return exists;
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

        if (reportWithCurrentDateExists()) {
            // Can't save this case
            return;
        }

        boolean isNewReport = (mRowId == null);

        // Get field values from the form elements

        // Date
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String date = df.format(mCalendar.getTime());

        // Value
        Double value;
        try {
            value = Double.valueOf(mValueText.getText().toString());
        } catch (NumberFormatException e) {
            value = 0.0;
        }

        // Create/update report
        boolean isSaved;
        if (isNewReport) {
            mRowId = mDbHelper.getReportPeer().createReport(mTaskId, date, value);
            isSaved = (mRowId != 0);
        } else {
            isSaved = mDbHelper.getReportPeer().updateReport(mRowId, date, value);
        }

        // Show toast notification
        if (isSaved) {
            int toastMessageId = isNewReport ?
                    R.string.message_report_created :
                    R.string.message_report_updated;
            Toast toast = Toast.makeText(getApplicationContext(), toastMessageId, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Launches confirmation dialog asking is user really wants to delete the
     * report, then deletes it if user confirms
     */
    private void runDeleteReport() {
        showDialog(DIALOG_CONFIRM_DELETE_REPORT_ID);
    }
}
