package com.lecturelens.ui.lecture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.exoplayer.ExoPlayer;

import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.core.player.AudioPlaybackController;
import com.lecturelens.databinding.FragmentPlayerTabBinding;
import com.lecturelens.domain.model.Lecture;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

/** Track 5 — ExoPlayer tab; binds the shared {@link AudioPlaybackController}. */
@AndroidEntryPoint
public class PlayerTabFragment extends Fragment {

    @Nullable private FragmentPlayerTabBinding binding;
    private LectureViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlayerTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(LectureViewModel.class);
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        bindPlayer();
    }

    @Override
    public void onResume() {
        super.onResume();
        bindPlayer();
    }

    private void bindPlayer() {
        if (binding == null) {
            return;
        }
        AudioPlaybackController controller = playerHost();
        if (controller == null) {
            return;
        }
        ExoPlayer player = controller.getPlayer();
        binding.playerView.setPlayer(player);
    }

    private void render(@NonNull UiState<LectureDetail> state) {
        if (binding == null) {
            return;
        }
        if (!(state instanceof UiState.Success)) {
            return;
        }
        LectureDetail detail = ((UiState.Success<LectureDetail>) state).data;
        Lecture lecture = detail.lecture;
        binding.textPlayerTitle.setText(lecture.getTitle());
        binding.textPlayerMeta.setText(
                lecture.getStatus().name() + " · " + formatDuration(lecture.getDurationMs()));

        boolean hasAudio = detail.hasAudioPath();
        binding.playerContainer.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
        binding.textNoAudio.setVisibility(hasAudio ? View.GONE : View.VISIBLE);
        if (!hasAudio) {
            binding.textNoAudio.setText(R.string.lecture_no_audio);
        }
        bindPlayer();
    }

    @Nullable
    private AudioPlaybackController playerHost() {
        Fragment parent = getParentFragment();
        if (parent instanceof LectureViewFragment) {
            return ((LectureViewFragment) parent).getPlaybackController();
        }
        return null;
    }

    @NonNull
    private static String formatDuration(long durationMs) {
        long minutes = Math.max(1L, TimeUnit.MILLISECONDS.toMinutes(durationMs));
        return minutes + " min";
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.playerView.setPlayer(null);
        }
        super.onDestroyView();
        binding = null;
    }
}
