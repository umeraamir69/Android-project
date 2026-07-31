package com.lecturelens.ui.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemCourseHeaderBinding;
import com.lecturelens.databinding.ItemLectureCardBinding;
import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.model.Lecture;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Rubric ListView adapter — course headers + lectures. */
public class LibraryListAdapter extends BaseAdapter {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_LECTURE = 1;

    public interface Listener extends CoursesAdapter.Listener {
    }

    private static final class Row {
        final int type;
        @Nullable final CourseSection section;
        @Nullable final Lecture lecture;

        Row(@NonNull CourseSection section) {
            this.type = TYPE_HEADER;
            this.section = section;
            this.lecture = null;
        }

        Row(@NonNull Lecture lecture) {
            this.type = TYPE_LECTURE;
            this.section = null;
            this.lecture = lecture;
        }
    }

    @NonNull private final Listener listener;
    @NonNull private final List<Row> rows = new ArrayList<>();

    public LibraryListAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void submitSections(@Nullable List<CourseSection> sections) {
        rows.clear();
        if (sections != null) {
            for (CourseSection section : sections) {
                rows.add(new Row(section));
                if (section.isExpanded()) {
                    for (Lecture lecture : section.getLectures()) {
                        rows.add(new Row(lecture));
                    }
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        Row row = rows.get(position);
        if (row.type == TYPE_HEADER && row.section != null) {
            return row.section.getCourse().getId();
        }
        return row.lecture != null ? row.lecture.getId() : position;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Row row = rows.get(position);
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        if (row.type == TYPE_HEADER) {
            ItemCourseHeaderBinding binding = convertView != null
                    ? ItemCourseHeaderBinding.bind(convertView)
                    : ItemCourseHeaderBinding.inflate(inflater, parent, false);
            binding.recyclerLectures.setVisibility(View.GONE);
            CourseSection section = row.section;
            if (section != null) {
                Course course = section.getCourse();
                boolean uncategorized = course.getId() == LibraryViewModel.UNCATEGORIZED_COURSE_ID;
                binding.textCourseName.setText(uncategorized
                        ? context.getString(R.string.library_uncategorized)
                        : course.getName());
                binding.textLectureCount.setText(String.valueOf(section.getLectures().size()));
                binding.getRoot().setOnClickListener(v ->
                        listener.onCourseToggled(course.getId()));
                binding.buttonCourseMenu.setOnClickListener(v -> {
                    if (!uncategorized) {
                        listener.onRenameCourse(
                                course.getId(), course.getName(), course.getProfessor());
                    }
                });
            }
            return binding.getRoot();
        }

        ItemLectureCardBinding binding = convertView != null
                ? ItemLectureCardBinding.bind(convertView)
                : ItemLectureCardBinding.inflate(inflater, parent, false);
        Lecture lecture = row.lecture;
        if (lecture != null) {
            binding.textLectureTitle.setText(lecture.getTitle());
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(lecture.getDate()));
            binding.textLectureMeta.setText(context.getString(
                    R.string.lecture_meta, date, formatDuration(lecture.getDurationMs())));
            binding.badgeStatus.setStatus(lecture.getStatus());
            binding.buttonLectureMenu.setVisibility(View.GONE);
            binding.getRoot().setOnClickListener(v ->
                    listener.onLectureClicked(lecture.getId()));
            binding.getRoot().setOnLongClickListener(v -> {
                listener.onRenameLecture(lecture.getId(), lecture.getTitle());
                return true;
            });
        }
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
        long totalMinutes = totalSeconds / 60L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours > 0) {
            return hours + " h " + minutes + " min";
        }
        return minutes + " min";
    }
}
