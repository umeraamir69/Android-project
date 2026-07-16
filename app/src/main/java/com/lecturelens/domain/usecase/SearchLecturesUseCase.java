package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lecturelens.data.local.SearchHit;
import com.lecturelens.data.local.dao.SearchDao;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

/**
 * Track 5 — FTS4 keyword search over transcript segments (arch doc §4.2).
 *
 * <p>Sanitizes the raw user query into a safe FTS4 {@code MATCH} expression
 * (prefix tokens) and delegates to {@link SearchDao}. Empty / whitespace
 * queries return an empty list without hitting the database.
 */
public class SearchLecturesUseCase {

    private final SearchDao searchDao;

    @Inject
    public SearchLecturesUseCase(@NonNull SearchDao searchDao) {
        this.searchDao = searchDao;
    }

    @VisibleForTesting
    protected SearchLecturesUseCase() {
        this.searchDao = null;
    }

    /**
     * @return LiveData of hits; never null. Emits an empty list for blank queries.
     */
    @NonNull
    public LiveData<List<SearchHit>> execute(@Nullable String rawQuery) {
        String ftsQuery = toFtsQuery(rawQuery);
        if (ftsQuery.isEmpty()) {
            MutableLiveData<List<SearchHit>> empty = new MutableLiveData<>();
            empty.setValue(Collections.emptyList());
            return empty;
        }
        return searchDao.search(ftsQuery);
    }

    /**
     * Builds an FTS4 prefix query from free-form user text.
     * Strips characters that change FTS operator meaning, then appends {@code *}
     * to each token so "life" matches "lifecycle".
     */
    @NonNull
    @VisibleForTesting
    static String toFtsQuery(@Nullable String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String trimmed = rawQuery.trim().toLowerCase(Locale.US);
        if (trimmed.isEmpty()) {
            return "";
        }
        // Drop FTS operators / quotes so a typed "OR" or "*" cannot break MATCH.
        String cleaned = trimmed.replaceAll("[\"*()^:]", " ").trim();
        if (cleaned.isEmpty()) {
            return "";
        }
        String[] tokens = cleaned.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(token).append('*');
        }
        return sb.toString();
    }
}
