package com.example.todolist.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.example.todolist.R;
import com.google.android.material.color.MaterialColors;

/**
 * A small column chart: one column per value with its number above and a label below. The column of
 * {@code highlight} (usually today) is drawn in the strong colour, the others in a lighter shade.
 * Drawn by hand to avoid a chart library for one simple picture.
 */
public class BarChartView extends View {

    private float[] values = new float[0];
    private String[] labels = new String[0];
    private int highlight = -1;
    /** 0..1: how far the columns have grown (the animation after new data). */
    private float grow = 1f;
    @Nullable
    private ValueAnimator animator;

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barDimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path path = new Path();
    private final float density;

    public BarChartView(Context context) {
        this(context, null);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        density = context.getResources().getDisplayMetrics().density;
        int strong = ContextCompat.getColor(context, R.color.lavender);
        barPaint.setColor(strong);
        barDimPaint.setColor((strong & 0x00FFFFFF) | 0x80000000);
        trackPaint.setColor((strong & 0x00FFFFFF) | 0x1A000000);
        int text = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF777777);
        int strongText = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, 0xFF222222);
        labelPaint.setColor(text);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTextSize(12 * density);
        valuePaint.setColor(strongText);
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setTextSize(12 * density);
        valuePaint.setFakeBoldText(true);
    }

    /** @param highlight index of the column to emphasise, or -1 */
    public void setData(float[] newValues, String[] newLabels, int highlight) {
        values = newValues == null ? new float[0] : newValues.clone();
        labels = newLabels == null ? new String[0] : newLabels.clone();
        this.highlight = highlight;
        if (animator != null) animator.cancel();
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(500);
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
        int height = (int) (170 * density);
        setMeasuredDimension(resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = values.length;
        if (count == 0) return;
        float labelBand = 22 * density;
        float valueBand = 18 * density;
        float chartTop = valueBand;
        float chartBottom = getHeight() - labelBand;
        float chartHeight = Math.max(1f, chartBottom - chartTop);
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) count;
        float barWidth = Math.min(slot * 0.56f, 34 * density);
        float radius = Math.min(barWidth / 2f, 8 * density);

        float max = 1f;
        for (float v : values) max = Math.max(max, v);

        for (int i = 0; i < count; i++) {
            float centerX = getPaddingLeft() + slot * i + slot / 2f;
            // The light track shows where a column could reach, so an empty day still looks like a day.
            rect.set(centerX - barWidth / 2f, chartTop, centerX + barWidth / 2f, chartBottom);
            canvas.drawRoundRect(rect, radius, radius, trackPaint);

            float value = Math.max(0f, values[i]);
            if (value > 0f) {
                float h = Math.max(radius * 2f, chartHeight * (value / max) * grow);
                rect.set(centerX - barWidth / 2f, chartBottom - h, centerX + barWidth / 2f, chartBottom);
                drawTopRounded(canvas, rect, radius, i == highlight ? barPaint : barDimPaint);
                if (grow > 0.85f) {
                    canvas.drawText(String.valueOf((int) value), centerX, rect.top - 5 * density, valuePaint);
                }
            }
            if (i < labels.length && labels[i] != null) {
                Paint p = labelPaint;
                p.setFakeBoldText(i == highlight);
                canvas.drawText(labels[i], centerX, getHeight() - 5 * density, p);
            }
        }
    }

    /** A column with rounded top corners and a flat bottom (it stands on the axis). */
    private void drawTopRounded(Canvas canvas, RectF r, float radius, Paint paint) {
        path.reset();
        path.moveTo(r.left, r.bottom);
        path.lineTo(r.left, r.top + radius);
        path.quadTo(r.left, r.top, r.left + radius, r.top);
        path.lineTo(r.right - radius, r.top);
        path.quadTo(r.right, r.top, r.right, r.top + radius);
        path.lineTo(r.right, r.bottom);
        path.close();
        canvas.drawPath(path, paint);
    }
}
