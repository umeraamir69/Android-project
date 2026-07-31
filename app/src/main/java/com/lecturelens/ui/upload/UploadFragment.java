package com.lecturelens.ui.upload;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.lecturelens.R;
import com.lecturelens.core.AppExecutors;
import com.lecturelens.core.UiState;
import com.lecturelens.data.audio.AudioFileFactory;
import com.lecturelens.data.audio.RecordingService;
import com.lecturelens.databinding.FragmentUploadBinding;
import com.lecturelens.domain.model.Course;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Track 3 — Record / Import screen. Observes {@link UploadViewModel}'s
 * {@link RecordingState} to drive the transport UI, requests RECORD_AUDIO, runs
 * the foreground {@link RecordingService} during capture, imports files via SAF,
 * and navigates to the lecture on save.
 */
@AndroidEntryPoint
public class UploadFragment extends Fragment {

    @Inject AppExecutors executors;
    @Inject AudioFileFactory fileFactory;

    @Nullable private FragmentUploadBinding binding;
    private UploadViewModel viewModel;
    private boolean serviceRunning;
    private boolean namingDialogVisible;
    @NonNull private final List<Long> categoryIds = new ArrayList<>();
    @NonNull private final List<String> categoryLabels = new ArrayList<>();
    @Nullable private ArrayAdapter<String> categoryAdapter;

    private final ActivityResultLauncher<String> recordPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> viewModel.onPermissionResult(granted));

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> { /* best-effort; recording proceeds regardless */ });

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                    this::onImportPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(UploadViewModel.class);

        FragmentUploadBinding b = requireBinding();
        b.toolbar.setNavigationOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigateUp();
            } catch (IllegalStateException e) {
                requireActivity().finish();
            }
        });
        b.toolbar.inflateMenu(R.menu.menu_help);
        b.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_help) {
                com.lecturelens.ui.util.HelpDialogs.show(this, getString(R.string.title_upload));
                return true;
            }
            return false;
        });
        com.lecturelens.ui.util.SectionFeedback.toast(this,
                getString(R.string.toast_section_ready, getString(R.string.title_upload)));
        b.buttonRecord.setOnClickListener(v -> onRecordTapped());
        b.buttonPause.setOnClickListener(v -> onPauseResumeTapped());
        b.buttonStop.setOnClickListener(v -> viewModel.onStopClicked());
        b.buttonImport.setOnClickListener(v -> {
            viewModel.onImportClicked();
            importLauncher.launch(new String[]{"audio/*"});
        });
        b.buttonRetry.setOnClickListener(v -> onRecordTapped());
        b.buttonCancel.setOnClickListener(v -> viewModel.onPermissionCancel());

        categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                categoryLabels);
        b.inputCategory.setAdapter(categoryAdapter);
        b.inputCategory.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < categoryIds.size()) {
                viewModel.selectCourse(categoryIds.get(position));
            }
        });

        String[] langLabels = getResources().getStringArray(R.array.stt_language_labels);
        String[] langValues = getResources().getStringArray(R.array.stt_language_values);
        b.inputLanguage.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, langLabels));
        b.inputLanguage.setOnItemClickListener((parent, view1, position, id) -> {
            if (position >= 0 && position < langValues.length) {
                viewModel.selectLanguage(langValues[position]);
            }
        });
        String currentLang = viewModel.currentLanguage();
        for (int i = 0; i < langValues.length; i++) {
            if (langValues[i].equals(currentLang)) {
                b.inputLanguage.setText(langLabels[i], false);
                break;
            }
        }

        viewModel.getRecordingState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::renderSaveState);
        viewModel.getCourses().observe(getViewLifecycleOwner(), this::bindCategories);
        viewModel.getSelectedCourseId().observe(getViewLifecycleOwner(), id ->
                syncCategoryFieldText());
    }

    private void bindCategories(@Nullable List<Course> courses) {
        categoryIds.clear();
        categoryLabels.clear();
        categoryIds.add(UploadViewModel.UNCATEGORIZED_COURSE_ID);
        categoryLabels.add(getString(R.string.upload_category_uncategorized));
        if (courses != null) {
            for (Course course : courses) {
                categoryIds.add(course.getId());
                categoryLabels.add(course.getName());
            }
        }
        if (categoryAdapter != null) {
            categoryAdapter.notifyDataSetChanged();
        }
        // If nav passed a courseId that exists, keep it; otherwise ensure selection is valid.
        long selected = viewModel.currentCourseId();
        if (!categoryIds.contains(selected)) {
            viewModel.selectCourse(UploadViewModel.UNCATEGORIZED_COURSE_ID);
        }
        syncCategoryFieldText();
    }

    private void syncCategoryFieldText() {
        if (binding == null) {
            return;
        }
        long selected = viewModel.currentCourseId();
        int index = categoryIds.indexOf(selected);
        if (index >= 0 && index < categoryLabels.size()) {
            binding.inputCategory.setText(categoryLabels.get(index), false);
        }
    }

    // ---- User actions ----

    private void onRecordTapped() {
        viewModel.onRecordClicked();
        maybeRequestNotificationPermission();
        if (hasRecordPermission()) {
            viewModel.onPermissionResult(true);
        } else {
            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void onPauseResumeTapped() {
        RecordingState state = viewModel.getRecordingState().getValue();
        if (state instanceof RecordingState.Paused) {
            viewModel.onResumeClicked();
        } else {
            viewModel.onPauseClicked();
        }
    }

    private void onImportPicked(@Nullable Uri uri) {
        if (uri == null) {
            viewModel.onImportCancelled();
            return;
        }
        // Copy off the main thread; the ViewModel takes over from onImported().
        executors.diskIO().execute(() -> {
            try {
                File dest = fileFactory.newRecordingFile();
                copy(uri, dest);
                long durationMs = readDurationMs(dest);
                executors.mainThread().execute(() ->
                        viewModel.onImported(dest.getAbsolutePath(), durationMs));
            } catch (Exception e) {
                executors.mainThread().execute(() -> {
                    viewModel.onImportCancelled();
                    if (binding != null) {
                        Toast.makeText(requireContext(),
                                R.string.upload_import_failed, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    // ---- Rendering ----

    private void render(@NonNull RecordingState state) {
        FragmentUploadBinding b = requireBinding();

        boolean recording = state instanceof RecordingState.Recording;
        boolean paused = state instanceof RecordingState.Paused;
        boolean denied = state instanceof RecordingState.PermissionDenied;
        boolean saving = state instanceof RecordingState.Saving;
        boolean naming = state instanceof RecordingState.Naming;
        boolean capturing = recording || paused;
        boolean busy = saving || naming;

        // Transport visibility
        b.buttonRecord.setVisibility(capturing || denied || busy ? View.GONE : View.VISIBLE);
        b.buttonImport.setVisibility(capturing || denied || busy ? View.GONE : View.VISIBLE);
        b.buttonPause.setVisibility(capturing ? View.VISIBLE : View.GONE);
        b.buttonStop.setVisibility(capturing ? View.VISIBLE : View.GONE);
        b.buttonPause.setText(paused ? R.string.upload_resume : R.string.upload_pause);

        // Category / language pickers — hide while recording so they can't collide
        // with Pause/Stop in the bottom action bar.
        boolean showPickers = !capturing && !busy && !denied;
        b.inputCategoryLayout.setVisibility(showPickers ? View.VISIBLE : View.GONE);
        b.inputLanguageLayout.setVisibility(showPickers ? View.VISIBLE : View.GONE);
        b.inputCategory.setEnabled(showPickers);
        b.inputCategoryLayout.setEnabled(showPickers);
        b.inputLanguage.setEnabled(showPickers);
        b.inputLanguageLayout.setEnabled(showPickers);

        // Permission-denied section
        int deniedVis = denied ? View.VISIBLE : View.GONE;
        b.textPermission.setVisibility(deniedVis);
        b.buttonRetry.setVisibility(deniedVis);
        b.buttonCancel.setVisibility(deniedVis);

        // Saving spinner
        b.progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);
        b.textHint.setVisibility(state instanceof RecordingState.Idle ? View.VISIBLE : View.GONE);

        // Indicator + timer
        b.indicator.setActive(recording);
        if (recording) {
            RecordingState.Recording r = (RecordingState.Recording) state;
            b.indicator.setVisibility(View.VISIBLE);
            b.indicator.setAmplitude(r.amplitude);
            b.textTimer.setText(formatElapsed(r.elapsedMs));
        } else if (paused) {
            b.indicator.setVisibility(View.INVISIBLE);
            b.textTimer.setText(formatElapsed(((RecordingState.Paused) state).elapsedMs));
        } else {
            b.indicator.setVisibility(View.INVISIBLE);
            if (state instanceof RecordingState.Idle) {
                b.textTimer.setText(formatElapsed(0L));
            }
        }

        // Foreground service lifecycle
        if (recording) {
            startRecordingService();
        } else {
            stopRecordingService();
        }

        if (naming) {
            showNameLectureDialog((RecordingState.Naming) state);
        } else {
            namingDialogVisible = false;
        }

        // Navigate on save
        if (state instanceof RecordingState.Saved) {
            com.lecturelens.ui.util.AppNavigator.openLecture(
                    this, ((RecordingState.Saved) state).lectureId, -1L);
        }
    }

    private void showNameLectureDialog(@NonNull RecordingState.Naming naming) {
        if (namingDialogVisible) {
            return;
        }
        namingDialogVisible = true;
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.dialog_lecture_title_hint);
        input.setText(naming.suggestedTitle);
        input.setSelectAllOnFocus(true);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = pad;
        params.rightMargin = pad;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_name_lecture_title)
                .setView(container)
                .setCancelable(false)
                .setNegativeButton(R.string.action_use_default_name, (d, w) -> {
                    namingDialogVisible = false;
                    viewModel.onTitleSkipped();
                })
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    namingDialogVisible = false;
                    CharSequence text = input.getText();
                    viewModel.onTitleConfirmed(text != null ? text.toString() : "");
                })
                .show();
        input.requestFocus();
    }

    private void renderSaveState(@Nullable UiState<Long> uiState) {
        if (uiState instanceof UiState.Error && binding != null) {
            Toast.makeText(requireContext(),
                    ((UiState.Error<Long>) uiState).message, Toast.LENGTH_LONG).show();
        }
    }

    private void navigateToLecture(long lectureId) {
        stopRecordingService();
        com.lecturelens.ui.util.AppNavigator.openLecture(this, lectureId, -1L);
    }

    // ---- Foreground service ----

    private void startRecordingService() {
        if (!serviceRunning) {
            RecordingService.start(requireContext());
            serviceRunning = true;
        }
    }

    private void stopRecordingService() {
        if (serviceRunning) {
            RecordingService.stop(requireContext());
            serviceRunning = false;
        }
    }

    // ---- Helpers ----

    private boolean hasRecordPermission() {
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void copy(@NonNull Uri source, @NonNull File dest) throws Exception {
        try (InputStream in = requireContext().getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            if (in == null) {
                throw new IllegalStateException("Cannot open " + source);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    private static long readDurationMs(@NonNull File file) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(file.getAbsolutePath());
            String value = retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION);
            return value != null ? Long.parseLong(value) : 0L;
        } catch (RuntimeException e) {
            return 0L;
        } finally {
            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }
    }

    @NonNull
    private static String formatElapsed(long elapsedMs) {
        long totalSeconds = elapsedMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    @NonNull
    private FragmentUploadBinding requireBinding() {
        FragmentUploadBinding b = binding;
        if (b == null) {
            throw new IllegalStateException("binding accessed outside view lifecycle");
        }
        return b;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopRecordingService();
        binding = null;
    }
}
