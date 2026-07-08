package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.lecturelens.data.local.dao.TranscriptDao;
import com.lecturelens.data.local.entity.TranscriptEntity;
import com.lecturelens.data.local.entity.TranscriptSegmentEntity;
import com.lecturelens.domain.model.Transcript;
import com.lecturelens.domain.model.TranscriptSegment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class TranscriptEntityMapper {

    @Inject
    TranscriptEntityMapper() {
    }

    @Nullable
    public Transcript toDomain(@Nullable TranscriptEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Transcript(
                entity.lectureId,
                entity.fullText,
                entity.language,
                entity.modelUsed);
    }

    @NonNull
    public TranscriptEntity toEntity(@NonNull Transcript transcript) {
        TranscriptEntity entity = new TranscriptEntity();
        entity.lectureId = transcript.getLectureId();
        entity.fullText = transcript.getFullText();
        entity.language = transcript.getLanguage();
        entity.modelUsed = transcript.getModelUsed();
        return entity;
    }

    @NonNull
    public List<TranscriptSegmentEntity> toSegmentEntities(long lectureId,
                                                           @NonNull List<TranscriptSegment> segments) {
        List<TranscriptSegmentEntity> entities = new ArrayList<>(segments.size());
        for (TranscriptSegment segment : segments) {
            TranscriptSegmentEntity entity = new TranscriptSegmentEntity();
            entity.id = segment.getId();
            entity.lectureId = lectureId;
            entity.startMs = segment.getStartMs();
            entity.endMs = segment.getEndMs();
            entity.text = segment.getText();
            entities.add(entity);
        }
        return entities;
    }

    @NonNull
    public List<TranscriptSegment> toSegmentDomain(@NonNull List<TranscriptSegmentEntity> entities) {
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        List<TranscriptSegment> segments = new ArrayList<>(entities.size());
        for (TranscriptSegmentEntity entity : entities) {
            segments.add(new TranscriptSegment(
                    entity.id,
                    entity.lectureId,
                    entity.startMs,
                    entity.endMs,
                    entity.text));
        }
        return segments;
    }

    @NonNull
    public static LiveData<Transcript> mapTranscriptLiveData(@NonNull LiveData<TranscriptEntity> source) {
        return Transformations.map(source, entity -> {
            TranscriptEntityMapper mapper = new TranscriptEntityMapper();
            Transcript transcript = mapper.toDomain(entity);
            return transcript != null
                    ? transcript
                    : new Transcript(0L, "", "en-US", "");
        });
    }

    @NonNull
    public static LiveData<List<TranscriptSegment>> mapSegmentsLiveData(
            @NonNull LiveData<List<TranscriptSegmentEntity>> source) {
        return Transformations.map(source, entities -> {
            if (entities == null) {
                return Collections.emptyList();
            }
            return new TranscriptEntityMapper().toSegmentDomain(entities);
        });
    }
}
