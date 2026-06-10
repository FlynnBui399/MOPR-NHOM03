package com.example.pocket.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pocket.R;

public class ShimmerView extends View {
    private static final long SHIMMER_DURATION_MS = 1200L;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix gradientMatrix = new Matrix();
    private final RectF bounds = new RectF();
    private final float cornerRadius;
    private final int baseColor;
    private final int highlightColor;

    private LinearGradient gradient;
    private ValueAnimator animator;
    private float shimmerOffset;

    public ShimmerView(@NonNull Context context) {
        this(context, null);
    }

    public ShimmerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        cornerRadius = getResources().getDimension(R.dimen.pocket_radius_lg);
        baseColor = ContextCompat.getColor(context, R.color.pocket_shimmer_base);
        highlightColor = ContextCompat.getColor(context, R.color.pocket_shimmer_highlight);
        paint.setColor(baseColor);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startShimmer();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopShimmer();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        bounds.set(0, 0, width, height);
        createGradient(width);
        restartShimmer();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (gradient != null) {
            gradientMatrix.setTranslate(shimmerOffset, 0);
            gradient.setLocalMatrix(gradientMatrix);
        }
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, paint);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE) {
            startShimmer();
        } else {
            stopShimmer();
        }
    }

    public void startShimmer() {
        if (getWidth() == 0 || !isShown() || animationsDisabled()) {
            return;
        }
        if (animator != null && animator.isStarted()) {
            return;
        }

        float travel = getWidth() * 2f;
        animator = ValueAnimator.ofFloat(-getWidth(), travel);
        animator.setDuration(SHIMMER_DURATION_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            shimmerOffset = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    public void stopShimmer() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void restartShimmer() {
        stopShimmer();
        startShimmer();
    }

    private void createGradient(int width) {
        if (width <= 0) {
            gradient = null;
            paint.setShader(null);
            paint.setColor(baseColor);
            return;
        }

        gradient = new LinearGradient(
                -width,
                0,
                0,
                0,
                new int[]{baseColor, highlightColor, baseColor},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP
        );
        paint.setShader(gradient);
    }

    private boolean animationsDisabled() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ValueAnimator.areAnimatorsEnabled();
    }
}
