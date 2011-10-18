package com.k10v.goaltracker;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Class responsible for drawing a graph on the canvas
 */
public class GraphDrawer {

    private final int mLabelTextMargin = 5;
    private final int mLabelTextSize = 16;
    private final int mTickSize = 4;

    private Context context;

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

    private Paint mPaintVerticalGrid;
    private Paint mPaintAxes;
    private Paint mPaintCurrentValue;
    private Paint mPaintProgress;
    private Paint mPaintSelectedDate;
    private Paint mPaintSelectedValue;
    private Paint mPaintSelectedValueNegative;
    private Paint mPaintLabels;

    public GraphDrawer(Context c) {
        context = c;
        setupPaints();
    }

    private void setupPaints() {

        mPaintVerticalGrid = new Paint();
        mPaintVerticalGrid.setARGB(255, 32, 32, 32);

        mPaintAxes = new Paint();
        mPaintAxes.setARGB(255, 255, 255, 255);

        mPaintCurrentValue = new Paint();
        mPaintCurrentValue.setARGB(255, 0, 64, 0);

        mPaintProgress = new Paint();
        mPaintProgress.setARGB(255, 64, 255, 64);
        mPaintProgress.setStrokeWidth(1.5f);
        mPaintProgress.setAntiAlias(true);

        mPaintSelectedDate = new Paint();
        mPaintSelectedDate.setARGB(255, 128, 128, 128);

        mPaintSelectedValue = new Paint();
        mPaintSelectedValue.setARGB(192, 64, 128, 64);

        mPaintSelectedValueNegative = new Paint();
        mPaintSelectedValueNegative.setARGB(192, 192, 96, 96);

        mPaintLabels = new Paint();
        mPaintLabels.setARGB(255, 255, 255, 255);
        mPaintLabels.setTextSize(mLabelTextSize);
        mPaintLabels.setFakeBoldText(true);
        mPaintLabels.setAntiAlias(true);
        mPaintLabels.setShadowLayer(5, 0, 0, Color.BLACK);
    }

    public void setCanvas(Canvas canvas) {
        mCanvas = canvas;
    }

    public void setCanvasRectangle(int xMin, int yMin, int xMax, int yMax) {
        mCanvasXMin = xMin;
        mCanvasYMin = yMin;
        mCanvasXMax = xMax;
        mCanvasYMax = yMax - mLabelTextSize - 2 * mLabelTextMargin;
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
        drawPointerAndLabels();
        drawAxes();
        drawProgress();
    }

    private void drawVerticalGrid() {
        for (int i = 0; i <= mDateRange; i++) {
            drawVerticalLineForDayN(i, mPaintVerticalGrid);
        }
    }

    private void drawAxes() {
        drawHorizontalLineForValue(mStartValue, mPaintAxes);
        drawVerticalLine(mCanvasXMin, mPaintAxes);
    }

    private void drawCurrentValue() {
        drawHorizontalLineForValue(mLastValue, mPaintCurrentValue);
    }

    private void drawProgress() {
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
            mCanvas.drawLine(x0, y0, x, y, mPaintProgress);

            // Next date
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();

            x0 = x;
            y0 = y;
        }
    }

    private void drawPointerAndLabels() {

        if (!mIsTouched) {
            drawLabels();
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

        // Vertical fill
        mCanvas.drawRect(x1, mCanvasYMin, x2 + 1, mCanvasYMax + 1, mPaintSelectedDate);

        // Horizontal fill
        Paint paint = isNegativeDifference ? mPaintSelectedValueNegative : mPaintSelectedValue;
        mCanvas.drawRect(mCanvasXMin, y1, mCanvasXMax + 1, y2 + 1, paint);

        drawLabels();
    }

    private void drawLabels() {
        drawLabels(true, true, true, true);
    }

    private void drawLabels(
            boolean drawMinValue, boolean drawMaxValue,
            boolean drawMinDate, boolean drawMaxDate) {

        Rect bounds = new Rect();
        String text;
        DecimalFormat format = new DecimalFormat("#.#####");

        // Min value
        if (drawMinValue) {
            text = format.format(mMinValue);
            mPaintLabels.getTextBounds(text, 0, text.length(), bounds);
            mCanvas.drawText(text,
                    mCanvasXMin + mLabelTextMargin,
                    mCanvasYMax - bounds.bottom - mLabelTextMargin,
                    mPaintLabels);
            drawHorizontalTickForValue(mMinValue);
        }

        // Max value
        if (drawMaxValue) {
            text = format.format(mMaxValue);
            mPaintLabels.getTextBounds(text, 0, text.length(), bounds);
            mCanvas.drawText(text,
                    mCanvasXMin + mTickSize + mLabelTextMargin,
                    mCanvasYMin - bounds.top,
                    mPaintLabels);
            drawHorizontalTickForValue(mMaxValue);
        }

        // Min date
        if (drawMinDate) {

            text = getFormattedDate(mMinDate);
            mPaintLabels.getTextBounds(text, 0, text.length(), bounds);

            // Try to center the label against date column, but adjust label's
            // position if there is not enough space
            int x = (
                    (getCanvasXByDayN(0) + getCanvasXByDayN(1)) -
                    (bounds.right - bounds.left)
                    ) / 2;
            if (x < 0) {
                x = mCanvasXMin;
            }

            mCanvas.drawText(text,
                    x,
                    mCanvasYMax - bounds.top + mTickSize + mLabelTextMargin,
                    mPaintLabels);

            drawVerticalTickForDayN(0);
            drawVerticalTickForDayN(1);
        }

        // Max date
        if (drawMaxDate) {

            text = getFormattedDate(mMaxDate);
            mPaintLabels.getTextBounds(text, 0, text.length(), bounds);

            // Try to center the label against date column, but adjust label's
            // position if there is not enough space
            int w = bounds.right - bounds.left;
            int x = (
                    (getCanvasXByDayN(mDateRange - 1) + getCanvasXByDayN(mDateRange)) -
                    (bounds.right + bounds.left)
                    ) / 2;
            if (mCanvasXMax < x + w) {
                x = mCanvasXMax - bounds.right;
            }

            mCanvas.drawText(text,
                    x,
                    mCanvasYMax - bounds.top + mTickSize + mLabelTextMargin,
                    mPaintLabels);

            drawVerticalTickForDayN(mDateRange - 1);
            drawVerticalTickForDayN(mDateRange);
        }
    }

    // TODO: Move this utility method outside?
    private String getFormattedDate(Date date) {

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (calendar.equals(today)) {
            return context.getString(R.string.date_today);
        }

        String format = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) ? "MMM d" : "MMM d, yyyy";
        return android.text.format.DateFormat.format(format, date).toString();
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

    private void drawVerticalTickForDayN(int n) {
        int x = getCanvasXByDayN(n);
        mCanvas.drawLine(x, mCanvasYMax, x, mCanvasYMax + mTickSize, mPaintAxes);
    }

    private void drawHorizontalTickForValue(float value) {
        int y = getCanvasYByValue(value);
        mCanvas.drawLine(mCanvasXMin, y, mCanvasXMin + mTickSize, y, mPaintAxes);
    }
}
