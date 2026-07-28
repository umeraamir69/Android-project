package com.lecturelens.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

/**
 * Track 5 — FTS4 search UI. Tapping a hit navigates to the lecture at
 * {@code seekMs}.
 */
@AndroidEntryPoint
public class SearchFragment extends Fragment implements SearchResultsAdapter.Listener {

    @Nullable private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private SearchResultsAdapter adapter;

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

        // Search is a bottom-nav tab — no up affordance; use tabs to leave.
        binding.toolbar.setNavigationIcon(null);

        binding.inputQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.onQueryChanged(s != null ? s.toString() : "");
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
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
        args.putLong("seekMs", hit.startMs);
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
