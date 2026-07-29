package com.lecturelens;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.data.repository.FirebaseAuthRepository;
import com.lecturelens.databinding.ActivityMainBinding;
import com.lecturelens.domain.repository.AuthRepository;
import com.lecturelens.domain.repository.CredentialsStore;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single-activity host. All screens are Fragments inside the
 * NavHostFragment declared in {@code activity_main.xml}; navigation is
 * defined in {@code res/navigation/nav_graph.xml}.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject AuthRepository authRepository;
    @Inject CredentialsStore credentialsStore;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (host == null) {
            return;
        }
        NavController navController = host.getNavController();

        if (authRepository.isSignedIn()
                && navController.getCurrentDestination() != null
                && navController.getCurrentDestination().getId() == R.id.login) {
            navController.navigate(R.id.action_login_to_home);
        }

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            NavDestination current = navController.getCurrentDestination();
            if (current != null && id == current.getId()) {
                return true;
            }
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.home, false, true)
                    .build();
            navController.navigate(id, null, options);
            return true;
        });
        binding.bottomNav.setOnItemReselectedListener(item -> {
            // Already on tab — no-op.
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            int destId = destination.getId();
            boolean showBottom = destId == R.id.home
                    || destId == R.id.library
                    || destId == R.id.search;
            int vis = showBottom ? View.VISIBLE : View.GONE;
            binding.bottomNav.setVisibility(vis);
            binding.bottomNavDivider.setVisibility(vis);
            if (showBottom) {
                binding.bottomNav.getMenu().findItem(destId).setChecked(true);
            }
        });

        handleEmailLinkIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleEmailLinkIntent(intent);
    }

    private void handleEmailLinkIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        Uri data = intent.getData();
        String link = FirebaseAuthRepository.extractLinkFromUri(data);
        if (link == null || !authRepository.isSignInWithEmailLink(link)) {
            return;
        }
        String pending = authRepository.getPendingEmail();
        if (pending != null && !pending.isEmpty()) {
            completeEmailLink(pending, link);
            return;
        }
        EditText input = new EditText(this);
        input.setHint(R.string.login_email_hint);
        new AlertDialog.Builder(this)
                .setTitle(R.string.login_magic_link)
                .setMessage("Enter the email you used for the sign-in link.")
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String email = input.getText() != null ? input.getText().toString().trim() : "";
                    if (!email.isEmpty()) {
                        completeEmailLink(email, link);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void completeEmailLink(@NonNull String email, @NonNull String link) {
        authRepository.completePasswordlessSignIn(email, link, new AuthRepository.Callback() {
            @Override
            public void onSuccess() {
                credentialsStore.setCloudConsent(true);
                NavHostFragment host = (NavHostFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.nav_host_fragment);
                if (host != null) {
                    NavController nav = host.getNavController();
                    if (nav.getCurrentDestination() != null
                            && nav.getCurrentDestination().getId() == R.id.login) {
                        nav.navigate(R.id.action_login_to_home);
                    }
                }
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), "Signed in", Snackbar.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }
}
