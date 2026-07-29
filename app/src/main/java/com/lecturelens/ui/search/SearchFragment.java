package com.lecturelens.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.databinding.FragmentSearchBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SearchFragment extends Fragment implements SearchResultsAdapter.Listener {

    @Nullable private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private SearchResultsAdapter adapter;
    @Nullable private ArrayAdapter<String> suggestAdapter;
    private boolean applyingSuggestion;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        adapter = new SearchResultsAdapter(this);
        binding.recyclerResults.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerResults.setAdapter(adapter);
        binding.toolbar.setNavigationIcon(null);

        suggestAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line);
        binding.inputQuery.setAdapter(suggestAdapter);
        binding.inputQuery.setThreshold(2);

        binding.inputQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (applyingSuggestion) {
                    return;
                }
                viewModel.onQueryChanged(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        binding.inputQuery.setOnItemClickListener((parent, v, position, id) -> {
            Object item = parent.getItemAtPosition(position);
            if (item != null) {
                applyingSuggestion = true;
                binding.inputQuery.setText(item.toString());
                binding.inputQuery.setSelection(item.toString().length());
                applyingSuggestion = false;
                viewModel.onQueryChanged(item.toString());
            }
        });

        binding.chipFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                viewModel.setFilter("ALL");
                return;
            }
            int id = checkedIds.get(0);
            if (id == R.id.chip_transcript) {
                viewModel.setFilter("TRANSCRIPT");
            } else if (id == R.id.chip_notes) {
                viewModel.setFilter("NOTES");
            } else if (id == R.id.chip_chat) {
                viewModel.setFilter("CHAT");
            } else {
                viewModel.setFilter("ALL");
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getSuggestions().observe(getViewLifecycleOwner(), list -> {
            if (suggestAdapter == null || list == null) {
                return;
            }
            suggestAdapter.clear();
            suggestAdapter.addAll(list);
            suggestAdapter.notifyDataSetChanged();
            if (!list.isEmpty()
                    && binding != null
                    && binding.inputQuery.hasFocus()
                    && binding.inputQuery.getText() != null
                    && binding.inputQuery.getText().length() >= 2) {
                binding.inputQuery.showDropDown();
            }
        });
    }

    private void render(@NonNull UiState<List<SearchResultsAdapter.ListItem>> state) {
        if (binding == null) {
            return;
        }
        boolean loading = state instanceof UiState.Loading;
        binding.progressSearch.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (state instanceof UiState.Success) {
            List<SearchResultsAdapter.ListItem> items =
                    ((UiState.Success<List<SearchResultsAdapter.ListItem>>) state).data;
            adapter.submitList(items);
            boolean empty = items.isEmpty();
            binding.emptyState.getRoot().setVisibility(empty ? View.VISIBLE : View.GONE);
            String query = binding.inputQuery.getText() != null
                    ? binding.inputQuery.getText().toString().trim()
                    : "";
            if (empty) {
                binding.emptyState.imageEmpty.setImageResource(R.drawable.ill_empty_search);
                binding.emptyState.textEmptyTitle.setVisibility(View.VISIBLE);
                binding.emptyState.buttonEmptyCta.setVisibility(View.GONE);
                if (query.isEmpty()) {
                    binding.emptyState.textEmptyTitle.setText(R.string.search_prompt_title);
                    binding.emptyState.textEmptyMessage.setText(R.string.search_prompt);
                } else {
                    binding.emptyState.textEmptyTitle.setText(R.string.search_empty_title);
                    binding.emptyState.textEmptyMessage.setText(R.string.search_empty);
                }
            }
        } else if (state instanceof UiState.Error) {
            Snackbar.make(binding.getRoot(),
                    ((UiState.Error<List<SearchResultsAdapter.ListItem>>) state).message,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    @Override
    public void onHitClicked(@NonNull SearchHit hit) {
        Bundle args = new Bundle();
        args.putLong("lectureId", hit.lectureId);
        long seek = hit.startMs >= 0 && SearchHit.SOURCE_TRANSCRIPT.equals(hit.sourceType)
                ? hit.startMs
                : -1L;
        args.putLong("seekMs", seek);
        nav().navigate(R.id.action_search_to_lecture, args);
    }

    @NonNull
    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
