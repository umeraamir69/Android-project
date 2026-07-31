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

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    @Nullable private FragmentLoginBinding binding;

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
        com.lecturelens.ui.util.UiAnimations.animateScreenEnter(view);
        LoginViewModel viewModel = new ViewModelProvider(this).get(LoginViewModel.class);

        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilEmail.setError(error);
        });
        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilPassword.setError(error);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.buttonSignIn.setEnabled(!busy);
            binding.buttonCreateAccount.setEnabled(!busy);
        });
        viewModel.getStatusMessage().observe(getViewLifecycleOwner(), msg -> {
            if (binding == null) return;
            if (msg == null || msg.isEmpty()) {
                binding.textStatus.setVisibility(View.GONE);
            } else {
                binding.textStatus.setVisibility(View.VISIBLE);
                binding.textStatus.setText(msg);
            }
        });
        viewModel.getSignedIn().observe(getViewLifecycleOwner(), signedIn -> {
            if (Boolean.TRUE.equals(signedIn)) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_home);
            }
        });

        binding.buttonSignIn.setOnClickListener(v -> viewModel.signInWithPassword(
                textOf(binding.editEmail.getText()),
                textOf(binding.editPassword.getText()),
                binding.checkboxConsent.isChecked()));
        binding.buttonCreateAccount.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_login_to_create_account));
    }

    @Nullable
    private static String textOf(@Nullable CharSequence cs) {
        return cs == null ? null : cs.toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
