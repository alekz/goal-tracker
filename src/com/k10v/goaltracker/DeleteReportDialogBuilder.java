package com.k10v.goaltracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

abstract public class DeleteReportDialogBuilder extends AlertDialog.Builder {

    Context mContext;
    GoalTrackerDbAdapter mDbHelper;
    long mRowId;

    /**
     * Initialize the dialog
     *
     * @param context
     * @param dbHelper Database helper that is used to delete record
     * @param rowId ID of record to delete
     */
    public DeleteReportDialogBuilder(Context context, GoalTrackerDbAdapter dbHelper, long rowId) {
        super(context);
        mContext = context;
        mDbHelper = dbHelper;
        mRowId = rowId;
        build();
    }

    /**
     * Build the dialog
     */
    private void build() {

        // Title
        setMessage(R.string.message_confirm_delete_report);

        // "Yes" button
        setPositiveButton(R.string.button_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doDeleteReport();
                        afterDeleteReport();
                    }
                });

        // "No" button
        setNegativeButton(R.string.button_no,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

    }

    /**
     * Delete report from the database
     */
    private void doDeleteReport() {

        // Delete the report
        mDbHelper.getReportPeer().deleteReport(mRowId);

        // Show message
        Toast toast = Toast.makeText(mContext.getApplicationContext(),
                R.string.message_report_deleted, Toast.LENGTH_SHORT);
        toast.show();

    }

    /**
     * Actions that should be performed after report is deleted
     */
    abstract public void afterDeleteReport();
}
