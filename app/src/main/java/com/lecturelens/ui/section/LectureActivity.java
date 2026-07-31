package com.lecturelens.ui.section;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lecturelens.ui.lecture.LectureViewFragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LectureActivity extends SectionHostActivity {

    public static final String EXTRA_LECTURE_ID = "lectureId";
    public static final String EXTRA_SEEK_MS = "seekMs";

    public static void start(@NonNull Context context, long lectureId, long seekMs) {
        Intent intent = new Intent(context, LectureActivity.class);
        intent.putExtra(EXTRA_LECTURE_ID, lectureId);
        intent.putExtra(EXTRA_SEEK_MS, seekMs);
        context.startActivity(intent);
        if (context instanceof android.app.Activity) {
            com.lecturelens.ui.util.UiAnimations.applyActivityEnter((android.app.Activity) context);
        }
    }

    @NonNull
    @Override
    protected Fragment createFragment() {
        long lectureId = getIntent().getLongExtra(EXTRA_LECTURE_ID, -1L);
        long seekMs = getIntent().getLongExtra(EXTRA_SEEK_MS, -1L);
        Bundle args = new Bundle();
        args.putLong("lectureId", lectureId);
        args.putLong("seekMs", seekMs);
        LectureViewFragment fragment = new LectureViewFragment();
        fragment.setArguments(args);
        return fragment;
    }
}
