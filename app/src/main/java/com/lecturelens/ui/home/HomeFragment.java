package com.lecturelens.ui.home;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentHomeBinding;
import com.lecturelens.databinding.ItemHomeShortcutBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Post-login landing: stats, shortcuts, and recent lectures.
 */
@AndroidEntryPoint
public class HomeFragment extends Fragment {

    /** Matches FirestoreCloudShareRepository share-code alphabet (6 chars). */
    private static final String SHARE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Nullable private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private RecentLecturesAdapter recentAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        bindShortcuts();
        recentAdapter = new RecentLecturesAdapter(this::openLecture);
        binding.recyclerRecent.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerRecent.setAdapter(recentAdapter);
        binding.recyclerRecent.setNestedScrollingEnabled(false);

        binding.buttonSettings.setOnClickListener(v ->
                nav().navigate(R.id.action_home_to_settings));
        binding.buttonSeeAll.setOnClickListener(v -> goToLibrary());
        binding.buttonEmptyRecord.setOnClickListener(v ->
                nav().navigate(R.id.action_home_to_upload));

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getImportError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && binding != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                viewModel.consumeImportError();
            }
        });
        viewModel.getImportEvent().observe(getViewLifecycleOwner(), event -> {
            if (event == null || binding == null) {
                return;
            }
            Snackbar.make(binding.getRoot(),
                            getString(R.string.share_import_saved, event.title),
                            Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_open, v -> openLecture(event.lectureId))
                    .show();
            viewModel.consumeImportEvent();
        });
        viewModel.getImportLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            binding.shortcutShared.getRoot().setEnabled(!Boolean.TRUE.equals(loading));
        });
    }

    private void bindShortcuts() {
        setupShortcut(binding.shortcutRecord, R.drawable.ic_mic_24, R.string.action_record,
                v -> nav().navigate(R.id.action_home_to_upload));
        setupShortcut(binding.shortcutLibrary, R.drawable.ic_library_24, R.string.title_library,
                v -> goToLibrary());
        setupShortcut(binding.shortcutSearch, R.drawable.ic_search_24, R.string.action_search,
                v -> goToSearch());
        setupShortcut(binding.shortcutShared, R.drawable.ic_folder_shared_24,
                R.string.home_open_shared,
                v -> showOpenSharedDialog());
    }

    private void setupShortcut(@NonNull ItemHomeShortcutBinding shortcut,
                               int iconRes,
                               int titleRes,
                               @NonNull View.OnClickListener click) {
        shortcut.imageShortcut.setImageResource(iconRes);
        shortcut.textShortcut.setText(titleRes);
        shortcut.getRoot().setOnClickListener(click);
    }

    private void showOpenSharedDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(R.string.settings_share_code_hint);
        input.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(6),
                (source, start, end, dest, dstart, dend) -> {
                    StringBuilder kept = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        char c = Character.toUpperCase(source.charAt(i));
                        if (SHARE_CODE_ALPHABET.indexOf(c) >= 0) {
                            kept.append(c);
                        }
                    }
                    // Reject / rewrite disallowed characters.
                    String filtered = kept.toString();
                    String original = source.subSequence(start, end).toString();
                    if (filtered.equalsIgnoreCase(original)) {
                        return null; // keep as-is (AllCaps handles case)
                    }
                    return filtered;
                }
        });
        input.setMaxLines(1);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        FrameLayout container = new FrameLayout(requireContext());
        container.setPadding(pad, pad / 2, pad, 0);
        container.addView(input);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.home_open_shared)
                .setMessage(R.string.home_open_shared_message)
                .setView(container)
                .setPositiveButton(R.string.settings_share_open, (d, w) -> {
                    CharSequence code = input.getText();
                    viewModel.importSharedNotes(code != null ? code.toString() : null);
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
        input.requestFocus();
    }

    private void render(@NonNull UiState<HomeDashboard> state) {
        if (binding == null || !(state instanceof UiState.Success)) {
            return;
        }
        HomeDashboard dash = ((UiState.Success<HomeDashboard>) state).data;
        binding.textGreeting.setText(R.string.home_greeting);
        if (dash.email.isEmpty()) {
            binding.textGreetingDetail.setText(R.string.home_subtitle);
        } else {
            binding.textGreetingDetail.setText(dash.email);
        }
        binding.textStatLectures.setText(String.valueOf(dash.lectureCount));
        binding.textStatCategories.setText(String.valueOf(dash.categoryCount));
        binding.textStatReady.setText(String.valueOf(dash.readyCount));

        if (dash.processingCount > 0) {
            binding.textProcessingBanner.setVisibility(View.VISIBLE);
            binding.textProcessingBanner.setText(getResources().getQuantityString(
                    R.plurals.home_processing_banner, dash.processingCount, dash.processingCount));
        } else if (dash.failedCount > 0) {
            binding.textProcessingBanner.setVisibility(View.VISIBLE);
            binding.textProcessingBanner.setText(getResources().getQuantityString(
                    R.plurals.home_failed_banner, dash.failedCount, dash.failedCount));
        } else {
            binding.textProcessingBanner.setVisibility(View.GONE);
        }

        boolean empty = dash.recentLectures.isEmpty();
        binding.emptyRecent.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerRecent.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.buttonSeeAll.setVisibility(empty ? View.GONE : View.VISIBLE);
        recentAdapter.submitList(dash.recentLectures);
    }

    private void openLecture(long lectureId) {
        Bundle args = new Bundle();
        args.putLong("lectureId", lectureId);
        nav().navigate(R.id.action_home_to_lecture, args);
    }

    private void goToLibrary() {
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.home, false, true)
                .build();
        nav().navigate(R.id.library, null, options);
    }

    private void goToSearch() {
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(R.id.home, false, true)
                .build();
        nav().navigate(R.id.search, null, options);
    }

    private NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
