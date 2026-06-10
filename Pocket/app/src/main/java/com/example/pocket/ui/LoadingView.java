package com.example.pocket.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pocket.R;

public class LoadingView extends LinearLayout {
    private final TextView messageView;

    public LoadingView(@NonNull Context context) {
        this(context, null);
    }

    public LoadingView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        int padding = getResources().getDimensionPixelSize(R.dimen.pocket_space_lg);
        setPadding(padding, padding, padding, padding);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.pocket_button_height));

        ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleSmall);
        addView(progressBar, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        messageView = new TextView(context);
        messageView.setTextColor(ContextCompat.getColor(context, R.color.pocket_text_secondary));
        messageView.setTextSize(14);
        messageView.setText(context.getString(R.string.pocket_loading));

        LayoutParams textParams = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        textParams.setMarginStart(getResources().getDimensionPixelSize(R.dimen.pocket_space_sm));
        addView(messageView, textParams);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);
            String loadingText = typedArray.getString(R.styleable.LoadingView_loadingText);
            if (loadingText != null) {
                setLoadingText(loadingText);
            }
            typedArray.recycle();
        }
    }

    public void setLoadingText(CharSequence text) {
        messageView.setText(text);
    }
}
