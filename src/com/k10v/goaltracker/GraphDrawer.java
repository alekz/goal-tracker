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
    private final int mTickSize = 5;

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
        int startDayN = getDayNByCanvasX(Math.round(mPointer1X));
        int finishDayN = getDayNByCanvasX(Math.round(mPointer2X));

        // Make sure day 1 is earlier than day 2
        if (finishDayN < startDayN) {
            int temp = startDayN;
            startDayN = finishDayN;
            finishDayN = temp;
        }

        // Check is pointer 1 is inside the graph
        if (startDayN <= 0 || mDateRange < startDayN) {
            return;
        }

        // Make sure pointer 2 is inside the graph
        if (finishDayN <= 0) {
            finishDayN = 1;
        }
        if (mDateRange < finishDayN) {
            finishDayN = mDateRange;
        }

        Calendar cal = Calendar.getInstance();
        Date date = mMinDate;

        float value = mStartValue;
        float startValue = value;
        float finishValue = value;

        for (int i = 1; i <= finishDayN; i++) {

            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            if (i == startDayN - 1) {
                startValue = value;
            }

            if (i == finishDayN) {
                finishValue = value;
            }

            // Next date
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_MONTH, 1);
            date = cal.getTime();
        }

        float valueDiff = finishValue - startValue;

        // X-coordinates of two vertical lines
        int xMin = getCanvasXByDayN(startDayN - 1);
        int xMax = getCanvasXByDayN(finishDayN);

        // Y-coordinates of two horizontal lines
        int yMin, yMax;
        if (0 <= valueDiff) {
            yMin = getCanvasYByValue(finishValue);
            yMax = getCanvasYByValue(startValue);
        } else {
            yMin = getCanvasYByValue(startValue);
            yMax = getCanvasYByValue(finishValue);
        }

        // Vertical fill
        mCanvas.drawRect(xMin, mCanvasYMin, xMax + 1, mCanvasYMax + 1, mPaintSelectedDate);

        // Horizontal fill
        Paint paint = valueDiff < 0 ? mPaintSelectedValueNegative : mPaintSelectedValue;
        mCanvas.drawRect(mCanvasXMin, yMin, mCanvasXMax + 1, yMax + 1, paint);

        // Build pointer's labels

        String topLabel;
        String bottomLabel;

        if (0 < valueDiff) {
            bottomLabel = formatNumber(startValue);
            topLabel = formatNumber(finishValue) + " (" + formatNumber(valueDiff, true) + ")";
        } else if (valueDiff < 0) {
            topLabel = formatNumber(startValue);
            // "\u2212" is a proper "minus" sign
            bottomLabel = formatNumber(finishValue) + " (" + formatNumber(valueDiff, true) + ")";
        } else {
            topLabel = formatNumber(finishValue);
            // Show only one label if value has not changed
            bottomLabel = null;
        }

        // Calculate label's positions (when calculating labels' heights, we
        // don't really care about the real bounds of the labels, only about
        // height of a digit)
        Rect bounds = new Rect();
        mPaintLabels.getTextBounds("0", 0, 1, bounds);
        int topLabelY = yMin - bounds.bottom - mLabelTextMargin;
        int bottomLabelY = yMax - bounds.top + mLabelTextMargin;

        // Check labels' bounds; if they don't fit into the drawing area,
        // combine them into one label
        final String combineLabelsSeparator = "...";
        int labelHeight = bounds.bottom - bounds.top;
        if (topLabel != null && topLabelY - labelHeight - mLabelTextMargin < mCanvasYMin) {

            // Move top label to the bottom when it is too close to the border
            if (0 < valueDiff) {
                bottomLabel = bottomLabel + combineLabelsSeparator + topLabel;
            } else if (valueDiff < 0) {
                bottomLabel = topLabel + combineLabelsSeparator + bottomLabel;
            } else {
                bottomLabel = topLabel;
            }
            topLabel = null;

        } else if (bottomLabel != null && mCanvasYMax < bottomLabelY + mLabelTextMargin) {

            // Move bottom label to the top when it is too close to the border
            if (0 < valueDiff) {
                topLabel = bottomLabel + combineLabelsSeparator + topLabel;
            } else if (valueDiff < 0) {
                topLabel = topLabel + combineLabelsSeparator + bottomLabel;
            } else {
                // Nothing to change, we don't have bottom label in this case
            }
            bottomLabel = null;

        }

        // Draw the labels
        int labelX = mCanvasXMin + mTickSize + mLabelTextMargin;
        if (topLabel != null) {
            mCanvas.drawText(topLabel, labelX, topLabelY, mPaintLabels);
        }
        if (bottomLabel != null) {
            mCanvas.drawText(bottomLabel, labelX, bottomLabelY, mPaintLabels);
        }

        // Draw the ticks
        drawHorizontalTickForValue(finishValue);
        drawHorizontalTickForValue(startValue);
        drawVerticalTickForDayN(startDayN);
        drawVerticalTickForDayN(finishDayN);

        // TODO: temporarily (?) disabled
        // drawLabels();
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

            text = formatDate(mMinDate);
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

            drawVerticalTickForDayN(1);
        }

        // Max date
        if (drawMaxDate) {

            text = formatDate(mMaxDate);
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

            drawVerticalTickForDayN(mDateRange);
        }
    }

    private void drawLabels() {
        drawLabels(true, true, true, true);
    }

    // TODO: Move this utility method outside?
    private String formatDate(Date date) {

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

    // TODO: Move this utility method outside?
    private String formatNumber(float number, boolean withPlusSign) {
        final DecimalFormat format = new DecimalFormat("#.#####");
        if (0 < number) {
            return (withPlusSign ? "+" : "") + format.format(number);
        } else if (number < 0) {
            // "\u2212" is a proper "minus" sign
            return "\u2212" + format.format(-number);
        } else {
            return "0";
        }
    }

    private String formatNumber(float number) {
        return formatNumber(number, false);
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

        // It's always two ticks for one day
        int x;

        x = getCanvasXByDayN(n);
        mCanvas.drawLine(x, mCanvasYMax, x, mCanvasYMax + mTickSize, mPaintAxes);

        x = getCanvasXByDayN(n - 1);
        mCanvas.drawLine(x, mCanvasYMax, x, mCanvasYMax + mTickSize, mPaintAxes);
    }

    private void drawHorizontalTickForValue(float value) {
        int y = getCanvasYByValue(value);
        mCanvas.drawLine(mCanvasXMin, y, mCanvasXMin + mTickSize, y, mPaintAxes);
    }
}
