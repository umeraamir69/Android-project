package com.lecturelens.ui.lecture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.databinding.ItemNotesRowBinding;
import com.lecturelens.domain.model.Notes;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Track 5 — typed note rows: heading / bullet / key-term chip.
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

        public NotesRow(@NonNull RowType type, @NonNull String text) {
            this.type = type;
            this.text = text;
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
            return type == notesRow.type && text.equals(notesRow.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, text);
        }
    }

    private static final DiffUtil.ItemCallback<NotesRow> DIFF =
            new DiffUtil.ItemCallback<NotesRow>() {
                @Override
                public boolean areItemsTheSame(@NonNull NotesRow oldItem,
                                               @NonNull NotesRow newItem) {
                    return oldItem.equals(newItem);
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
            switch (row.type) {
                case HEADING:
                    binding.textHeading.setVisibility(View.VISIBLE);
                    binding.textHeading.setText(row.text);
                    break;
                case BULLET:
                    binding.textBullet.setVisibility(View.VISIBLE);
                    binding.textBullet.setText("• " + row.text);
                    break;
                case KEY_TERM:
                    binding.chipKeyTerm.setVisibility(View.VISIBLE);
                    binding.chipKeyTerm.setText(row.text);
                    break;
                default:
                    break;
            }
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
        if (!notes.getKeyTerms().isEmpty()) {
            rows.add(new NotesRow(RowType.HEADING, keyTermsLabel));
            for (String term : notes.getKeyTerms()) {
                if (term != null && !term.trim().isEmpty()) {
                    rows.add(new NotesRow(RowType.KEY_TERM, term.trim()));
                }
            }
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
