package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.TenseiTheme;
import org.telegram.messenger.TenseiVibration;

/**
 * Modern iOS / Material-3 style pill toggle switch matching the design screenshot:
 * Automatically adapts track colors between Light and Dark mode.
 */
public class TenseiPillSwitch extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF trackRect = new RectF();

    private boolean isChecked = false;
    private float progress = 0f;
    private ValueAnimator animator;
    private OnCheckedChangeListener listener;

    public interface OnCheckedChangeListener {
        void onCheckedChanged(TenseiPillSwitch switchView, boolean checked);
    }

    public TenseiPillSwitch(Context context) {
        super(context);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
        shadowPaint.setColor(0x28000000);
        shadowPaint.setStyle(Paint.Style.FILL);
    }

    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.listener = listener;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked, boolean animated) {
        if (isChecked == checked) {
            return;
        }
        isChecked = checked;
        if (animated) {
            if (animator != null) {
                animator.cancel();
            }
            animator = ValueAnimator.ofFloat(progress, checked ? 1f : 0f);
            animator.setDuration(220);
            animator.setInterpolator(new DecelerateInterpolator(1.5f));
            animator.addUpdateListener(animation -> {
                progress = (float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        } else {
            progress = checked ? 1f : 0f;
            invalidate();
        }
    }

    @Override
    public boolean performClick() {
        super.performClick();
        setChecked(!isChecked, true);
        TenseiVibration.performHapticFeedback(this, HapticFeedbackConstants.KEYBOARD_TAP);
        if (listener != null) {
            listener.onCheckedChanged(this, isChecked);
        }
        return true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = AndroidUtilities.dp(38);
        int h = AndroidUtilities.dp(22);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float w = getWidth();
        float h = getHeight();
        float radius = h / 2f;

        trackRect.set(0, 0, w, h);

        int colorUnchecked = TenseiTheme.getSwitchTrackUnchecked(getContext());
        int colorChecked = TenseiTheme.getSwitchTrackChecked(getContext());

        // Blend track color
        int trackColor = blendColor(colorUnchecked, colorChecked, progress);
        trackPaint.setColor(trackColor);
        canvas.drawRoundRect(trackRect, radius, radius, trackPaint);

        // Thumb calculations
        float padding = AndroidUtilities.dp(2f);
        float thumbRadius = (h - padding * 2f) / 2f;
        float startX = padding + thumbRadius;
        float endX = w - padding - thumbRadius;
        float cx = startX + (endX - startX) * progress;
        float cy = h / 2f;

        // Drop shadow under thumb
        canvas.drawCircle(cx, cy + AndroidUtilities.dp(0.5f), thumbRadius, shadowPaint);

        // Pure white thumb
        thumbPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(cx, cy, thumbRadius, thumbPaint);
    }

    private static int blendColor(int from, int to, float ratio) {
        float inv = 1f - ratio;
        int a = (int) (((from >>> 24) * inv) + ((to >>> 24) * ratio));
        int r = (int) ((((from >> 16) & 0xFF) * inv) + (((to >> 16) & 0xFF) * ratio));
        int g = (int) ((((from >> 8) & 0xFF) * inv) + (((to >> 8) & 0xFF) * ratio));
        int b = (int) (((from & 0xFF) * inv) + ((to & 0xFF) * ratio));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
