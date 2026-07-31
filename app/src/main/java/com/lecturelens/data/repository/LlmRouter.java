package com.lecturelens.data.repository;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

import com.lecturelens.core.Result;
import com.lecturelens.data.local.dao.NotesDao;
import com.lecturelens.data.prefs.UserSettingsStore;
import com.lecturelens.data.remote.PipelineErrorStore;
import com.lecturelens.domain.model.LectureStatus;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.repository.LectureRepository;
import com.lecturelens.domain.repository.LibrarySyncRepository;
import com.lecturelens.domain.repository.LlmRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Cloud Gemini vs on-device extractive notes ({@link LocalExtractiveSummarizer}). */
@Singleton
public class LlmRouter implements LlmRepository {

    private final LlmRepository cloud;
    private final UserSettingsStore settings;
    private final Context appContext;
    private final NotesDao notesDao;
    private final NotesEntityMapper notesMapper;
    private final LectureRepository lectureRepository;
    private final PipelineErrorStore errorStore;
    private final LibrarySyncRepository librarySync;

    @Inject
    public LlmRouter(@NonNull @Named("cloudLlm") LlmRepository cloud,
                     @NonNull UserSettingsStore settings,
                     @ApplicationContext @NonNull Context context,
                     @NonNull NotesDao notesDao,
                     @NonNull NotesEntityMapper notesMapper,
                     @NonNull LectureRepository lectureRepository,
                     @NonNull PipelineErrorStore errorStore,
                     @NonNull LibrarySyncRepository librarySync) {
        this.cloud = cloud;
        this.settings = settings;
        this.appContext = context.getApplicationContext();
        this.notesDao = notesDao;
        this.notesMapper = notesMapper;
        this.lectureRepository = lectureRepository;
        this.errorStore = errorStore;
        this.librarySync = librarySync;
    }

    @NonNull
    @Override
    public Result<Notes> summarize(long lectureId, @NonNull String transcriptText) {
        if (shouldUseOnDevice()) {
            return summarizeLocal(lectureId, transcriptText);
        }
        return cloud.summarize(lectureId, transcriptText);
    }

    @NonNull
    @Override
    public LiveData<Notes> observeNotes(long lectureId) {
        return cloud.observeNotes(lectureId);
    }

    private boolean shouldUseOnDevice() {
        String mode = settings.getProcessingMode();
        if (UserSettingsStore.MODE_ON_DEVICE.equals(mode)) {
            return true;
        }
        if (UserSettingsStore.MODE_AUTO.equals(mode)) {
            return !isOnline();
        }
        return false;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    @NonNull
    private Result<Notes> summarizeLocal(long lectureId, @NonNull String transcriptText) {
        lectureRepository.updateStatus(lectureId, LectureStatus.SUMMARIZING);
        errorStore.clear(lectureId);
        String text = transcriptText.trim();
        if (text.isEmpty()) {
            return Result.error("Transcript is empty — cannot summarize.");
        }

        Notes notes = LocalExtractiveSummarizer.summarize(lectureId, text);
        notesDao.insert(notesMapper.toEntity(notes));
        lectureRepository.updateStatus(lectureId, LectureStatus.READY);
        errorStore.clear(lectureId);
        librarySync.pushLecture(lectureId);
        return Result.success(notes);
    }
}
