package com.noscroll;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ScrollGuardService extends AccessibilityService {

    private static final long ENTER_DELAY_MS = 400L;
    private static final long AUTO_EXIT_MS = 2200L;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private WindowManager windowManager;
    private View overlay;
    private String currentPkg = "";
    private String alertPkg = "";
    private int rootRetry = 0;

    private final Runnable enterAlert = new Runnable() {
        @Override
        public void run() {
            if (Prefs.isWatched(ScrollGuardService.this, currentPkg)) {
                showAlert();
            }
        }
    };

    private final Runnable autoExit = new Runnable() {
        @Override
        public void run() {
            if (overlay != null) {
                exitNow();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Prefs.init(this);
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        handler.postDelayed(() -> {
            try {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    CharSequence pkg = root.getPackageName();
                    if (pkg != null) {
                        switchPackage(pkg.toString());
                    }
                }
            } catch (Exception ignored) {
            }
        }, 800L);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }
        CharSequence pkgCs = event.getPackageName();
        if (pkgCs == null) {
            return;
        }
        String pkg = pkgCs.toString();

        if (pkg.equals(getPackageName())) {
            return;
        }

        if (!pkg.isEmpty() && !pkg.equals(currentPkg)) {
            switchPackage(pkg);
        }
    }

    private void switchPackage(String pkg) {
        if (pkg.equals(currentPkg)) {
            return;
        }

        if (overlay != null) {
            return;
        }

        handler.removeCallbacks(enterAlert);
        currentPkg = pkg;
        rootRetry = 0;

        if (Prefs.isWatched(this, pkg)) {
            handler.postDelayed(enterAlert, ENTER_DELAY_MS);
        }
    }

    private void showAlert() {
        if (overlay != null) {
            return;
        }

        String foreground = foregroundPackage();
        if (foreground == null) {
            if (rootRetry < 2) {
                rootRetry++;
                handler.postDelayed(enterAlert, 400L);
            }
            return;
        }
        rootRetry = 0;
        if (!foreground.equals(currentPkg)) {
            switchPackage(foreground);
            return;
        }
        if (!Prefs.isWatched(this, currentPkg)) {
            return;
        }

        alertPkg = currentPkg;
        Stats.addAlert(this);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setClickable(true);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(0xFF1C1C1E);
        cardBg.setCornerRadius(dp(24));
        cardBg.setStroke(dp(1), 0x33FFFFFF);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg);
        card.setPadding(dp(24), dp(28), dp(24), dp(20));

        FrameLayout.LayoutParams cardParams =
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT);
        cardParams.gravity = Gravity.CENTER;
        cardParams.leftMargin = dp(24);
        cardParams.rightMargin = dp(24);
        root.addView(card, cardParams);

        TextView title = new TextView(this);
        title.setText("你又在刷" + Prefs.nameOf(this, alertPkg) + "了！");
        title.setTextSize(25);
        title.setTextColor(0xFFECECEC);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        card.addView(title);

        TextView body = new TextView(this);
        body.setText("马上给你退出去。真想刷，只能先去设置里关掉 NoScroll。\n\n今天已经被叫停 "
                + Stats.alertCount(this) + " 次。");
        body.setTextSize(16);
        body.setTextColor(0xFFB8BCC2);
        body.setLineSpacing(dp(4), 1f);
        body.setPadding(0, dp(12), 0, dp(24));
        card.addView(body);

        Button quit = new Button(this);
        quit.setText("退出" + Prefs.nameOf(this, alertPkg));
        quit.setTextSize(16);
        quit.setTextColor(0xFF0F0F11);
        quit.setAllCaps(false);
        GradientDrawable quitBg = new GradientDrawable();
        quitBg.setColor(0xFFFF6B35);
        quitBg.setCornerRadius(dp(16));
        quit.setBackground(quitBg);
        quit.setOnClickListener(v -> exitNow());
        card.addView(quit, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52)));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                PixelFormat.TRANSLUCENT);
        lp.dimAmount = 0.8f;

        try {
            windowManager.addView(root, lp);
            overlay = root;
            root.setAlpha(0f);
            root.animate().alpha(1f).setDuration(160).start();
            handler.postDelayed(autoExit, AUTO_EXIT_MS);
        } catch (Exception ignored) {
            overlay = null;
        }
    }

    private void exitNow() {
        handler.removeCallbacks(enterAlert);
        handler.removeCallbacks(autoExit);
        dismissOverlay();
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    private String foregroundPackage() {
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence pkg = root.getPackageName();
                if (pkg != null) {
                    return pkg.toString();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void dismissOverlay() {
        handler.removeCallbacks(autoExit);
        if (overlay == null) {
            return;
        }
        try {
            windowManager.removeView(overlay);
        } catch (Exception ignored) {
        }
        overlay = null;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onInterrupt() {
        handler.removeCallbacksAndMessages(null);
        dismissOverlay();
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        dismissOverlay();
        super.onDestroy();
    }
}
