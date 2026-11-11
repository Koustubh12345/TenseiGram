package org.telegram.messenger;

import android.os.Build;
import android.os.VibrationEffect;

import androidx.annotation.RequiresApi;

public enum BotWebViewVibrationEffect {
    IMPACT_LIGHT(new long[] {7}, new int[] {65}, new long[] {60}),
    IMPACT_MEDIUM(new long[] {7}, new int[] {145}, new long[] {70}),
    IMPACT_HEAVY(new long[] {7}, new int[] {255}, new long[] {80}),
    IMPACT_RIGID(new long[] {3}, new int[] {225}, new long[] {50}),
    IMPACT_SOFT(new long[] {10}, new int[] {175}, new long[] {55}),
    NOTIFICATION_ERROR(new long[] {14,48,14,48,14,48,20}, new int[] {200,0,200,0,255,0,145}, new long[] {40,60,40,60,65,60,40}),
    NOTIFICATION_SUCCESS(new long[] {14,65,14}, new int[] {175,0,255}, new long[] {50,60,65}),
    NOTIFICATION_WARNING(new long[] {14,64,14}, new int[] {225,0,175}, new long[] {65,60,40}),
    SELECTION_CHANGE(new long[] {1}, new int[] {65}, new long[] {30}),
    APP_ERROR(new long[] {30,10,150,10}, new int[] {0,100,0,100}, new long[] {40,60,40,60,65,60,40});

    public final long[] timings;
    public final int[] amplitudes;
    public final long[] fallbackTimings;
    private Object vibrationEffect;
    private int builtIntensity = -1;

    BotWebViewVibrationEffect(long[] timings, int[] amplitudes, long[] fallbackTimings) {
        this.timings = timings;
        this.amplitudes = amplitudes;
        this.fallbackTimings = fallbackTimings;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public VibrationEffect getVibrationEffectForOreo() {
        return getScaledEffect();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private synchronized VibrationEffect getScaledEffect() {
        int pct = Math.max(1, Math.min(100, TenseiConfig.vibrationIntensity));
        if (vibrationEffect == null || builtIntensity != pct) {
            if (!AndroidUtilities.getVibrator().hasAmplitudeControl()) {
                vibrationEffect = VibrationEffect.createWaveform(fallbackTimings, -1);
            } else if (pct >= 100) {
                vibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, -1);
            } else {
                int[] scaled = new int[amplitudes.length];
                for (int i = 0; i < amplitudes.length; i++) {
                    scaled[i] = amplitudes[i] <= 0 ? 0 : Math.max(1, amplitudes[i] * pct / 100);
                }
                vibrationEffect = VibrationEffect.createWaveform(timings, scaled, -1);
            }
            builtIntensity = pct;
        }

        return (VibrationEffect) vibrationEffect;
    }

    public void vibrate() {
        if (!TenseiConfig.vibrationEnabled || TenseiConfig.vibrationIntensity <= 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AndroidUtilities.getVibrator().vibrate(getScaledEffect());
        } else {
            AndroidUtilities.getVibrator().vibrate(fallbackTimings, -1);
        }
    }
}
