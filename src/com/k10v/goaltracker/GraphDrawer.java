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
    int mDateRange;

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

    public void draw() {

        // Prepare line colors for different parst of the graph

        Paint paintProgress = new Paint();
        paintProgress.setARGB(255, 64, 255, 64);

        Paint paintAxes = new Paint();
        paintAxes.setARGB(255, 192, 192, 192);

        Paint paintCurrent = new Paint();
        paintCurrent.setARGB(255, 0, 64, 0);

        Paint paintDays = new Paint();
        paintDays.setARGB(255, 32, 32, 32);

        // Clear canvas
        mCanvas.drawColor(Color.BLACK);

        drawVerticalGrid(paintDays);
        drawCurrentValue(paintCurrent);
        drawAxes(paintAxes);
        drawProgress(paintProgress);
    }

    private void drawVerticalGrid(Paint paint) {
        for (int i = 0; i <= mDateRange; i++) {
            drawVerticalLineForDayN(i, paint);
        }
    }

    private void drawAxes(Paint paint) {
        drawHorizontalLineForValue(mStartValue, paint);
        drawVerticalLine(mCanvasXMin, paint);
    }

    private void drawCurrentValue(Paint paint) {
        drawHorizontalLineForValue(mLastValue, paint);
    }

    private void drawProgress(Paint paint) {

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

    private int getCanvasYByValue(float value) {
        return mCanvasYMax - Math.round(
                (mCanvasYMax - mCanvasYMin) *
                (value - mMinValue) /
                (mMaxValue - mMinValue));
    }

    private int getCanvasXByDayN(int n) {
        return mCanvasXMin + (mCanvasXMax - mCanvasXMin) * n / mDateRange;
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
