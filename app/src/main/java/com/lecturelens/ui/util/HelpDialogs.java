package com.lecturelens.ui.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lecturelens.R;

/** Course rubric — Help menu: authors, version, how-to. */
public final class HelpDialogs {

    private HelpDialogs() {
    }

    public static void show(@NonNull Fragment fragment, @NonNull String sectionName) {
        Context context = fragment.requireContext();
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_help, null, false);
        TextView title = content.findViewById(R.id.text_help_title);
        TextView body = content.findViewById(R.id.text_help_body);
        title.setText(context.getString(R.string.help_title, sectionName));
        body.setText(context.getString(
                R.string.help_body,
                authors(context),
                versionLabel(context),
                sectionName));
        new MaterialAlertDialogBuilder(context)
                .setView(content)
                .setPositiveButton(R.string.help_close, null)
                .show();
    }

    public static void showFromContext(@NonNull Context context, @NonNull String sectionName) {
        View content = LayoutInflater.from(context).inflate(R.layout.dialog_help, null, false);
        TextView title = content.findViewById(R.id.text_help_title);
        TextView body = content.findViewById(R.id.text_help_body);
        title.setText(context.getString(R.string.help_title, sectionName));
        body.setText(context.getString(
                R.string.help_body,
                authors(context),
                versionLabel(context),
                sectionName));
        new MaterialAlertDialogBuilder(context)
                .setView(content)
                .setPositiveButton(R.string.help_close, null)
                .show();
    }

    @NonNull
    private static String authors(@NonNull Context context) {
        return context.getString(R.string.help_authors);
    }

    @NonNull
    private static String versionLabel(@NonNull Context context) {
        try {
            PackageInfo info = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return info.versionName != null ? info.versionName : "0.1.0";
        } catch (PackageManager.NameNotFoundException e) {
            return "0.1.0";
        }
    }
}
