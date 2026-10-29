package com.noscroll;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Stats {

    private Stats() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("noscroll", Context.MODE_PRIVATE);
    }

    private static void ensureToday(Context c) {
        SharedPreferences p = sp(c);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        if (!today.equals(p.getString("statsDate", ""))) {
            p.edit()
                    .putString("statsDate", today)
                    .putInt("statsAlerts", 0)
                    .apply();
        }
    }

    public static void addAlert(Context c) {
        ensureToday(c);
        SharedPreferences p = sp(c);
        p.edit().putInt("statsAlerts", p.getInt("statsAlerts", 0) + 1).apply();
    }

    public static int alertCount(Context c) {
        ensureToday(c);
        return sp(c).getInt("statsAlerts", 0);
    }

    public static String summary(Context c) {
        return "今天弹了 " + alertCount(c) + " 次提醒";
    }
}
