package com.lecturelens.ui.home;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.lecturelens.R;
import com.lecturelens.core.UiState;
import com.lecturelens.databinding.FragmentHomeBinding;
import com.lecturelens.databinding.ItemHomeShortcutBinding;
import com.lecturelens.ui.util.AppNavigator;
import com.lecturelens.ui.util.HelpDialogs;
import com.lecturelens.ui.util.ListViewHeight;
import com.lecturelens.ui.util.SectionFeedback;
import com.lecturelens.ui.util.UiAnimations;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private static final String SHARE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Nullable private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private RecentLecturesListAdapter recentAdapter;

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

        UiAnimations.animateScreenEnter(binding.getRoot());
        bindShortcuts();
        recentAdapter = new RecentLecturesListAdapter(id ->
                AppNavigator.openLecture(this, id, -1L));
        binding.listRecent.setAdapter(recentAdapter);

        binding.buttonHelp.setOnClickListener(v ->
                HelpDialogs.show(this, getString(R.string.title_home)));
        binding.buttonSettings.setOnClickListener(v ->
                AppNavigator.openSettingsActivity(this));
        binding.buttonSeeAll.setOnClickListener(v ->
                AppNavigator.openLibraryActivity(this));
        binding.buttonEmptyRecord.setOnClickListener(v ->
                AppNavigator.openUploadActivity(this));
        UiAnimations.bindPressScale(binding.buttonSeeAll);
        UiAnimations.bindPressScale(binding.buttonEmptyRecord);
        UiAnimations.staggerChildren(binding.shortcutGrid);

        SectionFeedback.toast(this, getString(R.string.toast_section_ready,
                getString(R.string.title_home)));

        viewModel.getUiState().observe(getViewLifecycleOwner(), this::render);
        viewModel.getImportError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && binding != null) {
                Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_LONG).show();
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
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
                    .setAction(R.string.action_open, v ->
                            AppNavigator.openLecture(this, event.lectureId, -1L))
                    .show();
            viewModel.consumeImportEvent();
        });
        viewModel.getImportLoading().observe(getViewLifecycleOwner(), loading -> {
            if (binding == null) {
                return;
            }
            boolean busy = Boolean.TRUE.equals(loading);
            binding.progressHome.setVisibility(busy ? View.VISIBLE : View.GONE);
            binding.shortcutShared.getRoot().setEnabled(!busy);
        });
    }

    private void bindShortcuts() {
        setupShortcut(binding.shortcutRecord, R.drawable.ic_mic_24, R.string.action_record,
                v -> AppNavigator.openUploadActivity(this));
        setupShortcut(binding.shortcutLibrary, R.drawable.ic_library_24, R.string.title_library,
                v -> AppNavigator.openLibraryActivity(this));
        setupShortcut(binding.shortcutSearch, R.drawable.ic_search_24, R.string.action_search,
                v -> AppNavigator.openSearchActivity(this));
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
        UiAnimations.bindPressScale(shortcut.getRoot());
    }

    private void showOpenSharedDialog() {
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        input.setHint(R.string.settings_share_code_hint);
        input.setFilters(new InputFilter[]{
                new InputFilter.AllCaps(),
                new InputFilter.LengthFilter(com.lecturelens.domain.util.ShareCodes.LENGTH),
                (source, start, end, dest, dstart, dend) -> {
                    StringBuilder kept = new StringBuilder();
                    for (int i = start; i < end; i++) {
                        char c = Character.toUpperCase(source.charAt(i));
                        if (SHARE_CODE_ALPHABET.indexOf(c) >= 0) {
                            kept.append(c);
                        }
                    }
                    String filtered = kept.toString();
                    String original = source.subSequence(start, end).toString();
                    if (filtered.equalsIgnoreCase(original)) {
                        return null;
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
        if (binding == null) {
            return;
        }
        if (state instanceof UiState.Loading) {
            binding.progressHome.setVisibility(View.VISIBLE);
            return;
        }
        binding.progressHome.setVisibility(View.GONE);
        if (!(state instanceof UiState.Success)) {
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
        binding.listRecent.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.buttonSeeAll.setVisibility(empty ? View.GONE : View.VISIBLE);
        recentAdapter.submit(dash.recentLectures);
        if (!empty) {
            binding.listRecent.post(() -> {
                ListViewHeight.expand(binding.listRecent);
                UiAnimations.playListLayoutAnimation(binding.listRecent);
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
