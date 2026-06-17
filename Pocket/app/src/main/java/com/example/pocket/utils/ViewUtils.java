package com.example.pocket.utils;

import android.view.MotionEvent;
import android.view.View;

public class ViewUtils {
    public static void applyPressAnimation(View view) {
        if (view == null) return;
        view.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.90f).scaleY(0.90f).setDuration(80).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100)
                      .withEndAction(() -> v.performClick()).start();
                    break;
            }
            return true;
        });
    }
}
