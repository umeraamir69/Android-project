package com.lecturelens.di;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.lecturelens.data.local.dao.LectureDao;
import com.lecturelens.data.local.entity.LectureEntity;

/**
 * TEMPORARY — owned by Track 3 only so the Hilt graph resolves and the record →
 * save path runs before Track 1's real Room DB lands. In-memory (see UploadModule),
 * so it writes nothing to disk and cannot clash with Zeeshan's future
 * {@code LectureLensDatabase}. DELETE this class + the two temp @Provides in
 * UploadModule once Track 1's DatabaseModule provides LectureDao.
 */
@Database(entities = {LectureEntity.class}, version = 1, exportSchema = false)
public abstract class UploadTempDatabase extends RoomDatabase {
    public abstract LectureDao lectureDao();
}