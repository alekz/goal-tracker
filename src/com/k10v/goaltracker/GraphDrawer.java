package com.k10v.goaltracker;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Class responsible for drawing a graph on the canvas
 */
public class GraphDrawer {

    private Canvas mCanvas;

    private int mCanvasXMin;
    private int mCanvasXMax;
    private int mCanvasYMin;
    private int mCanvasYMax;

    private HashMap<Date, Float> mValues;

    private float mMinValue;
    private float mMaxValue;

    private float mStartValue;
    private float mLastValue;

    private Date mMinDate;
    private Date mMaxDate;
    private int mDateRange;

    private boolean mIsTouched = false;
    private float mPointer1X = 0;
    private float mPointer1Y = 0;
    private float mPointer2X = 0;
    private float mPointer2Y = 0;

    public void setCanvas(Canvas canvas) {
        mCanvas = canvas;
    }

    public void setCanvasRectangle(int xMin, int yMin, int xMax, int yMax) {
        mCanvasXMin = xMin;
        mCanvasYMin = yMin;
        mCanvasXMax = xMax;
        mCanvasYMax = yMax;
    }

    public void setValues(HashMap<Date, Float> values) {
        mValues = values;
    }

    public void setValueRange(float minValue, float maxValue) {
        mMinValue = minValue;
        mMaxValue = maxValue;
    }

    public void setDateRange(Date minDate, Date maxDate) {
        mMinDate = minDate;
        mMaxDate = maxDate;
        // TODO: Use JodaTime library for dates instead?
        mDateRange = 1 + Math.round(
                (maxDate.getTime() - minDate.getTime()) /
                (24 * 60 * 60 * 1000f));
    }

    public void setStartValue(float value) {
        mStartValue = value;
    }

    public void setLastValue(float value) {
        mLastValue = value;
    }

    public void setPointer(boolean isTouched, float x1, float y1, float x2, float y2) {
        mIsTouched = isTouched;
        mPointer1X = x1;
        mPointer1Y = y1;
        mPointer2X = x2;
        mPointer2Y = y2;
    }

    public void draw() {

        mCanvas.drawColor(Color.BLACK);

        drawVerticalGrid();
        drawCurrentValue();
        drawPointer();
        drawAxes();
        drawProgress();
    }

    private void drawVerticalGrid() {

        Paint paint = new Paint();
        paint.setARGB(255, 32, 32, 32);

        for (int i = 0; i <= mDateRange; i++) {
            drawVerticalLineForDayN(i, paint);
        }
    }

    private void drawAxes() {

        Paint paint = new Paint();
        paint.setARGB(255, 192, 192, 192);

        drawHorizontalLineForValue(mStartValue, paint);
        drawVerticalLine(mCanvasXMin, paint);
    }

    private void drawCurrentValue() {

        Paint paint = new Paint();
        paint.setARGB(255, 0, 64, 0);

        drawHorizontalLineForValue(mLastValue, paint);
    }

    private void drawProgress() {

        Paint paint = new Paint();
        paint.setARGB(255, 64, 255, 64);

        Calendar cal = Calendar.getInstance();
        Date date = mMinDate;
        float value = mStartValue;
        int x0 = getCanvasXByDayN(0);
        int y0 = getCanvasYByValue(mStartValue);

        for (int i = 1; i <= mDateRange; i++) {

            // TODO: probably should use dateString as a key instead?
            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            int x = getCanvasXByDayN(i);
            int y = getCanvasYByValue(value);

            // Draw progress
            mCanvas.drawLine(x0, y0, x, y, paint);

            // Next date
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();

            x0 = x;
            y0 = y;
        }
    }

    private void drawPointer() {

        if (!mIsTouched) {
            return;
        }

        // Find the dates below the pointers
        int day1N = getDayNByCanvasX(Math.round(mPointer1X));
        int day2N = getDayNByCanvasX(Math.round(mPointer2X));

        // Make sure day 1 is earlier than day 2
        if (day2N < day1N) {
            int temp = day1N;
            day1N = day2N;
            day2N = temp;
        }

        // Check is pointer 1 is inside the graph
        if (day1N <= 0 || mDateRange < day1N) {
            return;
        }

        // Make sure pointer 2 is inside the graph
        if (day2N <= 0) {
            day2N = 1;
        }
        if (mDateRange < day2N) {
            day2N = mDateRange;
        }

        Calendar cal = Calendar.getInstance();
        Date date = mMinDate;

        float value = mStartValue;
        float value1 = value;
        float value2 = value;

        for (int i = 1; i <= day2N; i++) {

            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            if (i == day1N - 1) {
                value1 = value;
            }

            if (i == day2N) {
                value2 = value;
            }

            // Next date
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();
        }

        // X-coordinates of two vertical lines
        int x1 = getCanvasXByDayN(day1N - 1);
        int x2 = getCanvasXByDayN(day2N);

        // Y-coordinates of two horizontal lines
        int y1, y2;
        boolean isNegativeDifference = false;
        if (value1 <= value2) {
            y1 = getCanvasYByValue(value2);
            y2 = getCanvasYByValue(value1);
        } else {
            y1 = getCanvasYByValue(value1);
            y2 = getCanvasYByValue(value2);
            isNegativeDifference = true;
        }

        Paint paint = new Paint();

        // Vertical fill
        paint.setARGB(255, 128, 128, 128);
        mCanvas.drawRect(x1, mCanvasYMin, x2 + 1, mCanvasYMax + 1, paint);

        // Horizontal fill
        if (isNegativeDifference) {
            paint.setARGB(192, 192, 96, 96);
        } else {
            paint.setARGB(192, 64, 128, 64);
        }
        mCanvas.drawRect(mCanvasXMin, y1, mCanvasXMax + 1, y2 + 1, paint);
    }

    private int getCanvasYByValue(float value) {
        return mCanvasYMax - Math.round(
                (mCanvasYMax - mCanvasYMin) *
                (value - mMinValue) /
                (mMaxValue - mMinValue));
    }

    private int getCanvasXByDayN(int n) {
        return mCanvasXMin + (mCanvasXMax - mCanvasXMin) * n / mDateRange;
    }

    private int getDayNByCanvasX(float x) {
        return (int) Math.ceil((x - mCanvasXMin) * mDateRange / (mCanvasXMax - mCanvasXMin));
    }

    private void drawHorizontalLine(int y, Paint paint) {
        mCanvas.drawLine(mCanvasXMin, y, mCanvasXMax + 1, y, paint);
    }

    private void drawHorizontalLineForValue(float value, Paint paint) {
        drawHorizontalLine(getCanvasYByValue(value), paint);
    }

    private void drawVerticalLine(int x, Paint paint) {
        mCanvas.drawLine(x, mCanvasYMin, x, mCanvasYMax + 1, paint);
    }

    private void drawVerticalLineForDayN(int n, Paint paint) {
        drawVerticalLine(getCanvasXByDayN(n), paint);
    }
}
