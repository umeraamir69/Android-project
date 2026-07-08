package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.local.entity.NotesEntity;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.LlmRepository;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Maps Room {@link NotesEntity} rows to frozen domain {@link Notes}.
 */
@Singleton
public final class NotesEntityMapper {

    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST = new TypeToken<List<String>>() { }.getType();

    @Inject
    NotesEntityMapper() {
    }

    @Nullable
    public Notes toDomain(@Nullable NotesEntity entity) {
        if (entity == null) {
            return null;
        }
        List<String> keyTerms = parseList(entity.keyTermsJson);
        List<String> actionItems = parseList(entity.actionItemsJson);
        return new Notes(entity.lectureId, entity.summary, keyTerms, actionItems);
    }

    @NonNull
    public NotesEntity toEntity(@NonNull Notes notes) {
        NotesEntity entity = new NotesEntity();
        entity.lectureId = notes.getLectureId();
        entity.summary = notes.getSummary();
        entity.keyTermsJson = GSON.toJson(notes.getKeyTerms());
        entity.actionItemsJson = GSON.toJson(notes.getActionItems());
        return entity;
    }

    @NonNull
    private static List<String> parseList(@NonNull String json) {
        try {
            List<String> list = GSON.fromJson(json, STRING_LIST);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @NonNull
    public static LiveData<Notes> mapNotesLiveData(@NonNull LiveData<NotesEntity> source) {
        return Transformations.map(source, entity -> {
            NotesEntityMapper mapper = new NotesEntityMapper();
            Notes notes = mapper.toDomain(entity);
            return notes != null ? notes : emptyNotes(0L);
        });
    }

    @NonNull
    private static Notes emptyNotes(long lectureId) {
        return new Notes(lectureId, "", Collections.emptyList(), Collections.emptyList());
    }
}
