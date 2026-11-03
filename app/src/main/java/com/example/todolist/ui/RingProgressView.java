package com.example.todolist.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;

/** A thin ring that fills from the top to show a percentage. The caption goes in a TextView laid over it. */
public class RingProgressView extends View {

    private int percent;
    private float shown;
    @Nullable
    private ValueAnimator animator;

    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;

    public RingProgressView(Context context) {
        this(context, null);
    }

    public RingProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RingProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setColor(ContextCompat.getColor(context, R.color.lavender));
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(0x338692F7);
    }

    public void setPercent(int newPercent) {
        percent = Math.max(0, Math.min(100, newPercent));
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(shown, percent);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            shown = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = 10 * density;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f + stroke / 2f;
        float top = (getHeight() - size) / 2f + stroke / 2f;
        oval.set(left, top, left + size - stroke, top + size - stroke);
        ringPaint.setStrokeWidth(stroke);
        trackPaint.setStrokeWidth(stroke);
        canvas.drawArc(oval, 0, 360, false, trackPaint);
        if (shown > 0.5f) canvas.drawArc(oval, -90, 360f * shown / 100f, false, ringPaint);
    }
}
