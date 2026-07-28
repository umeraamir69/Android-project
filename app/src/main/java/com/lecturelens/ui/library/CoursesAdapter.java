package com.lecturelens.ui.library;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemCourseHeaderBinding;
import com.lecturelens.domain.model.Course;

/**
 * Outer Library list: expandable course sections with nested lectures.
 * Tap header → expand/collapse. Long-press header → category actions.
 */
public class CoursesAdapter extends ListAdapter<CourseSection, CoursesAdapter.CourseViewHolder> {

    public interface Listener extends LecturesAdapter.Listener {
        void onCourseToggled(long courseId);

        void onRenameCourse(long courseId, @NonNull String currentName);

        void onDeleteCourse(long courseId, @NonNull String currentName);

        void onRecordInCourse(long courseId);
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
        private final Listener listener;
        private long courseId = -1L;
        @NonNull private String courseName = "";

        CourseViewHolder(@NonNull ItemCourseHeaderBinding binding,
                         @NonNull Listener listener,
                         @NonNull RecyclerView.RecycledViewPool lecturePool) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            lecturesAdapter = new LecturesAdapter(listener);
            binding.recyclerLectures.setLayoutManager(
                    new LinearLayoutManager(binding.getRoot().getContext()));
            binding.recyclerLectures.setAdapter(lecturesAdapter);
            binding.recyclerLectures.setRecycledViewPool(lecturePool);
            binding.recyclerLectures.setNestedScrollingEnabled(false);
            binding.headerRow.setOnClickListener(v -> listener.onCourseToggled(courseId));
            binding.headerRow.setOnLongClickListener(v -> {
                showCourseMenu(v);
                return true;
            });
            binding.buttonCourseMenu.setOnClickListener(this::showCourseMenu);
        }

        private void showCourseMenu(@NonNull View anchor) {
            PopupMenu menu = new PopupMenu(anchor.getContext(), anchor);
            menu.inflate(R.menu.menu_course_actions);
            boolean realCourse = courseId != LibraryViewModel.UNCATEGORIZED_COURSE_ID;
            menu.getMenu().findItem(R.id.action_rename_course).setVisible(realCourse);
            menu.getMenu().findItem(R.id.action_delete_course).setVisible(realCourse);
            menu.getMenu().findItem(R.id.action_record_in_course).setVisible(realCourse);
            if (!realCourse) {
                return; // Uncategorized has no actions
            }
            menu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_rename_course) {
                    listener.onRenameCourse(courseId, courseName);
                    return true;
                }
                if (id == R.id.action_delete_course) {
                    listener.onDeleteCourse(courseId, courseName);
                    return true;
                }
                if (id == R.id.action_record_in_course) {
                    listener.onRecordInCourse(courseId);
                    return true;
                }
                return false;
            });
            menu.show();
        }

        void bind(@NonNull CourseSection section) {
            Course course = section.getCourse();
            courseId = course.getId();
            courseName = course.getName();
            boolean realCourse = courseId != LibraryViewModel.UNCATEGORIZED_COURSE_ID;
            binding.buttonCourseMenu.setVisibility(realCourse ? View.VISIBLE : View.GONE);
            binding.textCourseName.setText(
                    realCourse
                            ? course.getName()
                            : binding.getRoot().getContext()
                                    .getString(R.string.library_uncategorized));
            binding.viewCourseColor.setBackgroundTintList(
                    ColorStateList.valueOf(course.getColor()));
            int count = section.getLectures().size();
            binding.textLectureCount.setText(String.valueOf(count));
            binding.textLectureCount.setContentDescription(
                    binding.getRoot().getResources().getQuantityString(
                            R.plurals.lecture_count, count, count));
            binding.imageExpand.setRotation(section.isExpanded() ? 180f : 0f);
            binding.recyclerLectures.setVisibility(
                    section.isExpanded() ? View.VISIBLE : View.GONE);
            lecturesAdapter.submitList(section.getLectures());
        }
    }
}
