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
import com.google.android.material.color.MaterialColors;

/** A ring split into coloured parts, with the total in the middle. Drawn by hand. */
public class DonutChartView extends View {

    private float[] values = new float[0];
    private int[] colors = new int[0];
    private float grow = 1f;
    @Nullable
    private ValueAnimator animator;

    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private final float density;

    public DonutChartView(Context context) {
        this(context, null);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DonutChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeCap(Paint.Cap.BUTT);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setColor(0x1F8692F7);
        centerPaint.setTextAlign(Paint.Align.CENTER);
        centerPaint.setFakeBoldText(true);
        centerPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF222222));
    }

    public void setData(float[] newValues, int[] newColors) {
        values = newValues == null ? new float[0] : newValues.clone();
        colors = newColors == null ? new int[0] : newColors.clone();
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            grow = (float) a.getAnimatedValue();
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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size = (int) (130 * density);
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float stroke = 18 * density;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f + stroke / 2f;
        float top = (getHeight() - size) / 2f + stroke / 2f;
        oval.set(left, top, left + size - stroke, top + size - stroke);
        arcPaint.setStrokeWidth(stroke);
        trackPaint.setStrokeWidth(stroke);
        canvas.drawArc(oval, 0, 360, false, trackPaint);

        float total = 0f;
        for (float v : values) total += Math.max(0f, v);
        if (total > 0f) {
            float start = -90f;
            float gap = values.length > 1 ? 2f : 0f;
            for (int i = 0; i < values.length && i < colors.length; i++) {
                float v = Math.max(0f, values[i]);
                if (v == 0f) continue;
                float sweep = 360f * (v / total) * grow;
                arcPaint.setColor(colors[i]);
                canvas.drawArc(oval, start, Math.max(0f, sweep - gap), false, arcPaint);
                start += sweep;
            }
        }
        centerPaint.setTextSize(26 * density);
        float baseline = getHeight() / 2f - (centerPaint.descent() + centerPaint.ascent()) / 2f;
        canvas.drawText(String.valueOf((int) total), getWidth() / 2f, baseline, centerPaint);
    }
}
