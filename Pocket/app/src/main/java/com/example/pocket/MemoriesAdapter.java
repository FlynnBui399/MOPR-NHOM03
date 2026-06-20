package com.example.pocket;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MemoriesAdapter extends RecyclerView.Adapter<MemoriesAdapter.MonthViewHolder> {
    public interface MemoryClickListener {
        void onMemoryClick(@NonNull Photo photo);
    }

    private final List<MonthSection> months = new ArrayList<>();
    private final MemoryClickListener clickListener;

    public MemoriesAdapter(@NonNull MemoryClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitPhotos(@NonNull List<Photo> photos) {
        months.clear();
        months.addAll(buildMonths(photos));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MonthViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MonthViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_memory_month, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull MonthViewHolder holder, int position) {
        holder.bind(months.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return months.size();
    }

    private static List<MonthSection> buildMonths(@NonNull List<Photo> photos) {
        List<MonthSection> result = new ArrayList<>();
        if (photos.isEmpty()) {
            return result;
        }

        Map<String, List<Photo>> byDay = new LinkedHashMap<>();
        Calendar earliest = null;
        Calendar latestPhoto = null;
        SimpleDateFormat dayKey = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (Photo photo : photos) {
            if (photo.getCreatedAt() == null) {
                continue;
            }
            Date date = photo.getCreatedAt().toDate();
            byDay.computeIfAbsent(dayKey.format(date), ignored -> new ArrayList<>()).add(photo);
            Calendar value = Calendar.getInstance();
            value.setTime(date);
            value.set(Calendar.DAY_OF_MONTH, 1);
            clearTime(value);
            if (earliest == null || value.before(earliest)) {
                earliest = (Calendar) value.clone();
            }
            if (latestPhoto == null || value.after(latestPhoto)) {
                latestPhoto = (Calendar) value.clone();
            }
        }
        if (earliest == null) {
            return result;
        }

        Calendar currentMonth = Calendar.getInstance();
        currentMonth.set(Calendar.DAY_OF_MONTH, 1);
        clearTime(currentMonth);
        Calendar end = latestPhoto != null && latestPhoto.after(currentMonth)
                ? latestPhoto : currentMonth;
        Calendar cursor = (Calendar) earliest.clone();
        while (!cursor.after(end)) {
            result.add(new MonthSection((Calendar) cursor.clone(), byDay));
            cursor.add(Calendar.MONTH, 1);
        }
        return result;
    }

    private static void clearTime(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    static class MonthSection {
        final Calendar month;
        final Map<String, List<Photo>> photosByDay;

        MonthSection(@NonNull Calendar month, @NonNull Map<String, List<Photo>> photosByDay) {
            this.month = month;
            this.photosByDay = photosByDay;
        }
    }

    static class MonthViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final GridLayout grid;

        MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.memory_month_title);
            grid = itemView.findViewById(R.id.memory_month_grid);
        }

        void bind(@NonNull MonthSection section, @NonNull MemoryClickListener listener) {
            Context context = itemView.getContext();
            title.setText(new SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    .format(section.month.getTime()));
            grid.removeAllViews();

            Calendar first = (Calendar) section.month.clone();
            int leadingCells = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
            int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
            SimpleDateFormat keyFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            LayoutInflater inflater = LayoutInflater.from(context);
            for (int cell = 0; cell < 42; cell++) {
                View dayView = inflater.inflate(R.layout.item_memory_day, grid, false);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(cell / 7, 1),
                        GridLayout.spec(cell % 7, 1, 1f));
                params.width = 0;
                params.height = dp(context, 54);
                params.setMargins(dp(context, 3), dp(context, 3),
                        dp(context, 3), dp(context, 3));
                dayView.setLayoutParams(params);

                int dayNumber = cell - leadingCells + 1;
                ImageView thumbnail = dayView.findViewById(R.id.memory_day_thumbnail);
                ImageView playIcon = dayView.findViewById(R.id.memory_day_play_icon);
                View dot = dayView.findViewById(R.id.memory_day_dot);
                TextView count = dayView.findViewById(R.id.memory_day_count);
                TextView number = dayView.findViewById(R.id.memory_day_number);
                if (dayNumber < 1 || dayNumber > daysInMonth) {
                    dayView.setVisibility(View.INVISIBLE);
                } else {
                    number.setText(String.valueOf(dayNumber));
                    Calendar date = (Calendar) first.clone();
                    date.set(Calendar.DAY_OF_MONTH, dayNumber);
                    List<Photo> dayPhotos = section.photosByDay.get(keyFormat.format(date.getTime()));
                    if (dayPhotos == null || dayPhotos.isEmpty()) {
                        dot.setVisibility(View.VISIBLE);
                        if (playIcon != null) {
                            playIcon.setVisibility(View.GONE);
                        }
                    } else {
                        Photo newest = dayPhotos.get(0);
                        thumbnail.setVisibility(View.VISIBLE);
                        dot.setVisibility(View.GONE);
                        boolean isVideo = "video".equals(newest.getType());
                        if (playIcon != null) {
                            playIcon.setVisibility(isVideo ? View.VISIBLE : View.GONE);
                        }
                        String url = newest.getThumbnailUrl();
                        if (url == null || url.trim().isEmpty()) {
                            url = newest.getImageUrl();
                        }
                        Glide.with(thumbnail)
                                .load(isVideo && newest.getVideoUrl() != null ? newest.getVideoUrl() : url)
                                .centerCrop()
                                .placeholder(R.drawable.placeholder_pocket)
                                .error(R.drawable.placeholder_pocket)
                                .into(thumbnail);
                        if (dayPhotos.size() > 1) {
                            count.setVisibility(View.VISIBLE);
                            count.setText(String.valueOf(dayPhotos.size()));
                        }
                        dayView.setOnClickListener(view -> listener.onMemoryClick(newest));
                    }
                }
                grid.addView(dayView);
            }
        }

        private static int dp(@NonNull Context context, int value) {
            return Math.round(value * context.getResources().getDisplayMetrics().density);
        }
    }
}
