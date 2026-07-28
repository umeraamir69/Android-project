package com.lecturelens.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.HandoutDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.SearchDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.CourseEntity;
import com.lecturelens.data.local.entity.HandoutEntity;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptFtsEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;

/**
 * Track 1 — the real, persistent Room database (replaces the temporary
 * in-memory {@code UploadTempDatabase}).
 *
 * <p>Schema per arch doc §3.3: courses, lectures, transcripts,
 * transcript_segments, notes, transcripts_fts (external-content FTS4 kept in
 * sync by Room-generated triggers). The stretch {@code embeddings} table is
 * added in a later version bump with a proper Migration.
 *
 * <p>Version history:
 * <ul>
 *   <li>1 — initial schema.</li>
 *   <li>2 — transcript_segments.speaker_tag for STT diarization.</li>
 *   <li>3 — handouts table (quiz / notes photo OCR).</li>
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
                HandoutEntity.class
        },
        version = 3,
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
}
