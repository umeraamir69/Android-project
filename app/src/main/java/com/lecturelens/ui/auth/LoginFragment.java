package com.lecturelens.ui.auth;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.lecturelens.BuildConfig;
import com.lecturelens.R;
import com.lecturelens.databinding.FragmentLoginBinding;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    @Nullable private FragmentLoginBinding binding;
    private final ExecutorService googleExecutor = Executors.newSingleThreadExecutor();
    @Nullable private CancellationSignal googleCancellation;

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
            binding.buttonGoogle.setEnabled(!busy);
            binding.buttonSignIn.setEnabled(!busy);
            binding.buttonCreateAccount.setEnabled(!busy);
            binding.buttonMagicLink.setEnabled(!busy);
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

        binding.buttonGoogle.setOnClickListener(v -> launchGoogleSignIn(viewModel));
        binding.buttonSignIn.setOnClickListener(v -> viewModel.signInWithPassword(
                textOf(binding.editEmail.getText()),
                textOf(binding.editPassword.getText()),
                binding.checkboxConsent.isChecked()));
        binding.buttonCreateAccount.setOnClickListener(v -> viewModel.createAccount(
                textOf(binding.editEmail.getText()),
                textOf(binding.editPassword.getText()),
                binding.checkboxConsent.isChecked()));
        binding.buttonMagicLink.setOnClickListener(v -> viewModel.sendMagicLink(
                textOf(binding.editEmail.getText()),
                binding.checkboxConsent.isChecked()));
    }

    private void launchGoogleSignIn(@NonNull LoginViewModel viewModel) {
        String webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID;
        if (webClientId == null || webClientId.isEmpty()) {
            int resId = getResources().getIdentifier(
                    "default_web_client_id", "string", requireContext().getPackageName());
            if (resId != 0) {
                webClientId = getString(resId);
            }
        }
        if (webClientId == null || webClientId.isEmpty()) {
            if (binding != null) {
                binding.textStatus.setVisibility(View.VISIBLE);
                binding.textStatus.setText(R.string.login_google_missing_client);
            }
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build();
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        if (binding != null) {
            binding.progress.setVisibility(View.VISIBLE);
            binding.buttonGoogle.setEnabled(false);
        }

        if (googleCancellation != null) {
            googleCancellation.cancel();
        }
        googleCancellation = new CancellationSignal();

        CredentialManager credentialManager = CredentialManager.create(requireContext());
        credentialManager.getCredentialAsync(
                requireActivity(),
                request,
                googleCancellation,
                googleExecutor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredential(result.getCredential(), viewModel);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        Log.w(TAG, "Google sign-in failed", e);
                        requireActivity().runOnUiThread(() -> {
                            if (binding != null) {
                                binding.progress.setVisibility(View.GONE);
                                binding.buttonGoogle.setEnabled(true);
                                binding.textStatus.setVisibility(View.VISIBLE);
                                binding.textStatus.setText(e.getMessage() != null
                                        ? e.getMessage()
                                        : getString(R.string.login_google_failed));
                            }
                        });
                    }
                });
    }

    private void handleGoogleCredential(@NonNull Credential credential,
                                        @NonNull LoginViewModel viewModel) {
        if (credential instanceof CustomCredential
                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(credential.getType())) {
            GoogleIdTokenCredential googleIdToken =
                    GoogleIdTokenCredential.createFrom(
                            ((CustomCredential) credential).getData());
            String idToken = googleIdToken.getIdToken();
            requireActivity().runOnUiThread(() -> viewModel.onGoogleIdToken(
                    idToken,
                    binding != null && binding.checkboxConsent.isChecked()));
        } else {
            requireActivity().runOnUiThread(() -> {
                if (binding != null) {
                    binding.progress.setVisibility(View.GONE);
                    binding.buttonGoogle.setEnabled(true);
                    binding.textStatus.setVisibility(View.VISIBLE);
                    binding.textStatus.setText(R.string.login_google_failed);
                }
            });
        }
    }

    @Nullable
    private static String textOf(@Nullable CharSequence text) {
        return text != null ? text.toString() : null;
    }

    @Override
    public void onDestroyView() {
        if (googleCancellation != null) {
            googleCancellation.cancel();
            googleCancellation = null;
        }
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        googleExecutor.shutdownNow();
        super.onDestroy();
    }
}
