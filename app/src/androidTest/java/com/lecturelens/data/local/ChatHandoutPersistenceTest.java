package com.lecturelens.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lecturelens.data.local.entity.ChatMessageEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.domain.model.LectureStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Instrumented coverage for Ask AI chat + handout metadata persistence. */
@RunWith(AndroidJUnit4.class)
public class ChatHandoutPersistenceTest {

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
    public void chatAndHandouts_roundTrip_forLecture() {
        LectureEntity lecture = new LectureEntity();
        lecture.courseId = -1;
        lecture.title = "Persisted lecture";
        lecture.date = 1L;
        lecture.durationMs = 0;
        lecture.status = LectureStatus.READY.name();
        long lectureId = db.lectureDao().insert(lecture);

        ChatMessageEntity user = new ChatMessageEntity();
        user.lectureId = lectureId;
        user.role = "user";
        user.text = "What is recursion?";
        user.citationsJson = "[]";
        user.createdAt = 10L;
        db.chatDao().insert(user);

        ChatMessageEntity assistant = new ChatMessageEntity();
        assistant.lectureId = lectureId;
        assistant.role = "assistant";
        assistant.text = "A function that calls itself.";
        assistant.citationsJson = "[0]";
        assistant.createdAt = 11L;
        db.chatDao().insert(assistant);

        HandoutEntity handout = new HandoutEntity();
        handout.lectureId = lectureId;
        handout.imagePath = "/tmp/slide.pdf";
        handout.mimeType = "application/pdf";
        handout.displayName = "Slides.pdf";
        handout.extractedText = "Recursion chapter";
        handout.remoteUrl = "https://example.com/h.pdf";
        handout.createdAt = 12L;
        db.handoutDao().insert(handout);

        List<ChatMessageEntity> chat = db.chatDao().getByLectureSync(lectureId);
        assertEquals(2, chat.size());
        assertEquals("user", chat.get(0).role);

        List<ChatMessageEntity> recent = db.chatDao().getRecentSync(lectureId, 1);
        assertEquals(1, recent.size());
        assertEquals("assistant", recent.get(0).role);

        List<HandoutEntity> handouts = db.handoutDao().getByLectureSync(lectureId);
        assertEquals(1, handouts.size());
        assertEquals("Slides.pdf", handouts.get(0).displayName);
        assertNotNull(handouts.get(0).remoteUrl);
    }
}
