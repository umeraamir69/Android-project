package com.lecturelens.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentLibraryBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Track 2 (Daniel). Library screen: expandable course sections with lecture
 * cards and status badges. Tap a lecture → lecture screen; FAB → record.
 */
@AndroidEntryPoint
public class LibraryFragment extends Fragment implements CoursesAdapter.Listener {

    @Nullable private FragmentLibraryBinding binding;
    private LibraryViewModel viewModel;
    private CoursesAdapter adapter;

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

        adapter = new CoursesAdapter(this);
        binding.recyclerLibrary.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerLibrary.setAdapter(adapter);

        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_search) {
                nav().navigate(R.id.action_library_to_search);
                return true;
            }
            if (id == R.id.menu_settings) {
                nav().navigate(R.id.action_library_to_settings);
                return true;
            }
            return false;
        });
        binding.fabRecord.setOnClickListener(v ->
                nav().navigate(R.id.action_library_to_upload));

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
            adapter.submitList(sections);
            binding.textEmpty.setVisibility(sections.isEmpty() ? View.VISIBLE : View.GONE);
        } else if (state instanceof UiState.Error) {
            binding.textEmpty.setVisibility(View.GONE);
            Snackbar.make(binding.getRoot(),
                    ((UiState.Error<List<CourseSection>>) state).message,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // ---- CoursesAdapter.Listener ----

    @Override
    public void onCourseToggled(long courseId) {
        viewModel.toggleCourse(courseId);
    }

    @Override
    public void onLectureClicked(long lectureId) {
        Bundle args = new Bundle();
        args.putLong("lectureId", lectureId);
        nav().navigate(R.id.action_library_to_lecture, args);
    }

    @NonNull
    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
