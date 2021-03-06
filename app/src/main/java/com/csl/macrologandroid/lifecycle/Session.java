package com.csl.macrologandroid.lifecycle;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Session {

    private static Session instance;

    private static Date timestamp = Calendar.getInstance().getTime();

    private static final long THRESHOLD = 30;

    private Session() {

    }

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public boolean isExpired() {
        long difference = Calendar.getInstance().getTimeInMillis() - timestamp.getTime();
        long minutes = TimeUnit.MINUTES.convert(difference, TimeUnit.MILLISECONDS);
        return THRESHOLD <= minutes;
    }

    public static void resetTimestamp() {
        timestamp = Calendar.getInstance().getTime();
    }

}
