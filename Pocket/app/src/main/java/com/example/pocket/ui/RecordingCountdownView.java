package com.example.pocket.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class RecordingCountdownView extends View {
    private Paint paint;
    private float sweepAngle = 0f; // 0 to 360
    private float strokeWidthPx;
    private float cornerRadiusPx;
    private Path path = new Path();
    private PathMeasure pathMeasure;
    private Path segmentPath = new Path();
    private android.animation.ValueAnimator animator;

    public RecordingCountdownView(Context context) {
        super(context);
        init(context, null);
    }

    public RecordingCountdownView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public RecordingCountdownView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        float density = context.getResources().getDisplayMetrics().density;
        strokeWidthPx = 5f * density; // 5dp stroke width
        cornerRadiusPx = 36f * density; // Matches card corner radius of 36dp

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidthPx);
        paint.setColor(Color.parseColor("#00E5FF")); // Cyan #00E5FF
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setSweepAngle(float sweepAngle) {
        this.sweepAngle = Math.max(0f, Math.min(360f, sweepAngle));
        invalidate();
    }

    public void startCountdown(long duration) {
        stopCountdown();
        animator = android.animation.ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(duration);
        animator.setInterpolator(new android.view.animation.LinearInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            setSweepAngle(val);
        });
        animator.start();
    }

    public void stopCountdown() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        setSweepAngle(0f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        buildPath(w, h);
    }

    private void buildPath(int w, int h) {
        path.reset();
        float sw = strokeWidthPx;
        float cr = cornerRadiusPx;

        // Top-center start running clockwise
        path.moveTo(w / 2f, sw / 2f);

        // Top right segment
        path.lineTo(w - cr, sw / 2f);
        path.arcTo(new RectF(w - cr * 2f + sw / 2f, sw / 2f, w - sw / 2f, cr * 2f - sw / 2f), -90, 90, false);

        // Right segment
        path.lineTo(w - sw / 2f, h - cr);
        path.arcTo(new RectF(w - cr * 2f + sw / 2f, h - cr * 2f + sw / 2f, w - sw / 2f, h - sw / 2f), 0, 90, false);

        // Bottom segment
        path.lineTo(cr, h - sw / 2f);
        path.arcTo(new RectF(sw / 2f, h - cr * 2f + sw / 2f, cr * 2f - sw / 2f, h - sw / 2f), 90, 90, false);

        // Left segment
        path.lineTo(sw / 2f, cr);
        path.arcTo(new RectF(sw / 2f, sw / 2f, cr * 2f - sw / 2f, cr * 2f - sw / 2f), 180, 90, false);

        // Top left segment
        path.lineTo(w / 2f, sw / 2f);

        pathMeasure = new PathMeasure(path, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sweepAngle > 0f && pathMeasure != null) {
            segmentPath.reset();
            segmentPath.rLineTo(0, 0); // Workaround for some rendering engines
            float length = pathMeasure.getLength();
            pathMeasure.getSegment(0, length * (sweepAngle / 360f), segmentPath, true);
            canvas.drawPath(segmentPath, paint);
        }
    }
}
