package com.lecturelens.ui.section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.lecturelens.ui.library.LibraryFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LibraryActivity extends SectionHostActivity {
    @NonNull
    @Override
    protected Fragment createFragment() {
        return new LibraryFragment();
    }
}
