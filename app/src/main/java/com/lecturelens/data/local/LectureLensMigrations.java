package com.lecturelens.data.local;

import androidx.annotation.NonNull;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

/**
 * Forward-only migrations from DB v7.
 * Older installs (v1–v6) still wipe via {@code fallbackToDestructiveMigrationFrom}.
 */
public final class LectureLensMigrations {

    private LectureLensMigrations() {
    }

    /** v7 → v8: no schema change; reserved so future bumps don't wipe user data. */
    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Intentionally empty — establishes non-destructive upgrade path.
        }
    };
}
