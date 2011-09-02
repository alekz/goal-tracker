package com.k10v.goaltracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class TaskEdit extends Activity {

    private Long mRowId;
    private EditText mTitle;
    private EditText mStartValue;
    private EditText mTargetValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.task_edit);
        setTitle(R.string.title_edit_task);
        initForm();
        fillForm();
        setupListeners();
    }

    /**
     * Initialize form elements
     */
    private void initForm() {
        mRowId = null;
        mTitle = (EditText) findViewById(R.id.task_title);
        mStartValue = (EditText) findViewById(R.id.task_start_value);
        mTargetValue = (EditText) findViewById(R.id.task_target_value);
    }

    /**
     * Fill form with data from the Intent
     */
    private void fillForm() {

        Bundle extras = getIntent().getExtras();
        if (extras == null) {
            return;
        }

        mRowId = extras.getLong(TaskPeer.KEY_ID);

        String title = extras.getString(TaskPeer.KEY_TITLE);
        Long startValue = extras.getLong(TaskPeer.KEY_START_VALUE);
        Long targetValue = extras.getLong(TaskPeer.KEY_TARGET_VALUE);

        if (title != null) {
            mTitle.setText(title);
        }

        if (startValue != null) {
            mStartValue.setText(startValue.toString());
        }

        if (targetValue != null) {
            mTargetValue.setText(targetValue.toString());
        }
    }

    /**
     * Set up listeners for form buttons
     */
    private void setupListeners() {
        Button confirmButton = (Button) findViewById(R.id.button_save_task);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Prepare return data and finish the Activity

                Bundle extras = new Bundle();

                extras.putString(TaskPeer.KEY_TITLE,
                        mTitle.getText().toString());

                extras.putFloat(TaskPeer.KEY_START_VALUE,
                        Float.valueOf(mStartValue.getText().toString()));

                // TODO: target value can be null
                extras.putFloat(TaskPeer.KEY_TARGET_VALUE,
                        Float.valueOf(mTargetValue.getText().toString()));

                if (mRowId != null) {
                    extras.putLong(TaskPeer.KEY_ID, mRowId);
                }

                Intent intent = new Intent();
                intent.putExtras(extras);
                setResult(RESULT_OK, intent);
                finish();

            }
        });
    }
}
