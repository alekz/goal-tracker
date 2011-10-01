package com.k10v.goaltracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Panel extends SurfaceView implements SurfaceHolder.Callback {

    private CanvasThread mCanvasThread;

    private Date mMinDate;
    private Date mMaxDate;

    private Float mStartValue = null;
    private Float mFinishValue = null;
    private Float mTargetValue = null;
    private Float mMinValue = null;
    private Float mMaxValue = null;

    private HashMap<Date, Float> mValues;

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        mCanvasThread = new CanvasThread(getHolder(), this);
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCanvasThread.setRunning(true);
        mCanvasThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        mCanvasThread.setRunning(false);
        while (retry) {
            try {
                mCanvasThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }
    }

    @Override
    public void onDraw(Canvas canvas) {

        Paint paint = new Paint();
        paint.setARGB(255, 255, 255, 255);

        Calendar cal = Calendar.getInstance();
        Date date = mMinDate;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // If task has a target value which is not reached yet, use target value
        // as a maximum value; also use today as a maximum date
        float maxValue = mMaxValue;
        Date maxDate = mMaxDate;
        if (mTargetValue != null && mMaxValue < mTargetValue) {

            maxValue = mTargetValue;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            maxDate = calendar.getTime();
        }

        // TODO: Use JodaTime library for dates instead?
        int dateRange = 1 + Math.round((maxDate.getTime() - mMinDate.getTime()) / (24 * 60 * 60 * 1000f));
        float valueRange = maxValue - mMinValue;

        int i = 0;
        float value = mStartValue;
        int x0 = 0;
        int y0 = Math.round((height - 1) * (value / valueRange));

        // while (maxDate >= date)
        while (maxDate.compareTo(date) >= 0) {

            i++;

            // TODO: probably should use dateString as a key instead?
            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            int x = (width - 1) * i / dateRange;
            int y = Math.round((height - 1) * (value / valueRange));

            canvas.drawLine(x0, height - y0 - 1, x, height - y - 1, paint);

            // Next date
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();

            x0 = x;
            y0 = y;
        }
    }

    /**
     * Provide canvas with the task/reports details from the database
     *
     * @param taskCursor
     * @param reportsCursor
     */
    public void setData(Cursor taskCursor, Cursor reportsCursor) {

        // Get information about the task
        mMinValue = mMaxValue = mStartValue = taskCursor.getFloat(
                taskCursor.getColumnIndexOrThrow(TaskPeer.KEY_START_VALUE));
        mFinishValue = mTargetValue = taskCursor.getFloat(
                taskCursor.getColumnIndexOrThrow(TaskPeer.KEY_TARGET_VALUE));

        // Process list of reports

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        mValues = new HashMap<Date, Float>(reportsCursor.getCount());

        reportsCursor.moveToFirst();

        while (!reportsCursor.isAfterLast()) {

            // Get values from the cursor
            Float value = reportsCursor.getFloat(reportsCursor.getColumnIndexOrThrow(ReportPeer.KEY_VALUE));
            String dateString = reportsCursor.getString(reportsCursor.getColumnIndexOrThrow(ReportPeer.KEY_DATE));
            Date date = null;

            reportsCursor.moveToNext();

            try {
                date = df.parse(dateString);
            } catch (ParseException e) {
                // Ignore records with invalid date
                continue;
            }

            mValues.put(date, value);

            if (value < mMinValue) {
                mMinValue = value;
            }

            if (mMaxValue < value) {
                mMaxValue = value;
            }

            if (mMinDate == null) {
                mMinDate = date;
            }

            mMaxDate = date;
        }

        if (mFinishValue == null) {
            mFinishValue = mMaxValue;
        }
    }
}
