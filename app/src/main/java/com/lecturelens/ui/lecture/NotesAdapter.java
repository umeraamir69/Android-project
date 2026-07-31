package com.lecturelens.ui.lecture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.lecturelens.databinding.ItemNotesRowBinding;
import com.lecturelens.domain.model.Notes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Typed note rows: heading / bullet / wrapping key-term chip group.
 */
public class NotesAdapter extends ListAdapter<NotesAdapter.NotesRow, NotesAdapter.RowViewHolder> {

    public enum RowType {
        HEADING,
        BULLET,
        KEY_TERM
    }

    public static final class NotesRow {
        @NonNull public final RowType type;
        @NonNull public final String text;
        /** Populated for {@link RowType#KEY_TERM} — all chips in one wrapping row. */
        @NonNull public final List<String> chips;

        public NotesRow(@NonNull RowType type, @NonNull String text) {
            this(type, text, Collections.emptyList());
        }

        public NotesRow(@NonNull RowType type,
                        @NonNull String text,
                        @NonNull List<String> chips) {
            this.type = type;
            this.text = text;
            this.chips = List.copyOf(chips);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NotesRow)) {
                return false;
            }
            NotesRow notesRow = (NotesRow) o;
            return type == notesRow.type
                    && text.equals(notesRow.text)
                    && chips.equals(notesRow.chips);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, text, chips);
        }
    }

    private static final DiffUtil.ItemCallback<NotesRow> DIFF =
            new DiffUtil.ItemCallback<NotesRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotesRow oldItem,
                                               @NonNull NotesRow newItem) {
                    return oldItem.type == newItem.type
                            && oldItem.text.equals(newItem.text);
                }

                @Override
                public boolean areContentsTheSame(@NonNull NotesRow oldItem,
                                                  @NonNull NotesRow newItem) {
                    return oldItem.equals(newItem);
                }
            };

    public NotesAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemNotesRowBinding binding = ItemNotesRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {

        private final ItemNotesRowBinding binding;

        RowViewHolder(@NonNull ItemNotesRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull NotesRow row) {
            binding.textHeading.setVisibility(View.GONE);
            binding.textBullet.setVisibility(View.GONE);
            binding.chipKeyTerm.setVisibility(View.GONE);
            binding.chipGroupKeyTerms.setVisibility(View.GONE);
            switch (row.type) {
                case HEADING:
                    binding.textHeading.setVisibility(View.VISIBLE);
                    binding.textHeading.setText(
                            com.lecturelens.ui.util.MarkdownSpans.fromLiteMarkdown(row.text));
                    break;
                case BULLET:
                    binding.textBullet.setVisibility(View.VISIBLE);
                    binding.textBullet.setText(
                            com.lecturelens.ui.util.MarkdownSpans.fromLiteMarkdown(
                                    "• " + stripLeadingBullet(row.text)));
                    break;
                case KEY_TERM:
                    bindChipGroup(row.chips.isEmpty()
                            ? Collections.singletonList(row.text)
                            : row.chips);
                    break;
                default:
                    break;
            }
        }

        private void bindChipGroup(@NonNull List<String> terms) {
            binding.chipGroupKeyTerms.removeAllViews();
            Context context = binding.getRoot().getContext();
            for (String term : terms) {
                if (term == null || term.trim().isEmpty()) {
                    continue;
                }
                Chip chip = new Chip(context);
                chip.setText(term.trim());
                chip.setClickable(false);
                chip.setFocusable(false);
                chip.setCheckable(false);
                // Default 48dp touch target makes each chip look like a full row.
                chip.setEnsureMinTouchTargetSize(false);
                binding.chipGroupKeyTerms.addView(chip);
            }
            binding.chipGroupKeyTerms.setVisibility(
                    binding.chipGroupKeyTerms.getChildCount() > 0 ? View.VISIBLE : View.GONE);
        }

        @NonNull
        private static String stripLeadingBullet(@NonNull String text) {
            String t = text.trim();
            if (t.startsWith("•")) {
                return t.substring(1).trim();
            }
            if (t.startsWith("- ") || t.startsWith("* ")) {
                return t.substring(2).trim();
            }
            return t;
        }
    }

    /** Flattens domain {@link Notes} into typed rows for the RecyclerView. */
    @NonNull
    @VisibleForTesting
    public static List<NotesRow> fromNotes(@NonNull Notes notes,
                                           @NonNull String summaryLabel,
                                           @NonNull String keyTermsLabel,
                                           @NonNull String actionItemsLabel) {
        List<NotesRow> rows = new ArrayList<>();
        String summary = notes.getSummary().trim();
        if (!summary.isEmpty()) {
            rows.add(new NotesRow(RowType.HEADING, summaryLabel));
            for (String line : summary.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.startsWith("#")) {
                    rows.add(new NotesRow(RowType.HEADING,
                            trimmed.replaceFirst("^#+\\s*", "")));
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    rows.add(new NotesRow(RowType.BULLET, trimmed.substring(2).trim()));
                } else {
                    rows.add(new NotesRow(RowType.BULLET, trimmed));
                }
            }
        }
        List<String> terms = new ArrayList<>();
        for (String term : notes.getKeyTerms()) {
            if (term != null && !term.trim().isEmpty()) {
                terms.add(term.trim());
            }
        }
        if (!terms.isEmpty()) {
            rows.add(new NotesRow(RowType.HEADING, keyTermsLabel));
            rows.add(new NotesRow(RowType.KEY_TERM, "key_terms", terms));
        }
        if (!notes.getActionItems().isEmpty()) {
            rows.add(new NotesRow(RowType.HEADING, actionItemsLabel));
            for (String item : notes.getActionItems()) {
                if (item != null && !item.trim().isEmpty()) {
                    rows.add(new NotesRow(RowType.BULLET, item.trim()));
                }
            }
        }
        return rows;
    }
}
