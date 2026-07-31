package com.lecturelens.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemLectureCardBinding;
import com.lecturelens.domain.model.Lecture;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Rubric ListView adapter for Home recent lectures. */
public class RecentLecturesListAdapter extends BaseAdapter {

    public interface Listener {
        void onLectureClicked(long lectureId);
    }

    @NonNull private final Listener listener;
    @NonNull private final List<Lecture> items = new ArrayList<>();

    public RecentLecturesListAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submit(@Nullable List<Lecture> lectures) {
        items.clear();
        if (lectures != null) {
            items.addAll(lectures);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Lecture getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return items.get(position).getId();
    }

    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        Context context = parent.getContext();
        ItemLectureCardBinding binding = convertView != null
                ? ItemLectureCardBinding.bind(convertView)
                : ItemLectureCardBinding.inflate(LayoutInflater.from(context), parent, false);
        Lecture lecture = items.get(position);
        binding.buttonLectureMenu.setVisibility(View.GONE);
        binding.textLectureTitle.setText(lecture.getTitle());
        String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                .format(new Date(lecture.getDate()));
        binding.textLectureMeta.setText(context.getString(
                R.string.lecture_meta, date, formatDuration(lecture.getDurationMs())));
        binding.badgeStatus.setStatus(lecture.getStatus());
        binding.getRoot().setOnClickListener(v -> listener.onLectureClicked(lecture.getId()));
        return binding.getRoot();
    }

    @NonNull
    private static String formatDuration(long durationMs) {
        if (durationMs < 0L) {
            durationMs = 0L;
        }
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs);
        if (totalSeconds < 60L) {
            return totalSeconds + " sec";
        }
        long minutes = totalSeconds / 60L;
        return minutes + " min";
    }
}
