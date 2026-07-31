package com.lecturelens.ui.section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.lecturelens.ui.settings.SettingsFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends SectionHostActivity {
    @NonNull
    @Override
    protected Fragment createFragment() {
        return new SettingsFragment();
    }
}
