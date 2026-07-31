package com.lecturelens.ui.lecture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.core.player.AudioPlaybackController;
import com.lecturelens.databinding.FragmentLectureViewBinding;

import com.lecturelens.domain.model.Course;
import com.lecturelens.domain.usecase.ExportFormat;
import com.lecturelens.domain.usecase.ExportResult;
import com.lecturelens.ui.library.LibraryViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Track 5 — lecture screen with Player / Transcript / Notes tabs.
 * Owns the shared {@link AudioPlaybackController} and polls playback position
 * so the transcript tab can auto-scroll.
 */
@AndroidEntryPoint
public class LectureViewFragment extends Fragment {

    private static final long POSITION_POLL_MS = 300L;

    @Nullable private FragmentLectureViewBinding binding;
    private LectureViewModel viewModel;
    private AudioPlaybackController playbackController;
    private final Handler positionHandler = new Handler(Looper.getMainLooper());
    @Nullable private Runnable positionTicker;
    private boolean mediaPrepared;
    @Nullable private String lastPreparedPath;
    private boolean initialSeekApplied;
    @Nullable private String lastShownPipelineError;

    private final ActivityResultLauncher<String[]> handoutPicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::onHandoutPicked);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLectureViewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LectureViewModel.class);

        playbackController = new AudioPlaybackController();
        playbackController.attach(requireContext());
        playbackController.setListener(new AudioPlaybackController.Listener() {
            @Override
            public void onPlaybackError(@NonNull String message) {
                mediaPrepared = false;
                if ("NO_AUDIO".equals(message) || "MISSING_FILE".equals(message)) {
                    return; // empty state is shown in the player tab
                }
                if (binding != null) {
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onReady() {
                mediaPrepared = true;
                applyPendingSeek(/* consume= */ true);
            }
        });

        binding.toolbar.setNavigationOnClickListener(v -> {
            try {
                NavHostFragment.findNavController(this).navigateUp();
            } catch (IllegalStateException e) {
                requireActivity().finish();
            }
        });
        binding.toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_rename) {
                showRenameDialog();
                return true;
            }
            if (id == R.id.menu_move) {
                showMoveDialog();
                return true;
            }
            if (id == R.id.menu_retranscribe) {
                viewModel.retranscribe();
                return true;
            }
            if (id == R.id.menu_export) {
                showShareSheet();
                return true;
            }
            if (id == R.id.menu_add_handout) {
                handoutPicker.launch(new String[]{
                        "image/*",
                        "application/pdf",
                        "text/plain",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                });
                return true;
            }
            if (id == R.id.menu_help) {
                com.lecturelens.ui.util.HelpDialogs.show(this, getString(R.string.title_lecture));
                return true;
            }
            return false;
        });
        com.lecturelens.ui.util.SectionFeedback.toast(this,
                getString(R.string.toast_section_ready, getString(R.string.title_lecture)));

        binding.pager.setAdapter(new LecturePagerAdapter(this));
        new TabLayoutMediator(binding.tabLayout, binding.pager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.lecture_tab_player);
                    break;
                case 1:
                    tab.setText(R.string.lecture_tab_transcript);
                    break;
                default:
                    tab.setText(R.string.lecture_tab_notes);
                    break;
            }
        }).attach();

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getExportResult().observe(getViewLifecycleOwner(), this::dispatchShare);
        viewModel.getCloudShareCode().observe(getViewLifecycleOwner(), this::showCloudShareCode);
        viewModel.getMessageEvent().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && binding != null) {
                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_LONG).show();
            }
        });

        startPositionPolling();
    }

    @Nullable
    public AudioPlaybackController getPlaybackController() {
        return playbackController;
    }

    private void maybeShowPipelineErrorDialog(@Nullable String err, boolean hasTranscript) {
        if (binding == null) {
            return;
        }
        if (err == null || err.isEmpty()) {
            lastShownPipelineError = null;
            return;
        }
        if (err.equals(lastShownPipelineError)) {
            return;
        }
        lastShownPipelineError = err;
        int titleRes = hasTranscript
                ? R.string.pipeline_error_notes_title
                : R.string.pipeline_error_transcribe_title;
        int actionRes = hasTranscript
                ? R.string.action_retry_notes
                : R.string.action_retranscribe;
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(err)
                .setPositiveButton(R.string.pipeline_error_dismiss, null)
                .setNeutralButton(actionRes, (d, w) -> viewModel.retranscribe())
                .show();
    }

    private void render(@NonNull UiState<LectureDetail> state) {
        if (binding == null) {
            return;
        }
        boolean loading = state instanceof UiState.Loading;
        binding.progressLecture.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            LectureDetail detail = ((UiState.Success<LectureDetail>) state).data;
            maybeShowPipelineErrorDialog(detail.pipelineError, !detail.segments.isEmpty());
            binding.toolbar.setTitle(detail.lecture.getTitle());
            if (detail.hasPlayableAudio()) {
                String path = detail.lecture.getAudioPath();
                if (path != null && !path.equals(lastPreparedPath)) {
                    lastPreparedPath = path;
                    mediaPrepared = false;
                    playbackController.prepare(path);
                }
            } else {
                mediaPrepared = false;
                lastPreparedPath = null;
                // No audio — still jump the transcript highlight for search.
                applyPendingSeek(/* consume= */ true);
            }
            // Highlight as soon as segments arrive; player seeks on onReady.
            long peek = viewModel.peekPendingSeekMs();
            if (peek >= 0L) {
                viewModel.onPlaybackPosition(peek);
            }
        } else if (state instanceof UiState.Error) {
            Snackbar.make(binding.getRoot(),
                    ((UiState.Error<LectureDetail>) state).message,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void applyPendingSeek(boolean consume) {
        if (initialSeekApplied) {
            return;
        }
        long seek = consume
                ? viewModel.consumePendingSeekMs()
                : viewModel.peekPendingSeekMs();
        if (seek < 0L) {
            return;
        }
        viewModel.onPlaybackPosition(seek);
        if (mediaPrepared && playbackController != null) {
            playbackController.seekTo(seek);
        }
        if (consume) {
            initialSeekApplied = true;
        }
    }

    private void onHandoutPicked(@Nullable Uri uri) {
        if (uri == null || binding == null) {
            return;
        }
        try {
            // Persist permission when available (OpenDocument).
            try {
                requireContext().getContentResolver().takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
            }
            File dir = new File(requireContext().getFilesDir(), "handouts");
            if (!dir.exists() && !dir.mkdirs()) {
                Snackbar.make(binding.getRoot(), R.string.handout_pick_failed, Snackbar.LENGTH_LONG)
                        .show();
                return;
            }
            String mime = requireContext().getContentResolver().getType(uri);
            if (mime == null || mime.isEmpty()) {
                mime = "application/octet-stream";
            }
            String displayName = queryDisplayName(uri);
            String ext = extensionForMime(mime, displayName);
            File out = new File(dir, "handout_" + viewModel.getLectureId()
                    + "_" + System.currentTimeMillis() + ext);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 OutputStream os = new FileOutputStream(out)) {
                if (in == null) {
                    throw new IllegalStateException("null stream");
                }
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) >= 0) {
                    os.write(buf, 0, n);
                }
            }
            viewModel.addHandoutFile(out, mime, displayName);
            binding.pager.setCurrentItem(2, true);
        } catch (Exception e) {
            Snackbar.make(binding.getRoot(), R.string.handout_pick_failed, Snackbar.LENGTH_LONG)
                    .show();
        }
    }

    @NonNull
    private String queryDisplayName(@NonNull Uri uri) {
        String fallback = "handout";
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(
                uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) {
                    return name.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    @NonNull
    private static String extensionForMime(@NonNull String mime, @NonNull String displayName) {
        String lower = displayName.toLowerCase(java.util.Locale.US);
        if (lower.contains(".")) {
            return lower.substring(lower.lastIndexOf('.'));
        }
        if (mime.contains("png")) {
            return ".png";
        }
        if (mime.contains("webp")) {
            return ".webp";
        }
        if (mime.contains("pdf")) {
            return ".pdf";
        }
        if (mime.contains("wordprocessingml") || mime.contains("docx")) {
            return ".docx";
        }
        if (mime.contains("msword")) {
            return ".doc";
        }
        if (mime.startsWith("text/")) {
            return ".txt";
        }
        if (mime.startsWith("image/")) {
            return ".jpg";
        }
        return ".bin";
    }

    private void showShareSheet() {
        CharSequence[] items = new CharSequence[]{
                getString(R.string.share_option_text),
                getString(R.string.share_option_whatsapp),
                getString(R.string.share_option_markdown),
                getString(R.string.share_option_pdf),
                getString(R.string.share_option_doc),
                getString(R.string.share_option_cloud)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_share_title)
                .setItems(items, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            viewModel.share(ExportFormat.TEXT, false);
                            break;
                        case 1:
                            viewModel.share(ExportFormat.TEXT, true);
                            break;
                        case 2:
                            viewModel.share(ExportFormat.MARKDOWN, false);
                            break;
                        case 3:
                            viewModel.share(ExportFormat.PDF, false);
                            break;
                        case 4:
                            viewModel.share(ExportFormat.DOC, false);
                            break;
                        case 5:
                            viewModel.shareToCloud();
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void dispatchShare(@Nullable ExportResult result) {
        if (result == null || binding == null) {
            return;
        }
        if (result.kind == ExportResult.Kind.TEXT) {
            shareText(result.text != null ? result.text : "", result.preferWhatsApp);
        } else if (result.file != null) {
            shareFile(result.file, result.mimeType != null ? result.mimeType : "*/*");
        }
        Snackbar.make(binding.getRoot(), R.string.export_success, Snackbar.LENGTH_SHORT).show();
    }

    private void shareText(@NonNull String text, boolean preferWhatsApp) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.export_share_title));
        if (preferWhatsApp) {
            share.setPackage("com.whatsapp");
            try {
                startActivity(share);
                return;
            } catch (Exception ignored) {
                share.setPackage(null);
            }
        }
        startActivity(Intent.createChooser(share, getString(R.string.export_share_title)));
    }

    private void shareFile(@NonNull File file, @NonNull String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mimeType);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.export_share_title)));
    }

    private void showCloudShareCode(@Nullable String code) {
        if (code == null || code.isEmpty() || binding == null) {
            return;
        }
        String message = getString(R.string.share_cloud_code_message, code);
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.share_cloud_code_title)
                .setMessage(message)
                .setPositiveButton(R.string.share_cloud_copy, (d, w) -> {
                    android.content.ClipboardManager clipboard =
                            (android.content.ClipboardManager) requireContext()
                                    .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        clipboard.setPrimaryClip(
                                android.content.ClipData.newPlainText("LectureLens code", code));
                    }
                    Snackbar.make(binding.getRoot(), R.string.share_cloud_copied,
                            Snackbar.LENGTH_SHORT).show();
                })
                .setNeutralButton(R.string.share_option_whatsapp, (d, w) ->
                        shareText(getString(R.string.share_cloud_whatsapp_body, code), true))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showRenameDialog() {
        UiState<LectureDetail> state = viewModel.getUiState().getValue();
        if (!(state instanceof UiState.Success)) {
            return;
        }
        String current = ((UiState.Success<LectureDetail>) state).data.lecture.getTitle();
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(R.string.dialog_lecture_title_hint);
        input.setText(current);
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
                .setTitle(R.string.dialog_rename_lecture_title)
                .setView(container)
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_save, (d, w) -> {
                    CharSequence text = input.getText();
                    viewModel.renameTitle(text != null ? text.toString() : "");
                })
                .show();
        input.requestFocus();
    }

    private void showMoveDialog() {
        List<Course> courses = viewModel.getCoursesSnapshot();
        if (courses.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_move_lecture_title)
                    .setMessage(R.string.move_lecture_no_categories)
                    .setPositiveButton(R.string.pipeline_error_dismiss, null)
                    .show();
            return;
        }
        List<String> labels = new ArrayList<>(courses.size() + 1);
        List<Long> ids = new ArrayList<>(courses.size() + 1);
        labels.add(getString(R.string.library_uncategorized));
        ids.add(LibraryViewModel.UNCATEGORIZED_COURSE_ID);
        for (Course course : courses) {
            labels.add(course.getName());
            ids.add(course.getId());
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_move_lecture_title)
                .setItems(labels.toArray(new String[0]), (d, which) ->
                        viewModel.moveToCourse(ids.get(which), labels.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void startPositionPolling() {
        stopPositionPolling();
        positionTicker = new Runnable() {
            @Override
            public void run() {
                if (playbackController != null && mediaPrepared) {
                    viewModel.onPlaybackPosition(playbackController.getCurrentPosition());
                }
                positionHandler.postDelayed(this, POSITION_POLL_MS);
            }
        };
        positionHandler.post(positionTicker);
    }

    private void stopPositionPolling() {
        if (positionTicker != null) {
            positionHandler.removeCallbacks(positionTicker);
            positionTicker = null;
        }
    }

    @Override
    public void onDestroyView() {
        stopPositionPolling();
        if (playbackController != null) {
            playbackController.release();
            playbackController = null;
        }
        super.onDestroyView();
        binding = null;
    }

    private static final class LecturePagerAdapter extends FragmentStateAdapter {

        LecturePagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new PlayerTabFragment();
                case 1:
                    return new TranscriptTabFragment();
                default:
                    return new NotesTabFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }
}
