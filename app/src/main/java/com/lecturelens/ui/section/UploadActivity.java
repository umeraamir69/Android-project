package com.lecturelens.ui.section;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.lecturelens.ui.upload.UploadFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class UploadActivity extends SectionHostActivity {
    @NonNull
    @Override
    protected Fragment createFragment() {
        return new UploadFragment();
    }
}
