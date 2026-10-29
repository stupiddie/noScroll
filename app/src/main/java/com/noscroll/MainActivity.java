package com.noscroll;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Set;

public class MainActivity extends Activity {

    private static final int BG = 0xFF0F0F11;
    private static final int CARD = 0xFF1A1B1E;
    private static final int TEXT = 0xFFECECEC;
    private static final int MUTED = 0xFF8E9297;
    private static final int ACCENT = 0xFFFF6B35;

    private TextView statusText;
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Prefs.init(this);
        getWindow().setStatusBarColor(BG);
        setContentView(build());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        statsText.setText(Stats.summary(this));
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(40), dp(20), dp(40));
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("NoScroll");
        title.setTextSize(32);
        title.setTextColor(TEXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("一打开抖音，就叫你一声。");
        sub.setTextSize(14);
        sub.setTextColor(MUTED);
        sub.setPadding(0, dp(6), 0, dp(26));
        root.addView(sub);

        LinearLayout statusCard = card();
        statusText = new TextView(this);
        statusText.setTextSize(15);
        statusText.setLineSpacing(dp(4), 1f);
        statusCard.addView(statusText);

        Button enable = new Button(this);
        enable.setText("打开无障碍设置");
        enable.setAllCaps(false);
        enable.setTextSize(16);
        enable.setTextColor(0xFF0F0F11);
        GradientDrawable enableBg = new GradientDrawable();
        enableBg.setColor(ACCENT);
        enableBg.setCornerRadius(dp(14));
        enable.setBackground(enableBg);
        enable.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams enableLp =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        enableLp.topMargin = dp(14);
        statusCard.addView(enable, enableLp);
        root.addView(statusCard, cardLp());

        addSection(root, "监控哪些 App");
        LinearLayout appCard = card();
        Set<String> targets = Prefs.targets(this);
        for (String[] app : Prefs.APPS) {
            CheckBox cb = new CheckBox(this);
            cb.setText(app[1]);
            cb.setTextColor(TEXT);
            cb.setTextSize(15);
            cb.setPadding(0, dp(4), 0, dp(4));
            cb.setChecked(targets.contains(app[0]));
            cb.setOnCheckedChangeListener((button, checked) ->
                    Prefs.setTarget(this, app[0], checked));
            appCard.addView(cb);
        }
        TextView appHint = small("打开上面勾选的 App 时，会立刻弹出提醒。");
        appHint.setPadding(0, dp(10), 0, 0);
        appCard.addView(appHint);
        root.addView(appCard, cardLp());

        addSection(root, "今天");
        LinearLayout statsCard = card();
        statsText = new TextView(this);
        statsText.setTextColor(TEXT);
        statsText.setTextSize(15);
        statsText.setText(Stats.summary(this));
        statsCard.addView(statsText);
        TextView statsHint = small("进入目标 App 会立刻提醒，并自动退回桌面。想刷只能先关掉无障碍。");
        statsHint.setPadding(0, dp(10), 0, 0);
        statsCard.addView(statsHint);
        root.addView(statsCard, cardLp());

        TextView note = small("安卓 13 及以上，侧载安装的应用要先到「应用信息」右上角菜单点「允许受限设置」，"
                + "否则无障碍开关是灰的、打不开。");
        note.setPadding(dp(4), dp(24), dp(4), 0);
        root.addView(note);

        return scroll;
    }

    private void updateStatus() {
        boolean on = isAccessibilityEnabled();
        if (on) {
            statusText.setText("无障碍服务：已开启");
            statusText.setTextColor(0xFF4ADE80);
        } else {
            statusText.setText("无障碍服务：未开启\n开启后一进入目标 App 就会弹提醒。");
            statusText.setTextColor(TEXT);
        }
    }

    private boolean isAccessibilityEnabled() {
        AccessibilityManager am =
                (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        if (am == null) {
            return false;
        }
        for (AccessibilityServiceInfo info :
                am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            String id = info.getId();
            if (id != null && id.startsWith(getPackageName() + "/")) {
                return true;
            }
        }
        return false;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(20));
        card.setBackground(bg);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        return card;
    }

    private LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(10);
        return lp;
    }

    private void addSection(LinearLayout root, String label) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(MUTED);
        tv.setLetterSpacing(0.08f);
        tv.setPadding(dp(4), dp(26), 0, dp(4));
        root.addView(tv);
    }

    private TextView small(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(MUTED);
        tv.setLineSpacing(dp(4), 1f);
        return tv;
    }
}
