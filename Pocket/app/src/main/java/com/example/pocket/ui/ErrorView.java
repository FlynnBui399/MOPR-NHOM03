package com.example.pocket.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pocket.R;

public class ErrorView extends LinearLayout {
    private final TextView titleView;
    private final TextView messageView;
    private final PocketButton actionButton;

    public ErrorView(@NonNull Context context) {
        this(context, null);
    }

    public ErrorView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        int padding = getResources().getDimensionPixelSize(R.dimen.pocket_space_xl);
        setPadding(padding, padding, padding, padding);

        TextView iconView = new TextView(context);
        iconView.setText("!");
        iconView.setGravity(Gravity.CENTER);
        iconView.setTypeface(Typeface.DEFAULT_BOLD);
        iconView.setTextColor(ContextCompat.getColor(context, R.color.pocket_danger));
        iconView.setTextSize(24);
        addView(iconView, new LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.pocket_avatar_lg),
                getResources().getDimensionPixelSize(R.dimen.pocket_avatar_lg)
        ));

        titleView = new TextView(context);
        titleView.setGravity(Gravity.CENTER);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(ContextCompat.getColor(context, R.color.pocket_text_primary));
        titleView.setTextSize(18);
        titleView.setText(context.getString(R.string.pocket_error_title));
        LayoutParams titleParams = wrapContentParams();
        titleParams.topMargin = getResources().getDimensionPixelSize(R.dimen.pocket_space_sm);
        addView(titleView, titleParams);

        messageView = new TextView(context);
        messageView.setGravity(Gravity.CENTER);
        messageView.setTextColor(ContextCompat.getColor(context, R.color.pocket_text_secondary));
        messageView.setTextSize(14);
        messageView.setText(context.getString(R.string.pocket_error_message));
        LayoutParams messageParams = wrapContentParams();
        messageParams.topMargin = getResources().getDimensionPixelSize(R.dimen.pocket_space_xs);
        addView(messageView, messageParams);

        actionButton = new PocketButton(context);
        actionButton.setText(context.getString(R.string.pocket_error_action));
        LayoutParams buttonParams = wrapContentParams();
        buttonParams.topMargin = getResources().getDimensionPixelSize(R.dimen.pocket_space_lg);
        addView(actionButton, buttonParams);

        readAttrs(context, attrs);
    }

    public void setTitle(CharSequence title) {
        titleView.setText(title);
    }

    public void setMessage(CharSequence message) {
        messageView.setText(message);
    }

    public void setActionText(CharSequence actionText) {
        actionButton.setText(actionText);
        actionButton.setVisibility(actionText == null || actionText.length() == 0 ? GONE : VISIBLE);
    }

    public void setOnRetryClickListener(@Nullable View.OnClickListener listener) {
        actionButton.setOnClickListener(listener);
        actionButton.setVisibility(listener == null ? GONE : VISIBLE);
    }

    private void readAttrs(Context context, @Nullable AttributeSet attrs) {
        if (attrs == null) {
            return;
        }

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.ErrorView);
        String title = typedArray.getString(R.styleable.ErrorView_errorTitle);
        String message = typedArray.getString(R.styleable.ErrorView_errorMessage);
        String actionText = typedArray.getString(R.styleable.ErrorView_errorActionText);
        typedArray.recycle();

        if (title != null) {
            setTitle(title);
        }
        if (message != null) {
            setMessage(message);
        }
        if (actionText != null) {
            setActionText(actionText);
        }
    }

    private LayoutParams wrapContentParams() {
        return new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
    }
}
