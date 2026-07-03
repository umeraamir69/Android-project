package com.lecturelens.core;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * Common ViewModel base: one {@code LiveData<UiState<T>>} per screen.
 *
 * Subclasses push state with {@link #setLoading()}, {@link #setSuccess} and
 * {@link #setError}; these use {@code postValue} so they are safe to call
 * from AppExecutors background threads.
 */
public abstract class BaseViewModel<T> extends ViewModel {

    private final MutableLiveData<UiState<T>> uiState = new MutableLiveData<>();

    @NonNull
    public LiveData<UiState<T>> getUiState() {
        return uiState;
    }

    protected void setLoading() {
        uiState.postValue(UiState.loading());
    }

    protected void setSuccess(@NonNull T data) {
        uiState.postValue(UiState.success(data));
    }

    protected void setError(@NonNull String message) {
        uiState.postValue(UiState.error(message));
    }
}
