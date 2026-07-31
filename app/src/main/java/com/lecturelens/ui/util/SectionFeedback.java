package com.lecturelens.ui.util;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;

/** Ensures each section can show Toast + Snackbar + custom dialog (rubric #11). */
public final class SectionFeedback {

    private SectionFeedback() {
    }

    public static void toast(@NonNull Fragment fragment, @StringRes int message) {
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void toast(@NonNull Fragment fragment, @NonNull String message) {
        Toast.makeText(fragment.requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    public static void snackbar(@NonNull Fragment fragment, @StringRes int message) {
        if (fragment.getView() != null) {
            Snackbar.make(fragment.getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void snackbar(@NonNull Fragment fragment, @NonNull String message) {
        if (fragment.getView() != null) {
            Snackbar.make(fragment.getView(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    public static void infoDialog(@NonNull Fragment fragment,
                                  @StringRes int title,
                                  @NonNull String message) {
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    public static void infoDialog(@NonNull Context context,
                                  @StringRes int title,
                                  @NonNull String message) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }
}
