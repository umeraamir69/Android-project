package com.lecturelens.ui.library;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.lecturelens.R;
import com.lecturelens.databinding.FragmentLibraryBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Day 0 placeholder — Track 2 (Daniel) replaces with the real course/lecture
 * list. The buttons only prove every nav action works before real UIs land.
 */
@AndroidEntryPoint
public class LibraryFragment extends Fragment {

    @Nullable private FragmentLibraryBinding binding;

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
        NavController nav = NavHostFragment.findNavController(this);

        binding.buttonOpenLecture.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putLong("lectureId", -1L); // placeholder id until seed data lands
            nav.navigate(R.id.action_library_to_lecture, args);
        });
        binding.buttonUpload.setOnClickListener(v ->
                nav.navigate(R.id.action_library_to_upload));
        binding.buttonSearch.setOnClickListener(v ->
                nav.navigate(R.id.action_library_to_search));
        binding.buttonSettings.setOnClickListener(v ->
                nav.navigate(R.id.action_library_to_settings));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
