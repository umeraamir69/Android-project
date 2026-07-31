package com.lecturelens.ui.library;

import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentLibraryBinding;
import com.lecturelens.domain.model.Course;
import com.lecturelens.ui.util.AppNavigator;
import com.lecturelens.ui.util.HelpDialogs;
import com.lecturelens.ui.util.SectionFeedback;
import com.lecturelens.ui.util.UiAnimations;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Library screen: expandable categories, create/rename/delete categories,
 * and move/rename lectures. FAB → record (Uncategorized unless opened from a category).
 */
@AndroidEntryPoint
public class LibraryFragment extends Fragment implements LibraryListAdapter.Listener {

    @Nullable private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private LibraryListAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LibraryViewModel.class);

        UiAnimations.animateScreenEnter(binding.getRoot());
        adapter = new LibraryListAdapter(this);
        binding.listLibrary.setAdapter(adapter);
        UiAnimations.bindPressScale(binding.fabRecord);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_search) {
                AppNavigator.openSearchActivity(this);
                return true;
            }
            if (id == R.id.menu_add_category) {
                showAddCategoryDialog();
                return true;
            }
            if (id == R.id.menu_settings) {
                AppNavigator.openSettingsActivity(this);
                return true;
            }
            if (id == R.id.menu_help) {
                HelpDialogs.show(this, getString(R.string.title_library));
                return true;
            }
            return false;
        });
        binding.fabRecord.setOnClickListener(v ->
                AppNavigator.openUploadActivity(this));
        binding.emptyState.buttonEmptyCta.setOnClickListener(v ->
                AppNavigator.openUploadActivity(this));
        SectionFeedback.toast(this, getString(R.string.toast_section_ready,
                getString(R.string.title_library)));
        binding.buttonExpandToggle.setOnClickListener(v -> viewModel.toggleExpandCollapse());
        binding.chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            int checked = checkedIds.get(0);
            LibraryFilter filter = LibraryFilter.ALL;
            if (checked == R.id.chip_filter_ready) {
                filter = LibraryFilter.READY;
            } else if (checked == R.id.chip_filter_failed) {
                filter = LibraryFilter.FAILED;
            } else if (checked == R.id.chip_filter_processing) {
                filter = LibraryFilter.PROCESSING;
            } else if (checked == R.id.chip_filter_shared) {
                filter = LibraryFilter.SHARED;
            } else if (checked == R.id.chip_filter_recorded) {
                filter = LibraryFilter.RECORDED;
            }
            viewModel.setStatusFilter(filter);
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
    }

    private void render(@NonNull UiState<List<CourseSection>> state) {
        if (binding == null) {
            return;
        }
        boolean loading = state instanceof UiState.Loading;
        binding.progressLibrary.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            List<CourseSection> sections = ((UiState.Success<List<CourseSection>>) state).data;
            adapter.submitSections(sections);
            boolean empty = sections.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            binding.listLibrary.setVisibility(empty ? View.GONE : View.VISIBLE);
            if (!empty) {
                UiAnimations.playListLayoutAnimation(binding.listLibrary);
            }
            binding.libraryControls.setVisibility(View.VISIBLE);
            binding.buttonExpandToggle.setVisibility(empty ? View.GONE : View.VISIBLE);
            binding.buttonExpandToggle.setText(viewModel.hasExpandedSection()
                    ? R.string.action_collapse_all
                    : R.string.action_expand_all);
            if (empty) {
                boolean filtering = viewModel.getStatusFilter() != LibraryFilter.ALL;
                binding.emptyState.imageEmpty.setImageResource(R.drawable.ill_empty_library);
                binding.emptyState.textEmptyTitle.setVisibility(View.VISIBLE);
                binding.emptyState.textEmptyTitle.setText(filtering
                        ? R.string.library_filter_empty_title
                        : R.string.library_empty_title);
                binding.emptyState.textEmptyMessage.setText(filtering
                        ? R.string.library_filter_empty
                        : R.string.library_empty);
                binding.emptyState.buttonEmptyCta.setVisibility(filtering ? View.GONE : View.VISIBLE);
                binding.emptyState.buttonEmptyCta.setText(R.string.action_record_lecture);
            }
            // Shrink label when scrolling through a full library.
            if (empty) {
                binding.fabRecord.extend();
            } else {
                binding.fabRecord.shrink();
            }
        } else if (state instanceof UiState.Error) {
            binding.emptyState.getRoot().setVisibility(View.GONE);
            binding.listLibrary.setVisibility(View.VISIBLE);
            String msg = ((UiState.Error<List<CourseSection>>) state).message;
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            SectionFeedback.toast(this, msg);
        }
    }

    // ---- LibraryListAdapter.Listener ----

    @Override
    public void onCourseToggled(long courseId) {
        viewModel.toggleCourse(courseId);
    }

    @Override
    public void onLectureClicked(long lectureId) {
        AppNavigator.openLecture(this, lectureId, -1L);
    }

    @Override
    public void onRenameCourse(long courseId,
                               @NonNull String currentName,
                               @NonNull String professor) {
        showCourseDialog(
                R.string.dialog_rename_category_title,
                currentName,
                professor,
                (name, prof) -> viewModel.renameCourse(courseId, name, prof));
    }

    @Override
    public void onDeleteCourse(long courseId, @NonNull String currentName) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.dialog_delete_category_message, currentName))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) ->
                        viewModel.deleteCourse(courseId))
                .show();
    }

    @Override
    public void onRecordInCourse(long courseId) {
        AppNavigator.openUploadActivity(this);
    }

    @Override
    public void onRenameLecture(long lectureId, @NonNull String currentTitle) {
        showTextDialog(
                R.string.dialog_rename_lecture_title,
                R.string.dialog_lecture_title_hint,
                currentTitle,
                title -> viewModel.renameLecture(lectureId, title));
    }

    @Override
    public void onMoveLecture(long lectureId) {
        List<Course> courses = viewModel.getCoursesSnapshot();
        if (courses.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_move_lecture_title)
                    .setMessage(R.string.move_lecture_no_categories)
                    .setPositiveButton(R.string.action_add_category, (d, w) -> showAddCategoryDialog())
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }
        List<String> labels = new ArrayList<>(courses.size() + 1);
        List<Long> ids = new ArrayList<>(courses.size() + 1);
        labels.add(getString(R.string.library_uncategorized));
        ids.add(LibraryViewModel.UNCATEGORIZED_COURSE_ID);
        for (Course course : courses) {
            labels.add(course.getName());
            ids.add(course.getId());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_move_lecture_title)
                .setItems(labels.toArray(new String[0]), (d, which) -> {
                    long targetId = ids.get(which);
                    String label = labels.get(which);
                    viewModel.moveLecture(lectureId, targetId);
                    if (binding != null) {
                        Snackbar.make(binding.getRoot(),
                                getString(R.string.move_lecture_done, label),
                                Snackbar.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showAddCategoryDialog() {
        showCourseDialog(
                R.string.dialog_add_category_title,
                "",
                "",
                (name, professor) -> viewModel.addCourse(name, professor));
    }

    private void showCourseDialog(int titleRes,
                                  @NonNull String initialName,
                                  @NonNull String initialProfessor,
                                  @NonNull CourseDialogCallback callback) {
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout column = new LinearLayout(requireContext());
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(pad, pad / 2, pad, 0);

        final EditText nameInput = new EditText(requireContext());
        nameInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nameInput.setHint(R.string.dialog_category_name_hint);
        nameInput.setText(initialName);
        nameInput.setSelectAllOnFocus(true);
        column.addView(nameInput, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        final EditText professorInput = new EditText(requireContext());
        professorInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        professorInput.setHint(R.string.dialog_professor_hint);
        professorInput.setText(initialProfessor);
        LinearLayout.LayoutParams profParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        profParams.topMargin = pad / 2;
        column.addView(professorInput, profParams);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(column)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) ->
                        callback.onResult(
                                nameInput.getText() != null
                                        ? nameInput.getText().toString()
                                        : "",
                                professorInput.getText() != null
                                        ? professorInput.getText().toString()
                                        : ""))
                .show();
        nameInput.requestFocus();
    }

    private void showTextDialog(int titleRes,
                                int hintRes,
                                @NonNull String initial,
                                @NonNull TextDialogCallback callback) {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(hintRes);
        input.setText(initial);
        input.setSelectAllOnFocus(true);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = pad;
        params.rightMargin = pad;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) ->
                        callback.onResult(input.getText() != null
                                ? input.getText().toString()
                                : ""))
                .show();
        input.requestFocus();
    }

    private interface TextDialogCallback {
        void onResult(@NonNull String value);
    }

    private interface CourseDialogCallback {
        void onResult(@NonNull String name, @NonNull String professor);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
