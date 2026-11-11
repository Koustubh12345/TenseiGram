package org.telegram.ui;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;

import org.telegram.messenger.AndroidUtilities;

public class TenseiHeaderDrawable extends Drawable {

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clipPath = new Path();
    private final float radius;

    public TenseiHeaderDrawable() {
        radius = AndroidUtilities.dp(24);
        bgPaint.setColor(0xFFE9EDF6);
        bgPaint.setStyle(Paint.Style.FILL);

        arcPaint1.setColor(0xFFD1DDEB);
        arcPaint1.setStyle(Paint.Style.FILL);

        arcPaint2.setColor(0xFFC4D4E4);
        arcPaint2.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        rect.set(bounds);
        clipPath.reset();
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW);
    }

    @Override
    public void draw(Canvas canvas) {
        int save = canvas.save();
        canvas.clipPath(clipPath);

        canvas.drawRect(rect, bgPaint);

        float w = rect.width();
        float h = rect.height();

        float r1 = w * 0.44f;
        canvas.drawCircle(w * 0.88f, -r1 * 0.12f, r1, arcPaint1);

        float r2 = w * 0.32f;
        canvas.drawCircle(w + r2 * 0.18f, h * 0.52f, r2, arcPaint2);

        float r3 = w * 0.36f;
        canvas.drawCircle(w * 0.82f, h + r3 * 0.28f, r3, arcPaint1);

        canvas.restoreToCount(save);
    }

    @Override
    public void getOutline(Outline outline) {
        if (Build.VERSION.SDK_INT >= 21) {
            outline.setRoundRect(getBounds(), radius);
        } else {
            super.getOutline(outline);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        bgPaint.setAlpha(alpha);
        arcPaint1.setAlpha(alpha);
        arcPaint2.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
        bgPaint.setColorFilter(colorFilter);
        arcPaint1.setColorFilter(colorFilter);
        arcPaint2.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
