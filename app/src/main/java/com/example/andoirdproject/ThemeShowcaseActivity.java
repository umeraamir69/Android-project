package com.example.andoirdproject;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.AttrRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

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
    }

    private void paintSwatches() {
        paintSwatch(R.id.swatch_primary,
                com.google.android.material.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary,
                R.string.swatch_primary);
        paintSwatch(R.id.swatch_primary_container,
                com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                R.string.swatch_primary_container);
        paintSwatch(R.id.swatch_secondary,
                com.google.android.material.R.attr.colorSecondary,
                com.google.android.material.R.attr.colorOnSecondary,
                R.string.swatch_secondary);
        paintSwatch(R.id.swatch_secondary_container,
                com.google.android.material.R.attr.colorSecondaryContainer,
                com.google.android.material.R.attr.colorOnSecondaryContainer,
                R.string.swatch_secondary_container);
        paintSwatch(R.id.swatch_tertiary,
                com.google.android.material.R.attr.colorTertiary,
                com.google.android.material.R.attr.colorOnTertiary,
                R.string.swatch_tertiary);
        paintSwatch(R.id.swatch_tertiary_container,
                com.google.android.material.R.attr.colorTertiaryContainer,
                com.google.android.material.R.attr.colorOnTertiaryContainer,
                R.string.swatch_tertiary_container);
        paintSwatch(R.id.swatch_surface,
                com.google.android.material.R.attr.colorSurface,
                com.google.android.material.R.attr.colorOnSurface,
                R.string.swatch_surface);
        paintSwatch(R.id.swatch_surface_variant,
                com.google.android.material.R.attr.colorSurfaceVariant,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                R.string.swatch_surface_variant);
        paintSwatch(R.id.swatch_error,
                com.google.android.material.R.attr.colorError,
                com.google.android.material.R.attr.colorOnError,
                R.string.swatch_error);
    }

    /**
     * Tint one {@code item_color_swatch} include with its role color and
     * write the label in the matching "on-" color.
     */
    private void paintSwatch(@IdRes int containerId,
                             @AttrRes int bgAttr,
                             @AttrRes int onAttr,
                             @StringRes int labelRes) {
        View container = findViewById(containerId);
        if (container == null) return;

        MaterialCardView card = container.findViewById(R.id.swatch_card);
        TextView label = container.findViewById(R.id.swatch_label);

        int bg = MaterialColors.getColor(card, bgAttr);
        int on = MaterialColors.getColor(card, onAttr);

        card.setCardBackgroundColor(bg);
        label.setText(getString(labelRes) + "\n" + hex(bg));
        label.setTextColor(on);
    }

    private static String hex(int color) {
        return String.format("#%06X", 0xFFFFFF & color);
    }
}
