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

    private CanvasThread canvasthread;

    private Date minDate;
    private Date maxDate;

    private Float startValue = null;
    private Float finishValue = null;
    private Float minValue = null;
    private Float maxValue = null;

    private HashMap<Date, Float> values;

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
        canvasthread = new CanvasThread(getHolder(), this);
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        canvasthread.setRunning(true);
        canvasthread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        canvasthread.setRunning(false);
        while (retry) {
            try {
                canvasthread.join();
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
        Date date = minDate;

        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // TODO: Use JodaTime library for dates instead?
        int dateRange = 1 + Math.round((maxDate.getTime() - minDate.getTime()) / (24 * 60 * 60 * 1000f));

        float valueRange = maxValue - minValue;

        int i = 0;
        float value = startValue;
        int x0 = 0;
        int y0 = Math.round((height - 1) * (value / valueRange));

        // while (maxDate >= date)
        while (maxDate.compareTo(date) >= 0) {

            i++;

            // TODO: probably should use dateString as a key instead?
            if (values.containsKey(date)) {
                value = values.get(date);
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
        startValue = taskCursor.getFloat(taskCursor.getColumnIndexOrThrow(TaskPeer.KEY_START_VALUE));
        finishValue = taskCursor.getFloat(taskCursor.getColumnIndexOrThrow(TaskPeer.KEY_TARGET_VALUE));
        minValue = maxValue = startValue;

        // Process list of reports

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        values = new HashMap<Date, Float>(reportsCursor.getCount());

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

            values.put(date, value);

            if (value < minValue) {
                minValue = value;
            }

            if (maxValue < value) {
                maxValue = value;
            }

            if (minDate == null) {
                minDate = date;
            }

            maxDate = date;
        }

        if (finishValue == null) {
            finishValue = maxValue;
        }
    }
}
