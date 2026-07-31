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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.ThemeShowcaseActivity;
import com.lecturelens.core.AppLocale;
import com.lecturelens.databinding.FragmentSettingsBinding;
import com.lecturelens.ui.util.HelpDialogs;
import com.lecturelens.ui.util.SectionFeedback;

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

        binding.toolbar.setNavigationOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigateUp();
            } catch (IllegalStateException e) {
                requireActivity().finish();
            }
        });
        binding.toolbar.inflateMenu(R.menu.menu_help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_help) {
                HelpDialogs.show(this, getString(R.string.title_settings));
                return true;
            }
            return false;
        });

        String[] themeLabels = getResources().getStringArray(R.array.theme_mode_labels);
        String[] themeValues = getResources().getStringArray(R.array.theme_mode_values);
        String[] langLabels = getResources().getStringArray(R.array.stt_language_labels);
        String[] langValues = getResources().getStringArray(R.array.stt_language_values);
        String[] uiLangLabels = getResources().getStringArray(R.array.ui_language_labels);
        String[] uiLangValues = getResources().getStringArray(R.array.ui_language_values);
        String[] modeLabels = getResources().getStringArray(R.array.processing_mode_labels);
        String[] modeValues = getResources().getStringArray(R.array.processing_mode_values);

        binding.inputTheme.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, themeLabels));
        binding.inputLanguage.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, langLabels));
        binding.inputUiLanguage.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, uiLangLabels));
        binding.inputProcessing.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, modeLabels));
        SectionFeedback.toast(this, getString(R.string.toast_section_ready,
                getString(R.string.title_settings)));

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || stateApplied || binding == null) {
                return;
            }
            stateApplied = true;
            String label = state.email.isEmpty() ? "—" : state.email;
            binding.textEmail.setText(getString(R.string.settings_signed_in_as, label));
            binding.editUsername.setText(state.profile.username);
            binding.editFullName.setText(state.profile.fullName);
            binding.editDob.setText(state.profile.dateOfBirth);
            binding.editUniversity.setText(state.profile.university);
            binding.editProgram.setText(state.profile.program);
            binding.editStudentId.setText(state.profile.studentId);
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

            int uiIndex = indexOf(uiLangValues, state.appLocale, 0);
            binding.inputUiLanguage.setText(uiLangLabels[uiIndex], false);
            binding.inputUiLanguage.setOnItemClickListener((parent, v, position, id) -> {
                if (position >= 0 && position < uiLangValues.length) {
                    viewModel.setAppLocale(uiLangValues[position]);
                    SectionFeedback.snackbar(this, R.string.settings_profile_saved);
                    AppLocale.recreate(requireActivity());
                }
            });

            int langIndex = indexOf(langValues, state.language, 0);
            binding.inputLanguage.setText(langLabels[langIndex], false);
            binding.inputLanguage.setOnItemClickListener((parent, v, position, id) -> {
                if (position >= 0 && position < langValues.length) {
                    viewModel.setSttLanguage(langValues[position]);
                }
            });

            int modeIndex = indexOf(modeValues, state.processingMode, 0);
            binding.inputProcessing.setText(modeLabels[modeIndex], false);
            binding.inputProcessing.setOnItemClickListener((parent, v, position, id) -> {
                if (position >= 0 && position < modeValues.length) {
                    viewModel.setProcessingMode(modeValues[position]);
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

        viewModel.getProfileSaved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved) && binding != null) {
                Snackbar.make(binding.getRoot(), R.string.settings_profile_saved,
                        Snackbar.LENGTH_SHORT).show();
                SectionFeedback.toast(this, R.string.settings_profile_saved);
                SectionFeedback.infoDialog(this, R.string.title_settings,
                        getString(R.string.settings_profile_saved));
                viewModel.ackProfileSaved();
            }
        });

        viewModel.getSignedOut().observe(getViewLifecycleOwner(), out -> {
            if (Boolean.TRUE.equals(out)) {
                NavOptions options = new NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build();
                NavHostFragment.findNavController(this)
                        .navigate(R.id.login, null, options);
            }
        });

        binding.buttonSaveProfile.setOnClickListener(v -> viewModel.saveProfile(
                textOf(binding.editUsername),
                textOf(binding.editFullName),
                textOf(binding.editDob),
                textOf(binding.editUniversity),
                textOf(binding.editProgram),
                textOf(binding.editStudentId)));

        binding.buttonSaveKey.setOnClickListener(v -> viewModel.saveApiKey(
                textOf(binding.editApiKey)));

        binding.buttonSignOut.setOnClickListener(v -> viewModel.signOut());

        binding.buttonThemeShowcase.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), ThemeShowcaseActivity.class)));
    }

    @Nullable
    private static String textOf(@Nullable android.widget.EditText edit) {
        return edit != null && edit.getText() != null ? edit.getText().toString() : null;
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
