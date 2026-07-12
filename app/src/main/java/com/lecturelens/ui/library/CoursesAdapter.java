package com.lecturelens.ui.library;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemCourseHeaderBinding;
import com.lecturelens.domain.model.Course;

/**
 * Track 2 (Daniel). Outer Library list: one expandable section per course,
 * lectures rendered by a nested {@link LecturesAdapter}. Header tap toggles
 * expansion via {@link Listener#onCourseToggled}.
 */
public class CoursesAdapter extends ListAdapter<CourseSection, CoursesAdapter.CourseViewHolder> {

    public interface Listener extends LecturesAdapter.Listener {
        void onCourseToggled(long courseId);
    }

    private static final DiffUtil.ItemCallback<CourseSection> DIFF =
            new DiffUtil.ItemCallback<CourseSection>() {
                @Override
                public boolean areItemsTheSame(@NonNull CourseSection oldItem,
                                               @NonNull CourseSection newItem) {
                    return oldItem.getCourse().getId() == newItem.getCourse().getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull CourseSection oldItem,
                                                  @NonNull CourseSection newItem) {
                    return oldItem.contentEquals(newItem);
                }
            };

    @NonNull private final Listener listener;
    private final RecyclerView.RecycledViewPool lecturePool =
            new RecyclerView.RecycledViewPool();

    public CoursesAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCourseHeaderBinding binding = ItemCourseHeaderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CourseViewHolder(binding, listener, lecturePool);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {

        private final ItemCourseHeaderBinding binding;
        private final LecturesAdapter lecturesAdapter;
        private long courseId = -1L;

        CourseViewHolder(@NonNull ItemCourseHeaderBinding binding,
                         @NonNull Listener listener,
                         @NonNull RecyclerView.RecycledViewPool lecturePool) {
            super(binding.getRoot());
            this.binding = binding;
            lecturesAdapter = new LecturesAdapter(listener);
            binding.recyclerLectures.setLayoutManager(
                    new LinearLayoutManager(binding.getRoot().getContext()));
            binding.recyclerLectures.setAdapter(lecturesAdapter);
            binding.recyclerLectures.setRecycledViewPool(lecturePool);
            binding.recyclerLectures.setNestedScrollingEnabled(false);
            binding.headerRow.setOnClickListener(v -> {
                if (courseId != -1L) {
                    listener.onCourseToggled(courseId);
                }
            });
        }

        void bind(@NonNull CourseSection section) {
            Course course = section.getCourse();
            courseId = course.getId();
            binding.textCourseName.setText(
                    courseId == LibraryViewModel.UNCATEGORIZED_COURSE_ID
                            ? binding.getRoot().getContext()
                                    .getString(R.string.library_uncategorized)
                            : course.getName());
            binding.viewCourseColor.setBackgroundTintList(
                    ColorStateList.valueOf(course.getColor()));
            int count = section.getLectures().size();
            binding.textLectureCount.setText(
                    binding.getRoot().getResources().getQuantityString(
                            R.plurals.lecture_count, count, count));
            binding.imageExpand.setRotation(section.isExpanded() ? 180f : 0f);
            binding.recyclerLectures.setVisibility(
                    section.isExpanded() ? View.VISIBLE : View.GONE);
            lecturesAdapter.submitList(section.getLectures());
        }
    }
}
