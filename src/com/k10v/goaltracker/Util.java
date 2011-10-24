package com.k10v.goaltracker;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.Context;

public final class Util {

    public static String formatDate(Calendar calendar, Context context) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        if (calendar.equals(today)) {
            return context.getString(R.string.date_today);
        }

        String format = calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) ? "MMM d" : "MMM d, yyyy";
        return android.text.format.DateFormat.format(format, calendar).toString();
    }

    public static String formatDate(Date date, Context context) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return formatDate(calendar, context);
    }

    public static String formatDate(String sqlDateString, Context context) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        Date date;
        try {
            date = df.parse(sqlDateString);
        } catch (ParseException e) {
            // Don't format records with invalid date
            return sqlDateString;
        }
        return formatDate(date, context);
    }

    /**
     * Formats a number in a nice way: strips trailing zeros from float numbers
     * and uses "real" minus character for negative numbers
     *
     * @param number
     * @param withPlusSign Whether to add a "plus" sign to positive numbers
     * @return
     */
    public static String formatNumber(float number, boolean withPlusSign) {
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

    public static String formatNumber(float number) {
        return formatNumber(number, false);
    }
}
