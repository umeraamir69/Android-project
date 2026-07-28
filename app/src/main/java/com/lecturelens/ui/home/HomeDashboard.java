package com.lecturelens.ui.home;

import androidx.annotation.NonNull;

import com.lecturelens.domain.model.Lecture;

import java.util.Collections;
import java.util.List;

/** Aggregated home-dashboard snapshot. */
public final class HomeDashboard {

    public final int lectureCount;
    public final int categoryCount;
    public final int readyCount;
    public final int processingCount;
    public final int failedCount;
    @NonNull public final List<Lecture> recentLectures;
    @NonNull public final String email;

    public HomeDashboard(int lectureCount,
                         int categoryCount,
                         int readyCount,
                         int processingCount,
                         int failedCount,
                         @NonNull List<Lecture> recentLectures,
                         @NonNull String email) {
        this.lectureCount = lectureCount;
        this.categoryCount = categoryCount;
        this.readyCount = readyCount;
        this.processingCount = processingCount;
        this.failedCount = failedCount;
        this.recentLectures = recentLectures;
        this.email = email;
    }

    @NonNull
    public static HomeDashboard empty(@NonNull String email) {
        return new HomeDashboard(0, 0, 0, 0, 0, Collections.emptyList(), email);
    }
}
