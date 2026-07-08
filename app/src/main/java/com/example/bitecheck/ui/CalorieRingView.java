package com.example.bitecheck.ui;

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

import com.example.bitecheck.R;

/**
 * Hero progress ring for the Dashboard: track circle + animated sweep arc.
 * Turns carrot-orange once the calorie target is exceeded.
 */
public class CalorieRingView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcBounds = new RectF();
    private final float strokeWidth;

    private float fraction;
    private ValueAnimator animator;

    public CalorieRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        strokeWidth = getResources().getDisplayMetrics().density * 14;

        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(strokeWidth);
        trackPaint.setColor(ContextCompat.getColor(context, R.color.bc_card_stroke));

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(strokeWidth);
        progressPaint.setStrokeCap(Paint.Cap.ROUND);
        progressPaint.setColor(ContextCompat.getColor(context, R.color.bc_primary));
    }

    public void setValues(int consumed, int target) {
        float goal = target > 0
                ? Math.min(1f, consumed / (float) target) : 0f;
        boolean over = target > 0 && consumed > target;
        progressPaint.setColor(ContextCompat.getColor(getContext(),
                over ? R.color.bc_secondary : R.color.bc_primary));

        if (animator != null) {
            animator.cancel();
        }
        animator = ValueAnimator.ofFloat(0f, goal);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            fraction = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = strokeWidth / 2f + 1;
        arcBounds.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(arcBounds, 0, 360, false, trackPaint);
        if (fraction > 0) {
            canvas.drawArc(arcBounds, -90, 360 * fraction, false, progressPaint);
        }
    }
}
