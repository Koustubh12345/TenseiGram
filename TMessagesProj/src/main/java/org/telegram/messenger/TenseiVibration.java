package org.telegram.messenger;

import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;

// Central gate for all in-app vibration/haptics: kill-switch + intensity scaling.
public class TenseiVibration {

    public static boolean enabled() {
        return TenseiConfig.vibrationEnabled && TenseiConfig.vibrationIntensity > 0;
    }

    public static int intensityPercent() {
        return Math.max(0, Math.min(100, TenseiConfig.vibrationIntensity));
    }

    public static void vibrate(Vibrator vibrator, long millis) {
        if (vibrator == null || !enabled()) {
            return;
        }
        int pct = intensityPercent();
        try {
            vibrator.vibrate(pct >= 100 ? millis : Math.max(1, millis * pct / 100));
        } catch (Throwable ignored) {
        }
    }

    public static void vibrate(Vibrator vibrator, VibrationEffect effect) {
        if (vibrator == null || effect == null || !enabled()) {
            return;
        }
        try {
            vibrator.vibrate(effect);
        } catch (Throwable ignored) {
        }
    }

    public static boolean performHapticFeedback(View view, int feedbackConstant) {
        if (view == null || !enabled()) {
            return false;
        }
        try {
            return view.performHapticFeedback(feedbackConstant);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean performHapticFeedback(View view, int feedbackConstant, int flags) {
        if (view == null || !enabled()) {
            return false;
        }
        try {
            return view.performHapticFeedback(feedbackConstant, flags);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
