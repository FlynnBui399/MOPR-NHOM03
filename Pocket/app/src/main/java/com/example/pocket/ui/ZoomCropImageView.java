package com.example.pocket.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class ZoomCropImageView extends AppCompatImageView {
    private Bitmap originalBitmap;
    private final Matrix matrix = new Matrix();
    private float minScale = 1.0f;
    private float maxScale = 5.0f;

    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;
    private float lastX, lastY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    public ZoomCropImageView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ZoomCropImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ZoomCropImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setScaleType(ScaleType.MATRIX);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(View view, android.graphics.Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), view.getResources().getDisplayMetrics().density * 36);
                }
            });
            setClipToOutline(true);
        }

        scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float scaleFactor = detector.getScaleFactor();
                float[] values = new float[9];
                matrix.getValues(values);
                float currentScale = values[Matrix.MSCALE_X];
                if (currentScale * scaleFactor < minScale) {
                    scaleFactor = minScale / currentScale;
                } else if (currentScale * scaleFactor > maxScale) {
                    scaleFactor = maxScale / currentScale;
                }
                matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
                clampMatrix();
                return true;
            }
        });

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                if (!scaleDetector.isInProgress()) {
                    matrix.postTranslate(-distanceX, -distanceY);
                    clampMatrix();
                }
                return true;
            }
        });
    }

    public void setImage(Bitmap bitmap) {
        this.originalBitmap = bitmap;
        super.setImageBitmap(bitmap);
        setScaleType(ScaleType.MATRIX);
        post(this::initBaseMatrix);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (originalBitmap != null) {
            initBaseMatrix();
        }
    }

    private void initBaseMatrix() {
        if (originalBitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float imageWidth = originalBitmap.getWidth();
        float imageHeight = originalBitmap.getHeight();

        minScale = Math.max(viewWidth / imageWidth, viewHeight / imageHeight);
        maxScale = minScale * 5.0f;

        float dx = (viewWidth - imageWidth * minScale) / 2f;
        float dy = (viewHeight - imageHeight * minScale) / 2f;

        matrix.setScale(minScale, minScale);
        matrix.postTranslate(dx, dy);
        setImageMatrix(matrix);
    }

    private void clampMatrix() {
        if (originalBitmap == null || getWidth() == 0 || getHeight() == 0) return;
        float[] values = new float[9];
        matrix.getValues(values);
        float currentScale = values[Matrix.MSCALE_X];
        float x = values[Matrix.MTRANS_X];
        float y = values[Matrix.MTRANS_Y];

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float scaledWidth = originalBitmap.getWidth() * currentScale;
        float scaledHeight = originalBitmap.getHeight() * currentScale;

        float minX = viewWidth - scaledWidth;
        float minY = viewHeight - scaledHeight;

        float newX = x;
        float newY = y;

        if (newX > 0) newX = 0;
        if (newX < minX) newX = minX;

        if (newY > 0) newY = 0;
        if (newY < minY) newY = minY;

        values[Matrix.MTRANS_X] = newX;
        values[Matrix.MTRANS_Y] = newY;
        matrix.setValues(values);
        setImageMatrix(matrix);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (originalBitmap == null) return false;

        android.view.ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
            Log.d("ZoomCropImageView", "requestDisallowInterceptTouchEvent(true) called for action " + event.getActionMasked());
        }

        scaleDetector.onTouchEvent(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastX = event.getX();
                lastY = event.getY();
                Log.d("ZoomCropImageView", "ACTION_DOWN: activePointerId=" + activePointerId);
                break;
            case MotionEvent.ACTION_POINTER_DOWN: {
                int actionIndex = event.getActionIndex();
                activePointerId = event.getPointerId(actionIndex);
                lastX = event.getX(actionIndex);
                lastY = event.getY(actionIndex);
                Log.d("ZoomCropImageView", "ACTION_POINTER_DOWN: activePointerId=" + activePointerId);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex != -1) {
                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);
                    if (!scaleDetector.isInProgress()) {
                        float dx = x - lastX;
                        float dy = y - lastY;
                        matrix.postTranslate(dx, dy);
                        clampMatrix();
                        Log.d("ZoomCropImageView", "ACTION_MOVE: dx=" + dx + ", dy=" + dy);
                    }
                    lastX = x;
                    lastY = y;
                }
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                int actionIndex = event.getActionIndex();
                int pointerId = event.getPointerId(actionIndex);
                if (pointerId == activePointerId) {
                    int newIndex = actionIndex == 0 ? 1 : 0;
                    activePointerId = event.getPointerId(newIndex);
                    lastX = event.getX(newIndex);
                    lastY = event.getY(newIndex);
                }
                Log.d("ZoomCropImageView", "ACTION_POINTER_UP: activePointerId=" + activePointerId);
                break;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                Log.d("ZoomCropImageView", "ACTION_UP/CANCEL");
                break;
        }
        return true;
    }

    public Bitmap getCroppedBitmap(int targetWidth, int targetHeight) {
        if (originalBitmap == null || getWidth() == 0 || getHeight() == 0) {
            return originalBitmap;
        }
        float[] values = new float[9];
        matrix.getValues(values);
        float currentScale = values[Matrix.MSCALE_X];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        int sourceX = Math.round(-transX / currentScale);
        int sourceY = Math.round(-transY / currentScale);
        int sourceWidth = Math.round(getWidth() / currentScale);
        int sourceHeight = Math.round(getHeight() / currentScale);

        sourceX = Math.max(0, Math.min(sourceX, originalBitmap.getWidth() - 1));
        sourceY = Math.max(0, Math.min(sourceY, originalBitmap.getHeight() - 1));
        sourceWidth = Math.max(1, Math.min(sourceWidth, originalBitmap.getWidth() - sourceX));
        sourceHeight = Math.max(1, Math.min(sourceHeight, originalBitmap.getHeight() - sourceY));

        Log.d("ZoomCropImageView", "Final crop rectangle: sourceX=" + sourceX + ", sourceY=" + sourceY + ", sourceWidth=" + sourceWidth + ", sourceHeight=" + sourceHeight);

        Bitmap cropped = Bitmap.createBitmap(originalBitmap, sourceX, sourceY, sourceWidth, sourceHeight);
        if (targetWidth > 0 && targetHeight > 0) {
            Bitmap scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true);
            if (cropped != scaled && cropped != originalBitmap) {
                cropped.recycle();
            }
            return scaled;
        }
        return cropped;
    }
}
