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
import com.lecturelens.databinding.FragmentCreateAccountBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CreateAccountFragment extends Fragment {

    @Nullable private FragmentCreateAccountBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCreateAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        CreateAccountViewModel viewModel =
                new ViewModelProvider(this).get(CreateAccountViewModel.class);

        viewModel.getEmailError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilEmail.setError(error);
        });
        viewModel.getPasswordError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilPassword.setError(error);
        });
        viewModel.getUsernameError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilUsername.setError(error);
        });
        viewModel.getUniversityError().observe(getViewLifecycleOwner(), error -> {
            if (binding != null) binding.tilUniversity.setError(error);
        });
        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) return;
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progress.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.buttonCreate.setEnabled(!busy);
            binding.buttonBackToSignIn.setEnabled(!busy);
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
                        .navigate(R.id.action_create_account_to_home);
            }
        });

        binding.buttonCreate.setOnClickListener(v -> viewModel.createAccount(
                textOf(binding.editEmail.getText()),
                textOf(binding.editPassword.getText()),
                textOf(binding.editUsername.getText()),
                textOf(binding.editFullName.getText()),
                textOf(binding.editUniversity.getText()),
                textOf(binding.editProgram.getText()),
                textOf(binding.editStudentId.getText()),
                binding.checkboxConsent.isChecked()));
        binding.buttonBackToSignIn.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @NonNull
    private static String textOf(@Nullable CharSequence cs) {
        return cs == null ? "" : cs.toString();
    }
}
