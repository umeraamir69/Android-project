package com.lecturelens.ui.lecture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.core.player.AudioPlaybackController;
import com.lecturelens.databinding.FragmentLectureViewBinding;

import java.io.File;

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

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_export) {
                viewModel.export();
                return true;
            }
            return false;
        });

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
        viewModel.getExportFile().observe(getViewLifecycleOwner(), this::shareExport);
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

    private void render(@NonNull UiState<LectureDetail> state) {
        if (binding == null) {
            return;
        }
        boolean loading = state instanceof UiState.Loading;
        binding.progressLecture.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            LectureDetail detail = ((UiState.Success<LectureDetail>) state).data;
            binding.toolbar.setTitle(detail.lecture.getTitle());
            if (detail.hasAudioPath()) {
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

    private void shareExport(@Nullable File file) {
        if (file == null || binding == null) {
            return;
        }
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/markdown");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, getString(R.string.export_share_title)));
        Snackbar.make(binding.getRoot(), R.string.export_success, Snackbar.LENGTH_SHORT).show();
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
