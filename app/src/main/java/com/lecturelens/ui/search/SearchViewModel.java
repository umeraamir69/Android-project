package com.lecturelens.ui.search;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
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

/**
 * Track 5 — debounces the query box (300 ms) and groups FTS hits by lecture.
 */
@HiltViewModel
public class SearchViewModel extends BaseViewModel<List<SearchResultsAdapter.ListItem>> {

    private static final long DEBOUNCE_MS = 300L;

    private final SearchLecturesUseCase searchLecturesUseCase;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    @Nullable private Runnable pendingSearch;
    @Nullable private LiveData<List<SearchHit>> activeSearch;
    @Nullable private Observer<List<SearchHit>> activeObserver;
    @NonNull private String latestQuery = "";

    @Inject
    public SearchViewModel(@NonNull SearchLecturesUseCase searchLecturesUseCase) {
        this.searchLecturesUseCase = searchLecturesUseCase;
        setSuccess(new ArrayList<>());
    }

    public void onQueryChanged(@Nullable String query) {
        latestQuery = query != null ? query : "";
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = this::runSearch;
        debounceHandler.postDelayed(pendingSearch, DEBOUNCE_MS);
    }

    private void runSearch() {
        detachActiveSearch();
        String query = latestQuery;
        if (query.trim().isEmpty()) {
            setSuccess(new ArrayList<>());
            return;
        }
        setLoading();
        LiveData<List<SearchHit>> live = searchLecturesUseCase.execute(query);
        Observer<List<SearchHit>> observer = hits -> {
            if (hits == null) {
                setSuccess(new ArrayList<>());
                return;
            }
            setSuccess(groupByLecture(hits));
        };
        activeSearch = live;
        activeObserver = observer;
        live.observeForever(observer);
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

    @Override
    protected void onCleared() {
        if (pendingSearch != null) {
            debounceHandler.removeCallbacks(pendingSearch);
        }
        detachActiveSearch();
        super.onCleared();
    }
}
