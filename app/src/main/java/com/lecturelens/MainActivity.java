package com.lecturelens;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.lecturelens.databinding.ActivityMainBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Single-activity host. All screens are Fragments inside the
 * NavHostFragment declared in {@code activity_main.xml}; navigation is
 * defined in {@code res/navigation/nav_graph.xml}.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Top/side insets on the root; bottom inset is handled by BottomNavigationView
        // so the bar height stays correct above the gesture/home indicator.
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
    }
}
