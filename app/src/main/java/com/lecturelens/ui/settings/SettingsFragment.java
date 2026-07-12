package com.lecturelens.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.ThemeShowcaseActivity;
import com.lecturelens.databinding.FragmentSettingsBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Track 1 — settings: API-key edit, cloud-consent revocation, and the Theme
 * Showcase launcher (design-review tool).
 */
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

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state == null || stateApplied || binding == null) {
                return;
            }
            stateApplied = true;
            binding.textEmail.setText(getString(R.string.settings_signed_in_as, state.email));
            binding.editApiKey.setText(state.apiKey);
            binding.switchConsent.setChecked(state.consent);
            // Attach the listener only after the initial value is applied so
            // restoring state doesn't immediately re-write the store.
            binding.switchConsent.setOnCheckedChangeListener(
                    (button, checked) -> viewModel.setCloudConsent(checked));
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
