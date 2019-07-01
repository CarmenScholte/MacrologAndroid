package com.csl.macrologandroid.util;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("CanBeFinal")
public class DateParser {

    private static SimpleDateFormat standardFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static SimpleDateFormat shortFormat = new SimpleDateFormat("yyyy-M-d", Locale.getDefault());
    private static SimpleDateFormat reversedFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private static SimpleDateFormat reversedShortFormat = new SimpleDateFormat("d-M-yyyy", Locale.getDefault());

    public static String format(Date date) {
        return standardFormat.format(date);
    }

    public static Date parse(String string) {
        Date date = null;
        try {
            date = standardFormat.parse(string);
        } catch (ParseException ex) {
            try {
                date = shortFormat.parse(string);
            } catch (ParseException ex2) {
                try {
                    date = reversedFormat.parse(string);
                } catch (ParseException ex3) {
                    try {
                        date = reversedShortFormat.parse(string);
                    } catch (ParseException ex4) {
                        Log.e(DateParser.class.toString(), "Could not parse to Date");
                    }
                }
            }
        }

        return date;
    }
}
