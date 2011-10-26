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

    private Context mContext;

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
    private Float mTargetValue;

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

    // Used for different date conversions; made it a class member for
    // performance reasons, to avoid object creation overhead
    private Calendar mCalendar = Calendar.getInstance();

    public GraphDrawer(Context c) {
        mContext = c;
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
        mPaintSelectedDate.setARGB(128, 255, 255, 255);

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

    public void setTargetValue(Float value) {
        mTargetValue = value;
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
        if (mIsTouched) {
            drawPointer();
        } else {
            drawLabels();
        }
        drawAxes();
        drawProgress();
    }

    private void drawVerticalGrid() {
        int x0 = getCanvasXByDayN(-1);
        int x;
        for (int i = 0; i <= mDateRange; i++) {
            x = getCanvasXByDayN(i);
            if (x != x0) {
                drawVerticalLine(x, mPaintVerticalGrid);
            }
            x0 = x;
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
        float value = mStartValue;
        int x0 = getCanvasXByDayN(0);
        int y0 = getCanvasYByValue(mStartValue);

        for (int i = 1; i <= mDateRange; i++) {

            Date date = getDateByDayN(i);

            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            int x = getCanvasXByDayN(i);
            int y = getCanvasYByValue(value);

            // Draw progress
            if (x != x0 || y != y0) {
                mCanvas.drawLine(x0, y0, x, y, mPaintProgress);
            }

            x0 = x;
            y0 = y;
        }
    }

    private void drawPointer() {

        if (!mIsTouched) {
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

        // Find min/max values for the selected date range
        float value = mStartValue;
        float startValue = value;
        float finishValue = value;
        for (int i = 1; i <= finishDayN; i++) {

            Date date = getDateByDayN(i);

            if (mValues.containsKey(date)) {
                value = mValues.get(date);
            }

            if (i == startDayN - 1) {
                startValue = value;
            }

            if (i == finishDayN) {
                finishValue = value;
            }
        }

        // Difference in value between start and finish dates
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

        // == Value labels ==

        String topValueLabel;
        String bottomValueLabel;

        String topPercentageLabel;
        String bottomPercentageLabel;

        boolean showPercentageLabels = (mTargetValue != null) && (mTargetValue != mStartValue);
        float startPercentage = 0;
        float finishPercentage = 0;
        float percentageDiff = 0;

        if (showPercentageLabels) {
            startPercentage = Math.round(1000 * (startValue - mStartValue) / (mTargetValue - mStartValue)) / 10f;
            finishPercentage = Math.round(1000 * (finishValue - mStartValue) / (mTargetValue - mStartValue)) / 10f;
            percentageDiff = mTargetValue - mStartValue;
        }

        if (0 < valueDiff) {

            bottomValueLabel = Util.formatNumber(startValue);

            topValueLabel = Util.formatNumber(finishValue) +
                    " (" + Util.formatNumber(valueDiff, true) + ")";

            bottomPercentageLabel = Util.formatNumber(startPercentage) + "%";

            topPercentageLabel = Util.formatNumber(finishPercentage) + "%" +
                    " (" + Util.formatNumber(percentageDiff, true) + ")";

        } else if (valueDiff < 0) {

            topValueLabel = Util.formatNumber(startValue);

            bottomValueLabel = Util.formatNumber(finishValue) +
                    " (" + Util.formatNumber(valueDiff, true) + ")";

            topPercentageLabel = Util.formatNumber(startPercentage) + "%";

            bottomPercentageLabel = Util.formatNumber(finishPercentage) + "%" +
                    " (" + Util.formatNumber(percentageDiff, true) + ")";

        } else {

            topValueLabel = Util.formatNumber(finishValue);
            topPercentageLabel = Util.formatNumber(finishPercentage) + "%";

            // Show only one label if value has not changed
            bottomValueLabel = null;
            bottomPercentageLabel = null;

        }

        // Calculate label's positions (when calculating labels' heights, we
        // don't really care about the real bounds of the labels, only about
        // height of a digit)
        Rect digitBounds = new Rect();
        Rect bounds = new Rect();
        mPaintLabels.getTextBounds("0", 0, 1, digitBounds);
        int topLabelY = yMin - digitBounds.bottom - mLabelTextMargin;
        int bottomLabelY = yMax - digitBounds.top + mLabelTextMargin;

        // Check labels' bounds; if they don't fit into the drawing area,
        // combine them into one label
        final String combineLabelsSeparator = "...";
        int labelHeight = digitBounds.bottom - digitBounds.top;
        if (topValueLabel != null && topLabelY - labelHeight - mLabelTextMargin < mCanvasYMin) {

            // Move top label to the bottom when it is too close to the border
            if (0 < valueDiff) {
                bottomValueLabel = bottomValueLabel + combineLabelsSeparator + topValueLabel;
                bottomPercentageLabel = bottomPercentageLabel + combineLabelsSeparator + topPercentageLabel;
            } else if (valueDiff < 0) {
                bottomValueLabel = topValueLabel + combineLabelsSeparator + bottomValueLabel;
                bottomPercentageLabel = topPercentageLabel + combineLabelsSeparator + bottomPercentageLabel;
            } else {
                bottomValueLabel = topValueLabel;
                bottomPercentageLabel = topPercentageLabel;
            }
            topValueLabel = null;
            topPercentageLabel = null;

        } else if (bottomValueLabel != null && mCanvasYMax < bottomLabelY + mLabelTextMargin) {

            // Move bottom label to the top when it is too close to the border
            if (0 < valueDiff) {
                topValueLabel = bottomValueLabel + combineLabelsSeparator + topValueLabel;
                topPercentageLabel = bottomPercentageLabel + combineLabelsSeparator + topPercentageLabel;
            } else if (valueDiff < 0) {
                topValueLabel = topValueLabel + combineLabelsSeparator + bottomValueLabel;
                topPercentageLabel = topPercentageLabel + combineLabelsSeparator + bottomPercentageLabel;
            } else {
                // Nothing to change, we don't have bottom label in this case
            }
            bottomValueLabel = null;
            bottomPercentageLabel = null;

        }

        // == Date labels ==

        // Build pointer's date labels
        String leftDateLabel = null;
        String rightDateLabel = null;
        String middleDateLabel = null;

        int leftDateX = 0;
        int rightDateX = 0;
        int middleDateX = 0;

        if (startDayN == finishDayN) {

            // Only one day is selected

            middleDateLabel = Util.formatDate(getDateByDayN(finishDayN), mContext);

        } else {

            // Date range is selected

            leftDateLabel = Util.formatDate(getDateByDayN(startDayN), mContext);
            rightDateLabel = Util.formatDate(getDateByDayN(finishDayN), mContext);

            mPaintLabels.getTextBounds(rightDateLabel, 0, rightDateLabel.length(), bounds);
            rightDateX = getCanvasXByDayN(finishDayN) - bounds.right;

            mPaintLabels.getTextBounds(leftDateLabel, 0, leftDateLabel.length(), bounds);
            leftDateX = getCanvasXByDayN(startDayN - 1) - bounds.left;
            int leftDateWidth = bounds.right - bounds.left;

            // Check if left and right labels are located too close to each
            // other or even overlap
            if (rightDateX <= leftDateX + leftDateWidth + 2 * mLabelTextMargin) {
                // In this case combine them to a single label
                // "\u2013" - endash; "\u2014" - emdash
                middleDateLabel = leftDateLabel + " \u2014 " + rightDateLabel;
                leftDateLabel = null;
                rightDateLabel = null;
            }

        }

        // Calculate and correct (if necessary) position of the middle label
        if (middleDateLabel != null) {
            mPaintLabels.getTextBounds(middleDateLabel, 0, middleDateLabel.length(), bounds);
            int middleDateWidth = bounds.right - bounds.left;
            middleDateX = (
                    getCanvasXByDayN(startDayN - 1) +
                    getCanvasXByDayN(finishDayN) -
                    middleDateWidth
                    ) / 2;
            if (middleDateX < mCanvasXMin) {
                middleDateX = mCanvasXMin;
            } else if (mCanvasXMax < middleDateX + middleDateWidth) {
                middleDateX = mCanvasXMax - bounds.right;
            }
        }

        // == Drawing ==

        // Vertical fill
        mCanvas.drawRect(xMin, mCanvasYMin, xMax + 1, mCanvasYMax + 1, mPaintSelectedDate);

        // Horizontal fill
        Paint paint = valueDiff < 0 ? mPaintSelectedValueNegative : mPaintSelectedValue;
        mCanvas.drawRect(mCanvasXMin, yMin, mCanvasXMax + 1, yMax + 1, paint);

        // Draw the labels
        int valueLabelX = mCanvasXMin + mTickSize + mLabelTextMargin;
        int dateLabelY = mCanvasYMax - digitBounds.top + mTickSize + mLabelTextMargin;
        if (topValueLabel != null) {
            mCanvas.drawText(topValueLabel, valueLabelX, topLabelY, mPaintLabels);
        }
        if (bottomValueLabel != null) {
            mCanvas.drawText(bottomValueLabel, valueLabelX, bottomLabelY, mPaintLabels);
        }
        if (topPercentageLabel != null) {
            mPaintLabels.getTextBounds(topPercentageLabel, 0, topPercentageLabel.length(), bounds);
            int labelX = mCanvasXMax - mLabelTextMargin - bounds.right;
            mCanvas.drawText(topPercentageLabel, labelX, topLabelY, mPaintLabels);
        }
        if (bottomPercentageLabel != null) {
            mPaintLabels.getTextBounds(bottomPercentageLabel, 0, bottomPercentageLabel.length(), bounds);
            int labelX = mCanvasXMax - mLabelTextMargin - bounds.right;
            mCanvas.drawText(bottomPercentageLabel, labelX, bottomLabelY, mPaintLabels);
        }
        if (leftDateLabel != null) {
            mCanvas.drawText(leftDateLabel, leftDateX, dateLabelY, mPaintLabels);
        }
        if (rightDateLabel != null) {
            mCanvas.drawText(rightDateLabel, rightDateX, dateLabelY, mPaintLabels);
        }
        if (middleDateLabel != null) {
            mCanvas.drawText(middleDateLabel, middleDateX, dateLabelY, mPaintLabels);
        }

        // Draw the ticks
        drawHorizontalTickForValue(finishValue);
        drawHorizontalTickForValue(startValue);
    }

    private void drawLabels() {

        Rect bounds = new Rect();
        String text;

        // == Min value ==

        text = Util.formatNumber(mMinValue);
        mPaintLabels.getTextBounds(text, 0, text.length(), bounds);
        mCanvas.drawText(text,
                mCanvasXMin + mLabelTextMargin,
                mCanvasYMax - bounds.bottom - mLabelTextMargin,
                mPaintLabels);
        drawHorizontalTickForValue(mMinValue);

        // == Max value ==

        text = Util.formatNumber(mMaxValue);
        mPaintLabels.getTextBounds(text, 0, text.length(), bounds);
        mCanvas.drawText(text,
                mCanvasXMin + mTickSize + mLabelTextMargin,
                mCanvasYMin - bounds.top,
                mPaintLabels);
        drawHorizontalTickForValue(mMaxValue);

        // == Min date ==

        text = Util.formatDate(mMinDate, mContext);
        mPaintLabels.getTextBounds(text, 0, text.length(), bounds);

        // Try to center the label against date column, but adjust label's
        // position if there is not enough space
        int minDateX = (
                (getCanvasXByDayN(0) + getCanvasXByDayN(1)) -
                (bounds.right - bounds.left)
                ) / 2;
        if (minDateX < 0) {
            minDateX = mCanvasXMin;
        }

        mCanvas.drawText(text,
                minDateX,
                mCanvasYMax - bounds.top + mTickSize + mLabelTextMargin,
                mPaintLabels);

        // == Max date ==

        text = Util.formatDate(mMaxDate, mContext);
        mPaintLabels.getTextBounds(text, 0, text.length(), bounds);

        // Try to center the label against date column, but adjust label's
        // position if there is not enough space
        int maxDateWidth = bounds.right - bounds.left;
        int maxDateX = (
                (getCanvasXByDayN(mDateRange - 1) + getCanvasXByDayN(mDateRange)) -
                (bounds.right + bounds.left)
                ) / 2;
        if (mCanvasXMax < maxDateX + maxDateWidth) {
            maxDateX = mCanvasXMax - bounds.right;
        }

        mCanvas.drawText(text,
                maxDateX,
                mCanvasYMax - bounds.top + mTickSize + mLabelTextMargin,
                mPaintLabels);
    }

    private Date getDateByDayN(int n) {
        mCalendar.setTime(mMinDate);
        mCalendar.add(Calendar.DAY_OF_MONTH, n - 1);
        return mCalendar.getTime();
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

    private void drawHorizontalTickForValue(float value) {
        int y = getCanvasYByValue(value);
        mCanvas.drawLine(mCanvasXMin, y, mCanvasXMin + mTickSize, y, mPaintAxes);
    }
}
