package com.k10v.goaltracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.Toast;

abstract public class DeleteTaskDialogBuilder extends AlertDialog.Builder {

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
    public DeleteTaskDialogBuilder(Context context, GoalTrackerDbAdapter dbHelper, long rowId) {
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
        setMessage(R.string.message_confirm_delete_task);

        // "Yes" button
        setPositiveButton(R.string.button_yes,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        doDeleteTask();
                        afterDeleteTask();
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
     * Delete task from the database
     */
    private void doDeleteTask() {

        // Delete the task
        mDbHelper.getTaskPeer().deleteTask(mRowId);

        // Show message
        Toast toast = Toast.makeText(mContext.getApplicationContext(),
                R.string.message_task_deleted, Toast.LENGTH_SHORT);
        toast.show();

    }

    /**
     * Actions that should be performed after task is deleted
     */
    abstract public void afterDeleteTask();
}
