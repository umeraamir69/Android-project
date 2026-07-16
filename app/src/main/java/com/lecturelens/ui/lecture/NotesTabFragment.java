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

import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentNotesTabBinding;
import com.lecturelens.domain.model.Notes;

import java.util.Collections;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Track 5 — notes as typed RecyclerView rows (heading / bullet / key-term). */
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
        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
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
        binding.textNotesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerNotes.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
