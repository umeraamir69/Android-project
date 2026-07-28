package com.lecturelens.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.ThemeShowcaseActivity;
import com.lecturelens.databinding.FragmentSettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    @Nullable private FragmentSettingsBinding binding;
    private boolean stateApplied;

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
        SettingsViewModel viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        String[] themeLabels = getResources().getStringArray(R.array.theme_mode_labels);
        String[] themeValues = getResources().getStringArray(R.array.theme_mode_values);
        String[] langLabels = getResources().getStringArray(R.array.stt_language_labels);
        String[] langValues = getResources().getStringArray(R.array.stt_language_values);

        binding.inputTheme.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, themeLabels));
        binding.inputLanguage.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, langLabels));

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || stateApplied || binding == null) {
                return;
            }
            stateApplied = true;
            binding.textEmail.setText(getString(R.string.settings_signed_in_as, state.email));
            binding.editApiKey.setText(state.apiKey);
            binding.switchConsent.setChecked(state.consent);
            binding.switchConsent.setOnCheckedChangeListener(
                    (button, checked) -> viewModel.setCloudConsent(checked));

            int themeIndex = indexOf(themeValues, state.themeMode, 0);
            binding.inputTheme.setText(themeLabels[themeIndex], false);
            binding.inputTheme.setOnItemClickListener((parent, v, position, id) -> {
                if (position >= 0 && position < themeValues.length) {
                    viewModel.setThemeMode(themeValues[position]);
                }
            });

            int langIndex = indexOf(langValues, state.language, 0);
            binding.inputLanguage.setText(langLabels[langIndex], false);
            binding.inputLanguage.setOnItemClickListener((parent, v, position, id) -> {
                if (position >= 0 && position < langValues.length) {
                    viewModel.setSttLanguage(langValues[position]);
                }
            });
        });

        viewModel.getApiKeyError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilApiKey.setError(error);
        });

        viewModel.getKeySaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved) && binding != null) {
                Snackbar.make(binding.getRoot(), R.string.settings_key_saved,
                        Snackbar.LENGTH_SHORT).show();
                viewModel.ackKeySaved();
            }
        });

        binding.buttonSaveKey.setOnClickListener(v -> viewModel.saveApiKey(
                binding.editApiKey.getText() != null
                        ? binding.editApiKey.getText().toString()
                        : null));

        binding.buttonThemeShowcase.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeShowcaseActivity.class)));
    }

    private static int indexOf(@NonNull String[] values, @Nullable String target, int fallback) {
        if (target == null) {
            return fallback;
        }
        for (int i = 0; i < values.length; i++) {
            if (target.equals(values[i])) {
                return i;
            }
        }
        return fallback;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
