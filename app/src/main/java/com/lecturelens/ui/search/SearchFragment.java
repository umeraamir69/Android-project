package com.lecturelens.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lecturelens.databinding.FragmentSearchBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Day 0 placeholder — Track 5 (Aaron) replaces with FTS4 search UI.
 * Results navigate via action_search_to_lecture with lectureId + seekMs.
 */
@AndroidEntryPoint
public class SearchFragment extends Fragment {

    @Nullable private FragmentSearchBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
