package com.lecturelens.domain.usecase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.lecturelens.core.AppExecutors;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.data.local.dao.SearchDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

/**
 * Flexible search across transcripts (FTS), notes, key terms, action items, and chat.
 */
public class SearchLecturesUseCase {

    private final SearchDao searchDao;
    private final AppExecutors executors;

    @Inject
    public SearchLecturesUseCase(@NonNull SearchDao searchDao,
                                 @NonNull AppExecutors executors) {
        this.searchDao = searchDao;
        this.executors = executors;
    }

    @VisibleForTesting
    protected SearchLecturesUseCase() {
        this.searchDao = null;
        this.executors = null;
    }

    @NonNull
    public LiveData<List<SearchHit>> execute(@Nullable String rawQuery) {
        MutableLiveData<List<SearchHit>> live = new MutableLiveData<>();
        String ftsQuery = toFtsQuery(rawQuery);
        String like = toLikeQuery(rawQuery);
        if (ftsQuery.isEmpty() && like.isEmpty()) {
            live.setValue(Collections.emptyList());
            return live;
        }
        executors.diskIO().execute(() -> {
            List<SearchHit> merged = new ArrayList<>();
            try {
                if (!ftsQuery.isEmpty()) {
                    List<SearchHit> transcript = searchDao.searchTranscriptSync(ftsQuery);
                    if (transcript != null) {
                        merged.addAll(transcript);
                    }
                }
                if (!like.isEmpty()) {
                    addAll(merged, searchDao.searchNotesSummarySync(like));
                    addAll(merged, searchDao.searchKeyTermsSync(like));
                    addAll(merged, searchDao.searchActionItemsSync(like));
                    addAll(merged, searchDao.searchChatSync(like));
                }
            } catch (Exception ignored) {
                // FTS MATCH can throw on bad syntax — return whatever we have.
            }
            live.postValue(merged);
        });
        return live;
    }

    /** Autocomplete suggestions from lecture titles + past Ask AI questions. */
    @NonNull
    public LiveData<List<String>> suggest(@Nullable String rawPrefix) {
        MutableLiveData<List<String>> live = new MutableLiveData<>();
        String prefix = rawPrefix == null ? "" : rawPrefix.trim().toLowerCase(Locale.US);
        if (prefix.length() < 2) {
            live.setValue(Collections.emptyList());
            return live;
        }
        String like = escapeLike(prefix) + "%";
        executors.diskIO().execute(() -> {
            Set<String> out = new LinkedHashSet<>();
            try {
                List<String> titles = searchDao.suggestLectureTitles(like);
                if (titles != null) {
                    out.addAll(titles);
                }
                List<String> chats = searchDao.suggestChatQuestions(like);
                if (chats != null) {
                    for (String c : chats) {
                        if (c != null && !c.trim().isEmpty()) {
                            String shortQ = c.trim();
                            if (shortQ.length() > 60) {
                                shortQ = shortQ.substring(0, 60) + "…";
                            }
                            out.add(shortQ);
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            live.postValue(new ArrayList<>(out));
        });
        return live;
    }

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

    @NonNull
    @VisibleForTesting
    static String toLikeQuery(@Nullable String rawQuery) {
        if (rawQuery == null) {
            return "";
        }
        String trimmed = rawQuery.trim().toLowerCase(Locale.US);
        if (trimmed.isEmpty()) {
            return "";
        }
        return "%" + escapeLike(trimmed) + "%";
    }

    @NonNull
    private static String escapeLike(@NonNull String raw) {
        return raw.replace("%", "").replace("_", "");
    }

    private static void addAll(@NonNull List<SearchHit> target, @Nullable List<SearchHit> extra) {
        if (extra != null) {
            target.addAll(extra);
        }
    }
}
