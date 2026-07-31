package com.lecturelens.ui.section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.lecturelens.ui.search.SearchFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SearchActivity extends SectionHostActivity {
    @NonNull
    @Override
    protected Fragment createFragment() {
        return new SearchFragment();
    }
}
