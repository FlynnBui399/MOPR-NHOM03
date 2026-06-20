package com.example.pocket.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class RecordingBorderView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float sweepAngle = 0f;
    private float cornerRadius = 0f;

    public RecordingBorderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(0xFF00E5FF);
        paint.setStrokeWidth(dpToPx(6));
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void setCornerRadius(float dp) { this.cornerRadius = dpToPx(dp); }
    public void setSweepAngle(float angle) { this.sweepAngle = angle; invalidate(); }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;

        float strokeWidth = paint.getStrokeWidth();
        float inset = strokeWidth / 2f;
        float left = inset;
        float top = inset;
        float right = getWidth() - inset;
        float bottom = getHeight() - inset;
        float r = cornerRadius;

        // Ensure corner radius is not larger than half of width/height
        float maxRadius = Math.min(getWidth(), getHeight()) / 2f - inset;
        if (r > maxRadius) {
            r = maxRadius;
        }

        float cx = getWidth() / 2f;

        android.graphics.Path path = new android.graphics.Path();
        if (r <= 0) {
            path.moveTo(cx, top);
            path.lineTo(right, top);
            path.lineTo(right, bottom);
            path.lineTo(left, bottom);
            path.lineTo(left, top);
            path.lineTo(cx, top);
        } else {
            path.moveTo(cx, top);
            path.lineTo(right - r, top);
            path.arcTo(new RectF(right - 2 * r, top, right, top + 2 * r), -90f, 90f, false);
            path.lineTo(right, bottom - r);
            path.arcTo(new RectF(right - 2 * r, bottom - 2 * r, right, bottom), 0f, 90f, false);
            path.lineTo(left + r, bottom);
            path.arcTo(new RectF(left, bottom - 2 * r, left + 2 * r, bottom), 90f, 90f, false);
            path.lineTo(left, top + r);
            path.arcTo(new RectF(left, top, left + 2 * r, top + 2 * r), 180f, 90f, false);
            path.lineTo(cx, top);
        }

        android.graphics.PathMeasure pm = new android.graphics.PathMeasure(path, false);
        float length = pm.getLength();
        android.graphics.Path segmentPath = new android.graphics.Path();
        float progress = sweepAngle / 360f;
        pm.getSegment(0, progress * length, segmentPath, true);

        canvas.drawPath(segmentPath, paint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
