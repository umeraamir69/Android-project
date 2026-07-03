package com.lecturelens.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lecturelens.ThemeShowcaseActivity;
import com.lecturelens.databinding.FragmentSettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Day 0 placeholder — Track 1 (Zeeshan) replaces with API-key edit +
 * consent revocation (week 2–3, alongside Auth).
 *
 * Hosts the launch point for the Theme Showcase design-review tool so it
 * stays reachable now that MainActivity is the nav host.
 */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Nullable private FragmentSettingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonThemeShowcase.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeShowcaseActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
