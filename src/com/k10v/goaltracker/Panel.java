package com.k10v.goaltracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class Panel extends SurfaceView implements SurfaceHolder.Callback {

    private CanvasThread mCanvasThread;

    private Date mMinDate;
    private Date mMaxDate;

    private Float mStartValue = null;
    private Float mLastValue = null;
    private Float mTargetValue = null;
    private Float mMinValue = null;
    private Float mMaxValue = null;

    private HashMap<Date, Float> mValues;

    private boolean mIsTouched = false;
    private float mPointer1X = 0;
    private float mPointer1Y = 0;
    private float mPointer2X = 0;
    private float mPointer2Y = 0;

    private GraphDrawer mGraph;

    /**
     * When true, indicates that graph should be updated
     */
    private boolean mUpdate = false;

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        setFocusable(true);

        mGraph = new GraphDrawer(getContext());
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        redraw();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        redraw();
        mCanvasThread = new CanvasThread(getHolder(), this);
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

        // Check if graph should be updated
        if (!needsUpdate()) {
            return;
        }
        mUpdate = false;

        // Don't have any reports yet?
        if (mValues.size() == 0) {
            return;
        }

        // If task has a target value which is not reached yet, consider using
        // target value as a minimum/maximum value; also use today as a maximum
        // date
        float minValue = mMinValue;
        float maxValue = mMaxValue;
        Date maxDate = mMaxDate;

        if (mTargetValue != null && mMaxValue < mTargetValue) {

            maxValue = mTargetValue;

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date today = calendar.getTime();

            if (today.compareTo(maxDate) > 0) {
                maxDate = today;
            }
        }

        if (mTargetValue != null && mTargetValue < mMinValue) {
            minValue = mTargetValue;
        }

        mGraph.setCanvas(canvas);
        mGraph.setCanvasRectangle(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
        mGraph.setValues(mValues);
        mGraph.setStartValue(mStartValue);
        mGraph.setLastValue(mLastValue);
        mGraph.setValueRange(minValue, maxValue);
        mGraph.setDateRange(mMinDate, maxDate);
        mGraph.setPointer(mIsTouched, mPointer1X, mPointer1Y, mPointer2X, mPointer2Y);
        mGraph.draw();
    }

    /**
     * Provide canvas with the task/reports details from the database
     *
     * @param taskCursor
     * @param reportsCursor
     */
    public void setData(Cursor taskCursor, Cursor reportsCursor) {

        // Reset values
        mMinDate = null;
        mMaxDate = null;

        // Get information about the task
        mMinValue = mMaxValue = mStartValue = taskCursor.getFloat(
                taskCursor.getColumnIndexOrThrow(TaskPeer.KEY_START_VALUE));
        mTargetValue = taskCursor.getFloat(
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

            mLastValue = value;

            if (mMinDate == null) {
                mMinDate = date;
            }

            mMaxDate = date;
        }

        redraw();
    }

    /**
     * Ask graph to redraw itself
     */
    public void redraw() {
        mUpdate = true;
    }

    /**
     * Returns true if graph should be redrawn
     *
     * @return
     */
    public boolean needsUpdate() {
        return mUpdate;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        mPointer1X = event.getX(0);
        mPointer1Y = event.getY(0);
        if (event.getPointerCount() > 1) {
            mPointer2X = event.getX(1);
            mPointer2Y = event.getY(1);
        } else {
            mPointer2X = mPointer1X;
            mPointer2Y = mPointer1Y;
        }

        switch (event.getAction()) {

        case MotionEvent.ACTION_DOWN:
            mIsTouched = true;
            redraw();
            return true;

        case MotionEvent.ACTION_UP:
            mIsTouched = false;
            redraw();
            return true;

        case MotionEvent.ACTION_POINTER_DOWN:
        case MotionEvent.ACTION_POINTER_UP:
        case MotionEvent.ACTION_MOVE:
            redraw();
            return true;
        }

        return super.onTouchEvent(event);
    }
}
