package com.lecturelens.ui.library;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemLectureCardBinding;
import com.lecturelens.domain.model.Lecture;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Lecture cards inside one course section. Tap → open. Long-press → rename / move.
 */
public class LecturesAdapter extends ListAdapter<Lecture, LecturesAdapter.LectureViewHolder> {

    public interface Listener {
        void onLectureClicked(long lectureId);

        void onRenameLecture(long lectureId, @NonNull String currentTitle);

        void onMoveLecture(long lectureId);
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
        private final Listener listener;
        private long lectureId = -1L;
        @NonNull private String lectureTitle = "";

        LectureViewHolder(@NonNull ItemLectureCardBinding binding,
                          @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            binding.getRoot().setOnClickListener(v -> {
                if (lectureId != -1L) {
                    listener.onLectureClicked(lectureId);
                }
            });
            binding.getRoot().setOnLongClickListener(v -> {
                if (lectureId != -1L) {
                    showLectureMenu(v);
                }
                return true;
            });
            binding.buttonLectureMenu.setOnClickListener(this::showLectureMenu);
        }

        private void showLectureMenu(@NonNull View anchor) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_lecture_actions);
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_rename_lecture) {
                    listener.onRenameLecture(lectureId, lectureTitle);
                    return true;
                }
                if (id == R.id.action_move_lecture) {
                    listener.onMoveLecture(lectureId);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        void bind(@NonNull Lecture lecture) {
            lectureId = lecture.getId();
            lectureTitle = lecture.getTitle();
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
