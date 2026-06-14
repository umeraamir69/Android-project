package com.example.andoirdproject;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Theme Showcase — renders every Material 3 color role and a sampler of
 * Material Components widgets so the LectureLens theme can be eyeballed
 * in light and dark mode without running any real screens.
 *
 * <p>The XML layout supplies the static widgets; this Activity only paints
 * each color swatch by resolving the relevant {@code ?attr/colorX} from the
 * current theme, then pairs each with its "on-" color so labels stay legible.
 */
public class ThemeShowcaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_theme_showcase);

        // Edge-to-edge: pad the root for system bars so content isn't clipped.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.showcase_root),
                (v, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, 0, bars.right, 0);
                    return insets;
                });

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        toolbar.setNavigationOnClickListener(v -> finish());

        paintSwatches();
        wireFeedbackButtons();
        showInputError();
    }

    /** Pre-set an error on one input so the error styling is visible at a glance. */
    private void showInputError() {
        TextInputLayout errorField = findViewById(R.id.input_error);
        if (errorField != null) {
            errorField.setError(getString(R.string.showcase_input_error_text));
        }
    }

    private void wireFeedbackButtons() {
        View root = findViewById(R.id.showcase_root);

        findViewById(R.id.btn_show_snackbar).setOnClickListener(v ->
                Snackbar.make(root, R.string.showcase_snackbar_message, Snackbar.LENGTH_SHORT).show());

        findViewById(R.id.btn_show_snackbar_action).setOnClickListener(v ->
                Snackbar.make(root, R.string.showcase_snackbar_action_message, Snackbar.LENGTH_LONG)
                        .setAction(R.string.showcase_snackbar_action, b ->
                                Snackbar.make(root, R.string.showcase_snackbar_retrying,
                                        Snackbar.LENGTH_SHORT).show())
                        .show());

        findViewById(R.id.btn_show_dialog).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.showcase_dialog_title)
                        .setMessage(R.string.showcase_dialog_message)
                        .setPositiveButton(R.string.showcase_dialog_positive, null)
                        .setNegativeButton(R.string.showcase_dialog_negative, null)
                        .show());

        findViewById(R.id.btn_show_dialog_destructive).setOnClickListener(v ->
                new MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.showcase_dialog_destructive_title)
                        .setMessage(R.string.showcase_dialog_destructive_message)
                        .setPositiveButton(R.string.showcase_dialog_destructive_positive, null)
                        .setNegativeButton(R.string.showcase_dialog_destructive_negative, null)
                        .show());
    }

    private void paintSwatches() {
        paintSwatch(R.id.swatch_primary,
                "colorPrimary", "colorOnPrimary",
                R.string.swatch_primary);
        paintSwatch(R.id.swatch_primary_container,
                "colorPrimaryContainer", "colorOnPrimaryContainer",
                R.string.swatch_primary_container);
        paintSwatch(R.id.swatch_secondary,
                "colorSecondary", "colorOnSecondary",
                R.string.swatch_secondary);
        paintSwatch(R.id.swatch_secondary_container,
                "colorSecondaryContainer", "colorOnSecondaryContainer",
                R.string.swatch_secondary_container);
        paintSwatch(R.id.swatch_tertiary,
                "colorTertiary", "colorOnTertiary",
                R.string.swatch_tertiary);
        paintSwatch(R.id.swatch_tertiary_container,
                "colorTertiaryContainer", "colorOnTertiaryContainer",
                R.string.swatch_tertiary_container);
        paintSwatch(R.id.swatch_surface,
                "colorSurface", "colorOnSurface",
                R.string.swatch_surface);
        paintSwatch(R.id.swatch_surface_variant,
                "colorSurfaceVariant", "colorOnSurfaceVariant",
                R.string.swatch_surface_variant);
        paintSwatch(R.id.swatch_error,
                "colorError", "colorOnError",
                R.string.swatch_error);
    }

    /**
     * Tint one {@code item_color_swatch} include with its role color and
     * write the label in the matching "on-" color. Attrs are resolved by name
     * from the merged resource table so we don't need to know which library
     * (appcompat vs material) declares each role.
     */
    private void paintSwatch(@IdRes int containerId,
                             String bgAttrName,
                             String onAttrName,
                             @StringRes int labelRes) {
        // The <include> tag's android:id overrides the root id of the included
        // layout, so the container IS the MaterialCardView (swatch_card).
        MaterialCardView card = findViewById(containerId);
        if (card == null) return;

        TextView label = card.findViewById(R.id.swatch_label);
        if (label == null) return;

        int bgAttr = attrIdByName(bgAttrName);
        int onAttr = attrIdByName(onAttrName);
        if (bgAttr == 0 || onAttr == 0) return;

        int bg = MaterialColors.getColor(card, bgAttr);
        int on = MaterialColors.getColor(card, onAttr);

        card.setCardBackgroundColor(bg);
        label.setText(getString(labelRes) + "\n" + hex(bg));
        label.setTextColor(on);
    }

    private int attrIdByName(String name) {
        return getResources().getIdentifier(name, "attr", getPackageName());
    }

    private static String hex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
