package com.example.pocket.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;

import com.example.pocket.R;
import com.bumptech.glide.Glide;

import java.util.Locale;

public class AvatarView extends FrameLayout {
    private final AppCompatImageView imageView;
    private final TextView initialsView;

    public AvatarView(@NonNull Context context) {
        this(context, null);
    }

    public AvatarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        int defaultSize = getResources().getDimensionPixelSize(R.dimen.pocket_avatar_md);
        int backgroundColor = ContextCompat.getColor(context, R.color.pocket_primary_container);
        String avatarText = null;
        int imageResId = 0;
        int requestedSize = defaultSize;

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.AvatarView);
            avatarText = typedArray.getString(R.styleable.AvatarView_avatarText);
            imageResId = typedArray.getResourceId(R.styleable.AvatarView_avatarImage, 0);
            requestedSize = typedArray.getDimensionPixelSize(R.styleable.AvatarView_avatarSize, defaultSize);
            backgroundColor = typedArray.getColor(R.styleable.AvatarView_avatarBackgroundColor, backgroundColor);
            typedArray.recycle();
        }

        setMinimumWidth(requestedSize);
        setMinimumHeight(requestedSize);
        setBackground(circle(backgroundColor));
        setClipToOutline(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setOval(0, 0, view.getWidth(), view.getHeight());
                }
            });
        }

        imageView = new AppCompatImageView(context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        addView(imageView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        initialsView = new TextView(context);
        initialsView.setGravity(Gravity.CENTER);
        initialsView.setTypeface(Typeface.DEFAULT_BOLD);
        initialsView.setTextColor(ContextCompat.getColor(context, R.color.pocket_primary_dark));
        initialsView.setTextSize(14);
        addView(initialsView, new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        if (imageResId != 0) {
            setAvatarImageResource(imageResId);
        } else {
            setAvatarText(avatarText);
        }
    }

    public void setAvatarText(@Nullable CharSequence text) {
        imageView.setVisibility(GONE);
        initialsView.setVisibility(VISIBLE);
        initialsView.setText(toInitials(text));
    }

    public void setAvatarUrl(@Nullable String url) {
        setAvatarUrl(url, null);
    }

    public void setAvatarUrl(@Nullable String url, @Nullable String nameFallback) {
        if (url != null && !url.trim().isEmpty()) {
            imageView.setVisibility(VISIBLE);
            initialsView.setVisibility(GONE);
            Glide.with(getContext())
                    .load(url)
                    .circleCrop()
                    .placeholder(R.drawable.avatar_placeholder)
                    .error(R.drawable.avatar_placeholder)
                    .into(imageView);
        } else {
            setAvatarText(nameFallback);
        }
    }

    public void setAvatarImageResource(@DrawableRes int imageResId) {
        imageView.setImageResource(imageResId);
        imageView.setVisibility(VISIBLE);
        initialsView.setVisibility(GONE);
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private String toInitials(@Nullable CharSequence text) {
        if (text == null) {
            return "?";
        }

        String value = text.toString().trim();
        if (value.isEmpty()) {
            return "?";
        }

        String[] parts = value.split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.substring(0, 1));
            }
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString().toUpperCase(Locale.getDefault());
    }
}
