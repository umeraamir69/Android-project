package com.lecturelens.ui.lecture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lecturelens.R;
import com.lecturelens.databinding.FragmentLectureViewBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Day 0 placeholder — Track 5 (Aaron) replaces with player + transcript +
 * notes tabs. Nav args: lectureId (long, required), seekMs (long, default -1;
 * used by search-result jumps).
 */
@AndroidEntryPoint
public class LectureViewFragment extends Fragment {

    @Nullable private FragmentLectureViewBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLectureViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        long lectureId = requireArguments().getLong("lectureId");
        binding.textPlaceholder.setText(
                getString(R.string.placeholder_lecture, lectureId));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
