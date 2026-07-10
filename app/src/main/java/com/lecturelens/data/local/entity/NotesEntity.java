package com.lecturelens.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * TEMP — Track 4 stub entity until Track 1's {@code LectureLensDatabase} lands.
 * {@code key_terms_json} / {@code action_items_json} are JSON string arrays.
 */
@Entity(tableName = "notes")
public class NotesEntity {

    @PrimaryKey
    @ColumnInfo(name = "lecture_id")
    public long lectureId;

    @NonNull
    @ColumnInfo(name = "summary")
    public String summary = "";

    @NonNull
    @ColumnInfo(name = "key_terms_json")
    public String keyTermsJson = "[]";

    @NonNull
    @ColumnInfo(name = "action_items_json")
    public String actionItemsJson = "[]";
}
