package com.lecturelens.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemLectureCardBinding;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.ui.library.CourseSection;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/** Compact recent-lecture list for the home dashboard (no overflow menu). */
public class RecentLecturesAdapter
        extends ListAdapter<Lecture, RecentLecturesAdapter.Holder> {

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

    public RecentLecturesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLectureCardBinding binding = ItemLectureCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ItemLectureCardBinding binding;
        private long lectureId = -1L;

        Holder(@NonNull ItemLectureCardBinding binding, @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.buttonLectureMenu.setVisibility(View.GONE);
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
                            R.string.lecture_meta,
                            date, formatDuration(lecture.getDurationMs())));
            binding.badgeStatus.setStatus(lecture.getStatus());
        }

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
}
