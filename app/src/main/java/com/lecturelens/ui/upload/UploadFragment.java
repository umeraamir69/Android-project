package com.lecturelens.ui.upload;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lecturelens.databinding.FragmentUploadBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Day 0 placeholder — Track 3 (Adeniyi) replaces with record/import UI,
 * recording state machine, and pipeline enqueue.
 * Nav arg: courseId (long, default -1).
 */
@AndroidEntryPoint
public class UploadFragment extends Fragment {

    @Nullable private FragmentUploadBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
