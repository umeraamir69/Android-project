package com.lecturelens.ui.search;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.lecturelens.core.BaseViewModel;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.domain.usecase.SearchLecturesUseCase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class SearchViewModel extends BaseViewModel<List<SearchResultsAdapter.ListItem>> {

    private static final long DEBOUNCE_MS = 280L;

    private final SearchLecturesUseCase searchLecturesUseCase;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    @Nullable private Runnable pendingSearch;
    @Nullable private Runnable pendingSuggest;
    @Nullable private LiveData<List<SearchHit>> activeSearch;
    @Nullable private Observer<List<SearchHit>> activeObserver;
    @Nullable private LiveData<List<String>> activeSuggest;
    @Nullable private Observer<List<String>> suggestObserver;
    @NonNull private String latestQuery = "";
    @NonNull private String filter = "ALL";

    private final MutableLiveData<List<String>> suggestions = new MutableLiveData<>(new ArrayList<>());
    @Nullable private List<SearchHit> latestHits;

    @Inject
    public SearchViewModel(@NonNull SearchLecturesUseCase searchLecturesUseCase) {
        this.searchLecturesUseCase = searchLecturesUseCase;
        setSuccess(new ArrayList<>());
    }

    @NonNull
    public LiveData<List<String>> getSuggestions() {
        return suggestions;
    }

    public void setFilter(@NonNull String filter) {
        this.filter = filter;
        if (latestHits != null) {
            setSuccess(groupByLecture(applyFilter(latestHits, this.filter)));
        }
    }

    public void onQueryChanged(@Nullable String query) {
        latestQuery = query != null ? query : "";
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        if (pendingSuggest != null) {
            debounceHandler.removeCallbacks(pendingSuggest);
        }
        pendingSearch = this::runSearch;
        pendingSuggest = this::runSuggest;
        debounceHandler.postDelayed(pendingSearch, DEBOUNCE_MS);
        debounceHandler.postDelayed(pendingSuggest, 180L);
    }

    private void runSuggest() {
        detachSuggest();
        LiveData<List<String>> live = searchLecturesUseCase.suggest(latestQuery);
        Observer<List<String>> observer = value ->
                suggestions.setValue(value != null ? value : new ArrayList<>());
        activeSuggest = live;
        suggestObserver = observer;
        live.observeForever(observer);
    }

    private void runSearch() {
        detachActiveSearch();
        String query = latestQuery;
        if (query.trim().isEmpty()) {
            latestHits = new ArrayList<>();
            setSuccess(new ArrayList<>());
            return;
        }
        setLoading();
        LiveData<List<SearchHit>> live = searchLecturesUseCase.execute(query);
        Observer<List<SearchHit>> observer = hits -> {
            latestHits = hits != null ? hits : new ArrayList<>();
            setSuccess(groupByLecture(applyFilter(latestHits, filter)));
        };
        activeSearch = live;
        activeObserver = observer;
        live.observeForever(observer);
    }

    @NonNull
    static List<SearchHit> applyFilter(@NonNull List<SearchHit> hits, @NonNull String filter) {
        if ("ALL".equals(filter)) {
            return hits;
        }
        List<SearchHit> out = new ArrayList<>();
        for (SearchHit hit : hits) {
            String type = hit.sourceType != null ? hit.sourceType : SearchHit.SOURCE_TRANSCRIPT;
            if ("TRANSCRIPT".equals(filter) && SearchHit.SOURCE_TRANSCRIPT.equals(type)) {
                out.add(hit);
            } else if ("NOTES".equals(filter) && (SearchHit.SOURCE_NOTES.equals(type)
                    || SearchHit.SOURCE_KEY_TERM.equals(type)
                    || SearchHit.SOURCE_ACTION.equals(type))) {
                out.add(hit);
            } else if ("CHAT".equals(filter) && SearchHit.SOURCE_CHAT.equals(type)) {
                out.add(hit);
            }
        }
        return out;
    }

    @NonNull
    static List<SearchResultsAdapter.ListItem> groupByLecture(@NonNull List<SearchHit> hits) {
        Map<Long, List<SearchHit>> grouped = new LinkedHashMap<>();
        Map<Long, String> titles = new LinkedHashMap<>();
        for (SearchHit hit : hits) {
            List<SearchHit> bucket = grouped.get(hit.lectureId);
            if (bucket == null) {
                bucket = new ArrayList<>();
                grouped.put(hit.lectureId, bucket);
                titles.put(hit.lectureId, hit.lectureTitle);
            }
            bucket.add(hit);
        }
        List<SearchResultsAdapter.ListItem> items = new ArrayList<>();
        for (Map.Entry<Long, List<SearchHit>> entry : grouped.entrySet()) {
            items.add(new SearchResultsAdapter.HeaderItem(
                    entry.getKey(), titles.get(entry.getKey())));
            for (SearchHit hit : entry.getValue()) {
                items.add(new SearchResultsAdapter.HitItem(hit));
            }
        }
        return items;
    }

    private void detachActiveSearch() {
        if (activeSearch != null && activeObserver != null) {
            activeSearch.removeObserver(activeObserver);
        }
        activeSearch = null;
        activeObserver = null;
    }

    private void detachSuggest() {
        if (activeSuggest != null && suggestObserver != null) {
            activeSuggest.removeObserver(suggestObserver);
        }
        activeSuggest = null;
        suggestObserver = null;
    }

    @Override
    protected void onCleared() {
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        if (pendingSuggest != null) {
            debounceHandler.removeCallbacks(pendingSuggest);
        }
        detachActiveSearch();
        detachSuggest();
        super.onCleared();
    }
}
