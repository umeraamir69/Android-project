package com.lecturelens.di;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.LectureEntity;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;

/**
 * TEMPORARY — shared in-memory Room DB for Tracks 3 + 4 until Track 1's real
 * {@code LectureLensDatabase} lands. DELETE once DatabaseModule provides DAOs.
 */
@Database(
        entities = {
                LectureEntity.class,
                TranscriptEntity.class,
                TranscriptSegmentEntity.class,
                NotesEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class UploadTempDatabase extends RoomDatabase {
    public abstract LectureDao lectureDao();

    public abstract TranscriptDao transcriptDao();

    public abstract NotesDao notesDao();
}