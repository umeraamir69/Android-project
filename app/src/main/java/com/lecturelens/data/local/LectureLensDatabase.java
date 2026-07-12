package com.lecturelens.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.lecturelens.data.local.dao.CourseDao;
import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.SearchDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.CourseEntity;
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
 *   <li>1 — initial schema (this file). Pre-release dev builds used a v2
 *       in-memory temp DB; nothing persisted, so we restart at 1.</li>
 * </ul>
 */
@Database(
        entities = {
                CourseEntity.class,
                LectureEntity.class,
                TranscriptEntity.class,
                TranscriptSegmentEntity.class,
                NotesEntity.class,
                TranscriptFtsEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class LectureLensDatabase extends RoomDatabase {

    public static final String NAME = "lecturelens.db";

    public abstract CourseDao courseDao();

    public abstract LectureDao lectureDao();

    public abstract TranscriptDao transcriptDao();

    public abstract NotesDao notesDao();

    public abstract SearchDao searchDao();
}
