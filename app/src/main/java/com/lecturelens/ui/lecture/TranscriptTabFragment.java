package com.lecturelens.ui.lecture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.core.UiState;
import com.lecturelens.core.player.AudioPlaybackController;
import com.lecturelens.databinding.FragmentTranscriptTabBinding;
import com.lecturelens.domain.model.TranscriptSegment;

import dagger.hilt.android.AndroidEntryPoint;

/** Track 5 — transcript list with active-segment highlight + tap-to-seek. */
@AndroidEntryPoint
public class TranscriptTabFragment extends Fragment implements TranscriptAdapter.Listener {

    @Nullable private FragmentTranscriptTabBinding binding;
    private LectureViewModel viewModel;
    private TranscriptAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTranscriptTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(LectureViewModel.class);

        adapter = new TranscriptAdapter(this);
        binding.recyclerTranscript.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTranscript.setAdapter(adapter);

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getActiveSegmentIndex().observe(getViewLifecycleOwner(), this::onActiveIndex);
    }

    private void render(@NonNull UiState<LectureDetail> state) {
        if (binding == null) {
            return;
        }
        if (!(state instanceof UiState.Success)) {
            return;
        }
        LectureDetail detail = ((UiState.Success<LectureDetail>) state).data;
        adapter.submitList(detail.segments);
        boolean empty = detail.segments.isEmpty();
        binding.textTranscriptEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerTranscript.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void onActiveIndex(@Nullable Integer index) {
        if (binding == null || index == null) {
            return;
        }
        adapter.setActiveIndex(index);
        if (index >= 0 && index < adapter.getItemCount()) {
            RecyclerView.LayoutManager lm = binding.recyclerTranscript.getLayoutManager();
            if (lm instanceof LinearLayoutManager) {
                LinearSmoothScroller scroller = new LinearSmoothScroller(requireContext()) {
                    @Override
                    protected int getVerticalSnapPreference() {
                        return SNAP_TO_START;
                    }
                };
                scroller.setTargetPosition(index);
                lm.startSmoothScroll(scroller);
            }
        }
    }

    @Override
    public void onSegmentClicked(@NonNull TranscriptSegment segment) {
        viewModel.onSegmentTapped(segment.getStartMs());
        AudioPlaybackController controller = playerHost();
        if (controller != null) {
            controller.seekTo(segment.getStartMs());
            if (controller.getPlayer() != null && !controller.isPlaying()) {
                controller.getPlayer().play();
            }
        }
    }

    @Nullable
    private AudioPlaybackController playerHost() {
        Fragment parent = getParentFragment();
        if (parent instanceof LectureViewFragment) {
            return ((LectureViewFragment) parent).getPlaybackController();
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
