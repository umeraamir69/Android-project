package com.lecturelens.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.lecturelens.data.local.dao.ChatDao;
import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.EmbeddingDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.SearchDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.ChatMessageEntity;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.EmbeddingEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptFtsEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;

/**
 * Track 1 — persistent Room database.
 *
 * <p>Version history:
 * <ul>
 *   <li>1 — initial schema.</li>
 *   <li>2 — transcript_segments.speaker_tag for STT diarization.</li>
 *   <li>3 — handouts table (quiz / notes photo OCR).</li>
 *   <li>4 — embeddings table for stretch RAG.</li>
 *   <li>5 — chat_messages for Ask AI history.</li>
 *   <li>6 — courses.professor for notes attribution.</li>
 *   <li>7 — handouts mime/display_name/remote_url.</li>
 *   <li>8 — migration path established (no wipe from 7+).</li>
 * </ul>
 */
@Database(
        entities = {
                CourseEntity.class,
                LectureEntity.class,
                TranscriptEntity.class,
                TranscriptSegmentEntity.class,
                NotesEntity.class,
                TranscriptFtsEntity.class,
                HandoutEntity.class,
                EmbeddingEntity.class,
                ChatMessageEntity.class
        },
        version = 8,
        exportSchema = false
)
public abstract class LectureLensDatabase extends RoomDatabase {

    public static final String NAME = "lecturelens.db";

    public abstract CourseDao courseDao();

    public abstract LectureDao lectureDao();

    public abstract TranscriptDao transcriptDao();

    public abstract NotesDao notesDao();

    public abstract SearchDao searchDao();

    public abstract HandoutDao handoutDao();

    public abstract EmbeddingDao embeddingDao();

    public abstract ChatDao chatDao();
}
