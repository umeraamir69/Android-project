package com.lecturelens.domain.model;

import androidx.annotation.NonNull;

/** One RAG retrieval hit used as a citation in Ask AI. */
public final class RagCitation {

    public final long startMs;
    public final long endMs;
    @NonNull public final String snippet;
    public final float score;

    public RagCitation(long startMs, long endMs, @NonNull String snippet, float score) {
        this.startMs = startMs;
        this.endMs = endMs;
        this.snippet = snippet;
        this.score = score;
    }
}
