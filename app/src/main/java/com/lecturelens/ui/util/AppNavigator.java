package com.lecturelens.ui.util;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.lecturelens.MainActivity;
import com.lecturelens.R;
import com.lecturelens.ui.section.LectureActivity;
import com.lecturelens.ui.section.LibraryActivity;
import com.lecturelens.ui.section.SearchActivity;
import com.lecturelens.ui.section.SettingsActivity;
import com.lecturelens.ui.section.UploadActivity;

/** Navigates via NavController when inside MainActivity, else section Activities. */
public final class AppNavigator {

    private AppNavigator() {
    }

    /** After sign-out: go to login whether hosted in MainActivity or a section Activity. */
    public static void openLoginAfterSignOut(@NonNull Fragment fragment) {
        try {
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build();
            NavHostFragment.findNavController(fragment)
                    .navigate(R.id.login, null, options);
        } catch (IllegalStateException e) {
            Intent intent = new Intent(fragment.requireContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            fragment.startActivity(intent);
            fragment.requireActivity().finish();
        }
    }

    public static void openLecture(@NonNull Fragment fragment, long lectureId, long seekMs) {
        try {
            NavController nav = NavHostFragment.findNavController(fragment);
            Bundle args = new Bundle();
            args.putLong("lectureId", lectureId);
            args.putLong("seekMs", seekMs);
            int dest = nav.getCurrentDestination() != null
                    ? nav.getCurrentDestination().getId()
                    : 0;
            if (dest == R.id.home) {
                nav.navigate(R.id.action_home_to_lecture, args);
            } else if (dest == R.id.library) {
                nav.navigate(R.id.action_library_to_lecture, args);
            } else if (dest == R.id.search) {
                nav.navigate(R.id.action_search_to_lecture, args);
            } else {
                LectureActivity.start(fragment.requireContext(), lectureId, seekMs);
            }
        } catch (IllegalStateException e) {
            LectureActivity.start(fragment.requireContext(), lectureId, seekMs);
        }
    }

    public static void openLibraryActivity(@NonNull Fragment fragment) {
        startWithAnim(fragment, LibraryActivity.class);
    }

    public static void openSearchActivity(@NonNull Fragment fragment) {
        startWithAnim(fragment, SearchActivity.class);
    }

    public static void openUploadActivity(@NonNull Fragment fragment) {
        startWithAnim(fragment, UploadActivity.class);
    }

    public static void openSettingsActivity(@NonNull Fragment fragment) {
        startWithAnim(fragment, SettingsActivity.class);
    }

    private static void startWithAnim(@NonNull Fragment fragment, @NonNull Class<?> activity) {
        fragment.startActivity(new android.content.Intent(fragment.requireContext(), activity));
        UiAnimations.applyActivityEnter(fragment.requireActivity());
    }

    public static void goBottomTab(@NonNull Fragment fragment, int destId) {
        try {
            NavController nav = NavHostFragment.findNavController(fragment);
            NavOptions options = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(R.id.home, false, true)
                    .build();
            nav.navigate(destId, null, options);
        } catch (IllegalStateException e) {
            if (destId == R.id.library) {
                openLibraryActivity(fragment);
            } else if (destId == R.id.search) {
                openSearchActivity(fragment);
            }
        }
    }
}
