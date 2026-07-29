package com.lecturelens.ui.lecture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.core.player.AudioPlaybackController;
import com.lecturelens.databinding.FragmentPlayerTabBinding;
import com.lecturelens.databinding.ViewPipelineTimelineBinding;
import com.lecturelens.domain.model.Lecture;
import com.lecturelens.domain.model.LectureStatus;

import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;

/** Track 5 — ExoPlayer tab; binds the shared {@link AudioPlaybackController}. */
@AndroidEntryPoint
public class PlayerTabFragment extends Fragment {

    private enum StepUi { PENDING, ACTIVE, DONE, FAILED }

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
        requireBinding().buttonRetranscribe.setOnClickListener(v -> viewModel.retranscribe());
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
        LectureStatus status = lecture.getStatus();
        binding.textPlayerTitle.setText(lecture.getTitle());
        binding.textPlayerMeta.setText(
                status.name() + " · " + formatDuration(lecture.getDurationMs()));

        boolean processing = isPipelineBusy(status);
        boolean showTimeline = processing || status == LectureStatus.READY || status == LectureStatus.FAILED;
        ViewPipelineTimelineBinding timeline = binding.pipelineTimelineInclude;
        timeline.getRoot().setVisibility(showTimeline ? View.VISIBLE : View.GONE);
        if (showTimeline) {
            bindTimeline(timeline, status, !detail.segments.isEmpty());
        }

        if (processing) {
            binding.textPlayerError.setVisibility(View.GONE);
        } else if (detail.pipelineError != null && !detail.pipelineError.isEmpty()) {
            // Short banner line only — full raw API bodies are never shown here.
            binding.textPlayerError.setVisibility(View.VISIBLE);
            binding.textPlayerError.setText(detail.pipelineError);
            binding.textPlayerError.setMaxLines(3);
        } else {
            binding.textPlayerError.setVisibility(View.GONE);
        }

        boolean hasAudio = detail.hasPlayableAudio();
        boolean showRetry = hasAudio && !processing
                && (status == LectureStatus.FAILED || status == LectureStatus.RECORDED);
        binding.buttonRetranscribe.setVisibility(showRetry ? View.VISIBLE : View.GONE);
        binding.buttonRetranscribe.setEnabled(!processing);
        if (showRetry && !detail.segments.isEmpty()) {
            binding.buttonRetranscribe.setText(R.string.action_retry_notes);
        } else if (showRetry) {
            binding.buttonRetranscribe.setText(R.string.action_retranscribe);
        }

        binding.playerContainer.setVisibility(hasAudio ? View.VISIBLE : View.GONE);
        binding.textNoAudio.setVisibility(hasAudio ? View.GONE : View.VISIBLE);
        if (!hasAudio) {
            if (status == LectureStatus.SHARED) {
                binding.textNoAudio.setText(R.string.lecture_shared_no_audio);
            } else {
                binding.textNoAudio.setText(detail.hasAudioPath()
                        ? R.string.lecture_audio_missing
                        : R.string.lecture_no_audio);
            }
        }
        bindPlayer();
    }

    private void bindTimeline(@NonNull ViewPipelineTimelineBinding timeline,
                              @NonNull LectureStatus status,
                              boolean hasTranscript) {
        StepUi transcribe;
        StepUi summarize;
        StepUi ready;
        switch (status) {
            case TRANSCRIBING:
                transcribe = StepUi.ACTIVE;
                summarize = StepUi.PENDING;
                ready = StepUi.PENDING;
                break;
            case TRANSCRIBED:
                transcribe = StepUi.DONE;
                summarize = StepUi.PENDING;
                ready = StepUi.PENDING;
                break;
            case SUMMARIZING:
            case INDEXING:
                transcribe = StepUi.DONE;
                summarize = StepUi.ACTIVE;
                ready = StepUi.PENDING;
                break;
            case READY:
                transcribe = StepUi.DONE;
                summarize = StepUi.DONE;
                ready = StepUi.DONE;
                break;
            case FAILED:
                if (hasTranscript) {
                    transcribe = StepUi.DONE;
                    summarize = StepUi.FAILED;
                } else {
                    transcribe = StepUi.FAILED;
                    summarize = StepUi.PENDING;
                }
                ready = StepUi.PENDING;
                break;
            default:
                transcribe = StepUi.PENDING;
                summarize = StepUi.PENDING;
                ready = StepUi.PENDING;
                break;
        }

        applyStep(timeline.stepTranscribeSpinner, timeline.stepTranscribeCheck,
                timeline.stepTranscribeDot, timeline.stepTranscribeLabel,
                R.string.pipeline_step_transcribe, transcribe);
        applyStep(timeline.stepSummarizeSpinner, timeline.stepSummarizeCheck,
                timeline.stepSummarizeDot, timeline.stepSummarizeLabel,
                R.string.pipeline_step_summarize, summarize);
        applyStep(timeline.stepReadySpinner, timeline.stepReadyCheck,
                timeline.stepReadyDot, timeline.stepReadyLabel,
                R.string.pipeline_step_ready, ready);
    }

    private void applyStep(@NonNull CircularProgressIndicator spinner,
                           @NonNull ImageView check,
                           @NonNull View dot,
                           @NonNull TextView label,
                           int labelRes,
                           @NonNull StepUi ui) {
        spinner.setVisibility(ui == StepUi.ACTIVE ? View.VISIBLE : View.GONE);
        check.setVisibility(ui == StepUi.DONE ? View.VISIBLE : View.GONE);
        dot.setVisibility(ui == StepUi.PENDING || ui == StepUi.FAILED ? View.VISIBLE : View.GONE);
        if (ui == StepUi.FAILED) {
            label.setText(getString(labelRes) + " — " + getString(R.string.pipeline_step_failed));
            label.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
        } else {
            label.setText(labelRes);
            int colorAttr = ui == StepUi.ACTIVE || ui == StepUi.DONE
                    ? com.google.android.material.R.attr.colorOnSurface
                    : com.google.android.material.R.attr.colorOnSurfaceVariant;
            android.util.TypedValue tv = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(colorAttr, tv, true);
            label.setTextColor(tv.data);
        }
    }

    private static boolean isPipelineBusy(@NonNull LectureStatus status) {
        return status == LectureStatus.TRANSCRIBING
                || status == LectureStatus.TRANSCRIBED
                || status == LectureStatus.SUMMARIZING
                || status == LectureStatus.INDEXING;
    }

    @NonNull
    private FragmentPlayerTabBinding requireBinding() {
        FragmentPlayerTabBinding b = binding;
        if (b == null) {
            throw new IllegalStateException("binding accessed outside view lifecycle");
        }
        return b;
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
        if (durationMs < 0L) {
            durationMs = 0L;
        }
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(durationMs);
        if (totalSeconds < 60L) {
            return totalSeconds + " sec";
        }
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (seconds == 0L) {
            return minutes + " min";
        }
        return minutes + " min " + seconds + " sec";
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
