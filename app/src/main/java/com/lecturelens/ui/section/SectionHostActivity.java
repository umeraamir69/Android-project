package com.lecturelens.ui.section;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.lecturelens.R;
import com.lecturelens.ui.util.UiAnimations;

import dagger.hilt.android.AndroidEntryPoint;

/** Hosts a section Fragment in its own Activity (course rubric). */
@AndroidEntryPoint
public abstract class SectionHostActivity extends AppCompatActivity {

    @NonNull
    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_section_host);

        View host = findViewById(R.id.section_host);
        ViewCompat.setOnApplyWindowInsetsListener(host, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(
                            R.anim.slide_up_fade_in,
                            R.anim.fade_out,
                            R.anim.fade_in,
                            R.anim.fade_out)
                    .replace(R.id.section_host, createFragment())
                    .commit();
        }
    }

    @Override
    public void finish() {
        super.finish();
        UiAnimations.applyActivityExit(this);
    }
}
