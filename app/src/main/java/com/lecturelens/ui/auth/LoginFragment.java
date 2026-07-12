package com.lecturelens.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.lecturelens.R;
import com.lecturelens.databinding.FragmentLoginBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Track 1 — sign-in: email, Google API key (stored encrypted), and the
 * cloud-processing consent checkbox (arch doc §1.1). Navigates to Library on
 * success; login is popped from the back stack by the nav action.
 */
@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Nullable private FragmentLoginBinding binding;
    private boolean prefilled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LoginViewModel viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.getPrefill().observe(getViewLifecycleOwner(), prefill -> {
            if (prefill == null || prefilled || binding == null) {
                return;
            }
            prefilled = true;
            binding.editEmail.setText(prefill.email);
            binding.editApiKey.setText(prefill.apiKey);
            binding.checkboxConsent.setChecked(prefill.consent);
        });

        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilEmail.setError(error);
        });
        viewModel.getApiKeyError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilApiKey.setError(error);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding != null) binding.buttonContinue.setEnabled(!Boolean.TRUE.equals(loading));
        });
        viewModel.getSignedIn().observe(getViewLifecycleOwner(), signedIn -> {
            if (Boolean.TRUE.equals(signedIn)) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_library);
            }
        });

        binding.buttonContinue.setOnClickListener(v -> viewModel.signIn(
                textOf(binding.editEmail.getText()),
                textOf(binding.editApiKey.getText()),
                binding.checkboxConsent.isChecked()));
    }

    @Nullable
    private static String textOf(@Nullable CharSequence text) {
        return text != null ? text.toString() : null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
