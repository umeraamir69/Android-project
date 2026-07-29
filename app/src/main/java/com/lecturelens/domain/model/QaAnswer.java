package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

/** Ask-AI answer with optional RAG citations. */
public final class QaAnswer {

    @NonNull public final String text;
    @NonNull public final List<RagCitation> citations;

    public QaAnswer(@NonNull String text, @NonNull List<RagCitation> citations) {
        this.text = text;
        this.citations = citations != null ? citations : Collections.emptyList();
    }

    @NonNull
    public static QaAnswer plain(@NonNull String text) {
        return new QaAnswer(text, Collections.emptyList());
    }
}
