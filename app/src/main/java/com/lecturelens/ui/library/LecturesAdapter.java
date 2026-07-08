package com.lecturelens.ui.library;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.databinding.ItemLectureCardBinding;
import com.lecturelens.domain.model.Lecture;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Track 2 (Daniel). Lecture cards inside one course section — nested inside
 * {@link CoursesAdapter}. Tap → Library navigates to the lecture screen.
 */
public class LecturesAdapter extends ListAdapter<Lecture, LecturesAdapter.LectureViewHolder> {

    public interface Listener {
        void onLectureClicked(long lectureId);
    }

    private static final DiffUtil.ItemCallback<Lecture> DIFF =
            new DiffUtil.ItemCallback<Lecture>() {
                @Override
                public boolean areItemsTheSame(@NonNull Lecture oldItem,
                                               @NonNull Lecture newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Lecture oldItem,
                                                  @NonNull Lecture newItem) {
                    return CourseSection.lectureContentEquals(oldItem, newItem);
                }
            };

    @NonNull private final Listener listener;

    public LecturesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public LectureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLectureCardBinding binding = ItemLectureCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new LectureViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull LectureViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class LectureViewHolder extends RecyclerView.ViewHolder {

        private final ItemLectureCardBinding binding;
        private long lectureId = -1L;

        LectureViewHolder(@NonNull ItemLectureCardBinding binding,
                          @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(v -> {
                if (lectureId != -1L) {
                    listener.onLectureClicked(lectureId);
                }
            });
        }

        void bind(@NonNull Lecture lecture) {
            lectureId = lecture.getId();
            binding.textLectureTitle.setText(lecture.getTitle());
            String date = DateFormat.getDateInstance(DateFormat.MEDIUM)
                    .format(new Date(lecture.getDate()));
            binding.textLectureMeta.setText(
                    binding.getRoot().getContext().getString(
                            com.lecturelens.R.string.lecture_meta,
                            date, formatDuration(lecture.getDurationMs())));
            binding.badgeStatus.setStatus(lecture.getStatus());
        }

        private static String formatDuration(long durationMs) {
            long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(durationMs);
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            if (hours > 0) {
                return hours + " h " + minutes + " min";
            }
            return minutes + " min";
        }
    }
}
