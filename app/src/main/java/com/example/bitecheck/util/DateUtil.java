package com.example.bitecheck.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {

    public static final SimpleDateFormat DAY =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    public static final SimpleDateFormat TIMESTAMP =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public static final SimpleDateFormat DISPLAY_DAY =
            new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);
    public static final SimpleDateFormat DISPLAY_TIME =
            new SimpleDateFormat("HH:mm", Locale.US);

    private DateUtil() {
    }

    public static String today() {
        return DAY.format(new Date());
    }

    public static String now() {
        return TIMESTAMP.format(new Date());
    }

    public static String dayNDaysAgo(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        return DAY.format(calendar.getTime());
    }

    public static String displayDay(String day) {
        try {
            Date date = DAY.parse(day);
            return date == null ? day : DISPLAY_DAY.format(date);
        } catch (ParseException e) {
            return day;
        }
    }

    public static String displayTime(String timestamp) {
        try {
            Date date = TIMESTAMP.parse(timestamp);
            return date == null ? "" : DISPLAY_TIME.format(date);
        } catch (ParseException e) {
            return "";
        }
    }
}
