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
        float inset = dpToPx(3);
        RectF rect = new RectF(inset, inset, getWidth()-inset, getHeight()-inset);
        canvas.drawArc(rect, -90f, sweepAngle, false, paint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }
}
