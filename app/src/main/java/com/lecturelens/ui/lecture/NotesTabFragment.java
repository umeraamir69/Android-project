package com.lecturelens.ui.lecture;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentNotesTabBinding;
import com.lecturelens.domain.model.Handout;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.ui.util.MarkdownSpans;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Notes tab: study notes + Ask AI (grounded) + handout OCR snippets. */
@AndroidEntryPoint
public class NotesTabFragment extends Fragment {

    @Nullable private FragmentNotesTabBinding binding;
    private LectureViewModel viewModel;
    private NotesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotesTabBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireParentFragment()).get(LectureViewModel.class);
        adapter = new NotesAdapter();
        binding.recyclerNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerNotes.setAdapter(adapter);

        binding.buttonAsk.setOnClickListener(v -> submitQuestion());
        binding.editAsk.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitQuestion();
                return true;
            }
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getAiAnswer().observe(getViewLifecycleOwner(), answer -> {
            if (binding == null || answer == null) {
                return;
            }
            binding.cardAnswer.setVisibility(View.VISIBLE);
            binding.textAiAnswer.setText(MarkdownSpans.fromLiteMarkdown(answer));
        });
        viewModel.getAiLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progressAsk.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.buttonAsk.setEnabled(!busy);
            binding.editAsk.setEnabled(!busy);
        });
        viewModel.getHandouts().observe(getViewLifecycleOwner(), this::renderHandouts);
    }

    private void submitQuestion() {
        if (binding == null) {
            return;
        }
        CharSequence q = binding.editAsk.getText();
        viewModel.askAboutNotes(q != null ? q.toString() : "");
    }

    private void render(@NonNull UiState<LectureDetail> state) {
        if (binding == null) {
            return;
        }
        if (!(state instanceof UiState.Success)) {
            return;
        }
        LectureDetail detail = ((UiState.Success<LectureDetail>) state).data;
        Notes notes = detail.notes;
        List<NotesAdapter.NotesRow> rows = notes == null
                ? Collections.emptyList()
                : NotesAdapter.fromNotes(
                notes,
                getString(R.string.lecture_notes_summary),
                getString(R.string.lecture_notes_key_terms),
                getString(R.string.lecture_notes_action_items));
        adapter.submitList(rows);
        boolean empty = rows.isEmpty();
        binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            binding.emptyState.imageEmpty.setImageResource(R.drawable.ill_empty_notes);
            binding.emptyState.textEmptyTitle.setVisibility(View.VISIBLE);
            binding.emptyState.textEmptyTitle.setText(R.string.lecture_notes_empty_title);
            binding.emptyState.textEmptyMessage.setText(R.string.lecture_notes_empty);
            binding.emptyState.buttonEmptyCta.setVisibility(View.GONE);
        }
        binding.recyclerNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderHandouts(@Nullable List<Handout> handouts) {
        if (binding == null) {
            return;
        }
        if (handouts == null || handouts.isEmpty()) {
            binding.textHandoutsHeader.setVisibility(View.GONE);
            binding.textHandouts.setVisibility(View.GONE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Handout h : handouts) {
            sb.append(i++).append(". ");
            String snippet = h.extractedText.trim();
            if (snippet.length() > 280) {
                snippet = snippet.substring(0, 280) + "…";
            }
            sb.append(snippet).append("\n\n");
        }
        binding.textHandoutsHeader.setVisibility(View.VISIBLE);
        binding.textHandouts.setVisibility(View.VISIBLE);
        binding.textHandouts.setText(MarkdownSpans.fromLiteMarkdown(sb.toString().trim()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
