package com.example.pocket.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.pocket.R;
import com.google.android.material.button.MaterialButton;

public class PocketButton extends MaterialButton {
    private static final int VARIANT_PRIMARY = 0;
    private static final int VARIANT_SECONDARY = 1;
    private static final int VARIANT_OUTLINE = 2;
    private static final int VARIANT_GHOST = 3;
    private static final int VARIANT_DANGER = 4;
    private static final int VARIANT_DARK = 5;

    private CharSequence normalText;
    private CharSequence loadingText;
    private boolean loading;
    private boolean enabledBeforeLoading = true;
    private int variant = VARIANT_PRIMARY;

    public PocketButton(@NonNull Context context) {
        this(context, null);
    }

    public PocketButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialButtonStyle);
    }

    public PocketButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        normalText = getText();
        loadingText = context.getString(R.string.pocket_loading);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PocketButton);
            variant = typedArray.getInt(R.styleable.PocketButton_pocketVariant, VARIANT_PRIMARY);
            loading = typedArray.getBoolean(R.styleable.PocketButton_pocketLoading, false);
            String customLoadingText = typedArray.getString(R.styleable.PocketButton_pocketLoadingText);
            if (customLoadingText != null) {
                loadingText = customLoadingText;
            }
            typedArray.recycle();
        }

        setAllCaps(false);
        setMinHeight(getResources().getDimensionPixelSize(R.dimen.pocket_button_height));
        setCornerRadius(getResources().getDimensionPixelSize(R.dimen.pocket_radius_md));
        applyVariant();

        if (loading) {
            loading = false;
            setLoading(true);
        }
    }

    public void setVariant(int variant) {
        this.variant = variant;
        applyVariant();
    }

    public void setLoading(boolean loading) {
        if (this.loading == loading) {
            return;
        }

        this.loading = loading;
        if (loading) {
            normalText = getText();
            enabledBeforeLoading = isEnabled();
            setText(loadingText);
            setEnabled(false);
        } else {
            setText(normalText);
            setEnabled(enabledBeforeLoading);
        }
    }

    public boolean isLoading() {
        return loading;
    }

    private void applyVariant() {
        int backgroundColor = R.color.pocket_primary;
        int textColor = R.color.pocket_on_primary;
        int strokeColor = android.R.color.transparent;
        int strokeWidth = 0;

        if (variant == VARIANT_SECONDARY) {
            backgroundColor = R.color.pocket_primary_container;
            textColor = R.color.pocket_primary_dark;
        } else if (variant == VARIANT_OUTLINE) {
            backgroundColor = android.R.color.transparent;
            textColor = R.color.pocket_primary;
            strokeColor = R.color.pocket_outline;
            strokeWidth = getResources().getDimensionPixelSize(R.dimen.pocket_stroke_hairline);
        } else if (variant == VARIANT_GHOST) {
            backgroundColor = android.R.color.transparent;
            textColor = R.color.pocket_primary;
        } else if (variant == VARIANT_DANGER) {
            backgroundColor = R.color.pocket_danger;
            textColor = R.color.pocket_on_danger;
        } else if (variant == VARIANT_DARK) {
            backgroundColor = R.color.pocket_text_primary;
            textColor = R.color.pocket_background;
        }

        setBackgroundTintList(tint(backgroundColor));
        setTextColor(ContextCompat.getColor(getContext(), textColor));
        setIconTint(tint(textColor));
        setStrokeColor(tint(strokeColor));
        setStrokeWidth(strokeWidth);
    }

    private ColorStateList tint(int colorRes) {
        int color = colorRes == android.R.color.transparent
                ? Color.TRANSPARENT
                : ContextCompat.getColor(getContext(), colorRes);
        return ColorStateList.valueOf(color);
    }
}
