package com.lecturelens.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.repository.DatabaseSeeder;
import com.lecturelens.domain.model.LectureStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Track 1 — DB smoke test (WORK_BREAKDOWN "done when"): schema builds,
 * seeding is idempotent, writes round-trip, and FTS4 search returns hits
 * from seeded transcript segments.
 */
@RunWith(AndroidJUnit4.class)
public class DatabaseSmokeTest {

    private LectureLensDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, LectureLensDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void seed_isIdempotent_andSearchable() {
        DatabaseSeeder seeder =
                new DatabaseSeeder(db.courseDao(), db.lectureDao(), db.transcriptDao());

        assertTrue(seeder.seedIfEmpty());
        assertFalse("second run must be a no-op", seeder.seedIfEmpty());

        assertEquals(1, db.courseDao().count());
        assertEquals(2, db.lectureDao().count());

        // FTS4 over seeded segments — external-content triggers must be live.
        List<SearchHit> hits = db.searchDao().searchSync("lifecycle*");
        assertFalse("expected FTS hits for 'lifecycle'", hits.isEmpty());
        assertTrue(hits.get(0).snippet.toLowerCase().contains("lifecycle"));
        assertTrue(hits.get(0).startMs >= 0);
        assertNotNull(hits.get(0).lectureTitle);
    }

    @Test
    public void lectureWrites_roundTrip() {
        CourseEntity course = new CourseEntity();
        course.name = "Test course";
        course.color = 0xFF000000;
        course.createdAt = 1L;
        long courseId = db.courseDao().insert(course);

        LectureEntity lecture = new LectureEntity();
        lecture.courseId = courseId;
        lecture.title = "Test lecture";
        lecture.date = 2L;
        lecture.durationMs = 3L;
        lecture.status = LectureStatus.RECORDED.name();
        long lectureId = db.lectureDao().insert(lecture);
        assertTrue(lectureId > 0);

        db.lectureDao().updateStatus(lectureId, LectureStatus.TRANSCRIBING.name());
        db.lectureDao().updateAudioPath(lectureId, "/tmp/a.m4a");
        assertEquals(lectureId, db.lectureDao().findIdByAudioPath("/tmp/a.m4a"));
    }
}
