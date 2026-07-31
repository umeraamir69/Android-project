package com.lecturelens.ui.lecture;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.core.VectorMath;
import com.lecturelens.databinding.FragmentNotesTabBinding;
import com.lecturelens.domain.model.ChatMessage;
import com.lecturelens.domain.model.Handout;
import com.lecturelens.domain.model.Notes;
import com.lecturelens.domain.model.RagCitation;

import java.io.File;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Notes tab: study notes + handouts media + full Ask AI chat. */
@AndroidEntryPoint
public class NotesTabFragment extends Fragment implements HandoutsAdapter.Listener {

    @Nullable private FragmentNotesTabBinding binding;
    private LectureViewModel viewModel;
    private NotesAdapter notesAdapter;
    private HandoutsAdapter handoutsAdapter;
    private ChatMessagesAdapter chatAdapter;

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

        notesAdapter = new NotesAdapter();
        binding.recyclerNotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerNotes.setAdapter(notesAdapter);

        handoutsAdapter = new HandoutsAdapter(this);
        binding.recyclerHandouts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerHandouts.setAdapter(handoutsAdapter);

        chatAdapter = new ChatMessagesAdapter();
        binding.recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChat.setAdapter(chatAdapter);

        // Lecture screen hides bottom nav; lift Ask bar above gesture / nav inset.
        final int padH = binding.askBar.getPaddingLeft();
        final int padTop = binding.askBar.getPaddingTop();
        final int padBottomBase = binding.askBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(binding.askBar, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(padH, padTop, padH, padBottomBase + bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(binding.askBar);

        com.lecturelens.ui.util.SectionFeedback.toast(this,
                getString(R.string.toast_section_ready, getString(R.string.lecture_tab_notes)));
        binding.buttonAsk.setOnClickListener(v -> submitQuestion());
        binding.buttonClearChat.setOnClickListener(v ->
                new MaterialAlertDialogBuilder(requireContext())
                        .setMessage(R.string.notes_clear_chat_confirm)
                        .setNegativeButton(R.string.action_cancel, null)
                        .setPositiveButton(R.string.notes_clear_chat, (d, w) ->
                                viewModel.clearChat())
                        .show());
        binding.editAsk.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                submitQuestion();
                return true;
            }
            return false;
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getAiCitations().observe(getViewLifecycleOwner(), this::renderCitations);
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
        viewModel.getChatMessages().observe(getViewLifecycleOwner(), this::renderChat);
    }

    private void renderCitations(@Nullable List<RagCitation> citations) {
        if (binding == null) {
            return;
        }
        binding.chipCitations.removeAllViews();
        if (citations == null || citations.isEmpty()) {
            binding.chipCitations.setVisibility(View.GONE);
            return;
        }
        binding.chipCitations.setVisibility(View.VISIBLE);
        int i = 1;
        for (RagCitation c : citations) {
            Chip chip = new Chip(requireContext());
            chip.setText("[" + i + "] " + VectorMath.formatTimestamp(c.startMs));
            chip.setClickable(true);
            chip.setCheckable(false);
            final long seek = c.startMs;
            chip.setOnClickListener(v -> viewModel.onCitationTapped(seek));
            binding.chipCitations.addView(chip);
            i++;
        }
    }

    private void submitQuestion() {
        if (binding == null) {
            return;
        }
        CharSequence q = binding.editAsk.getText();
        String question = q != null ? q.toString().trim() : "";
        if (question.isEmpty()) {
            return;
        }
        binding.editAsk.setText("");
        viewModel.askAboutNotes(question);
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
        notesAdapter.submitList(rows);
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
        boolean has = handouts != null && !handouts.isEmpty();
        binding.textHandoutsHeader.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.recyclerHandouts.setVisibility(has ? View.VISIBLE : View.GONE);
        handoutsAdapter.submitList(handouts != null ? handouts : Collections.emptyList());
    }

    private void renderChat(@Nullable List<ChatMessage> messages) {
        if (binding == null) {
            return;
        }
        List<ChatMessage> list = messages != null ? messages : Collections.emptyList();
        chatAdapter.submitList(list);
        boolean empty = list.isEmpty();
        binding.textChatEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.buttonClearChat.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (!empty) {
            binding.scrollNotes.post(() ->
                    binding.scrollNotes.fullScroll(View.FOCUS_DOWN));
        }
    }

    @Override
    public void onOpen(@NonNull Handout handout) {
        File file = new File(handout.localPath);
        if (!file.exists()) {
            if (handout.remoteUrl != null && !handout.remoteUrl.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(handout.remoteUrl)));
                return;
            }
            Snackbar.make(requireView(), R.string.handout_missing_file, Snackbar.LENGTH_LONG).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri,
                    handout.mimeType.isEmpty() ? "*/*" : handout.mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, getString(R.string.action_open_handout)));
        } catch (Exception e) {
            Snackbar.make(requireView(), R.string.handout_pick_failed, Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDelete(@NonNull Handout handout) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(getString(R.string.handout_delete_confirm, handout.displayName))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_delete, (d, w) ->
                        viewModel.deleteHandout(handout.id))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
