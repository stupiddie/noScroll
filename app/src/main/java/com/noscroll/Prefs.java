package com.noscroll;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class Prefs {

    public static final String[][] APPS = {
            {"com.ss.android.ugc.aweme", "抖音"},
            {"com.ss.android.ugc.aweme.lite", "抖音极速版"},
            {"com.zhiliaoapp.musically", "TikTok"},
            {"com.smile.gifmaker", "快手"},
            {"com.kuaishou.nebula", "快手极速版"},
            {"com.xingin.xhs", "小红书"},
            {"tv.danmaku.bili", "哔哩哔哩"},
            {"com.sina.weibo", "微博"},
            {"com.google.android.youtube", "YouTube"},
    };

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("noscroll", Context.MODE_PRIVATE);
    }

    public static void init(Context c) {
        SharedPreferences p = sp(c);
        if (!p.getBoolean("v2", false)) {
            Set<String> onlyDouyin = new HashSet<>();
            onlyDouyin.add("com.ss.android.ugc.aweme");
            p.edit().putStringSet("targets", onlyDouyin).putBoolean("v2", true).apply();
        }
        if (!p.getBoolean("v3", false)) {
            p.edit().putInt("statsAlerts", 0).putBoolean("v3", true).apply();
        }
    }

    public static Set<String> targets(Context c) {
        Set<String> saved = sp(c).getStringSet("targets", null);
        if (saved == null) {
            Set<String> onlyDouyin = new HashSet<>();
            onlyDouyin.add("com.ss.android.ugc.aweme");
            return onlyDouyin;
        }
        return new HashSet<>(saved);
    }

    public static boolean isWatched(Context c, String pkg) {
        return pkg != null && !pkg.isEmpty() && targets(c).contains(pkg);
    }

    public static void setTarget(Context c, String pkg, boolean on) {
        Set<String> set = targets(c);
        if (on) {
            set.add(pkg);
        } else {
            set.remove(pkg);
        }
        sp(c).edit().putStringSet("targets", set).apply();
    }

    public static String nameOf(Context c, String pkg) {
        for (String[] app : APPS) {
            if (app[0].equals(pkg)) {
                return app[1];
            }
        }
        return "这个 App";
    }
}
