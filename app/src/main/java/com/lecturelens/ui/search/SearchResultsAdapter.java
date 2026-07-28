package com.lecturelens.ui.search;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.lecturelens.R;
import com.lecturelens.data.local.SearchHit;
import com.lecturelens.databinding.ItemSearchHeaderBinding;
import com.lecturelens.databinding.ItemSearchHitBinding;
import com.lecturelens.ui.lecture.TranscriptAdapter;

import java.util.Objects;

/**
 * Search results grouped by lecture, with icons by source type.
 */
public class SearchResultsAdapter
        extends ListAdapter<SearchResultsAdapter.ListItem, RecyclerView.ViewHolder> {

    public interface Listener {
        void onHitClicked(@NonNull SearchHit hit);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_HIT = 1;

    public abstract static class ListItem {
        abstract int viewType();

        @Override
        public abstract boolean equals(Object o);

        @Override
        public abstract int hashCode();
    }

    public static final class HeaderItem extends ListItem {
        public final long lectureId;
        @NonNull public final String title;

        public HeaderItem(long lectureId, @NonNull String title) {
            this.lectureId = lectureId;
            this.title = title;
        }

        @Override
        int viewType() {
            return TYPE_HEADER;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HeaderItem)) {
                return false;
            }
            HeaderItem that = (HeaderItem) o;
            return lectureId == that.lectureId && title.equals(that.title);
        }

        @Override
        public int hashCode() {
            return Objects.hash(lectureId, title);
        }
    }

    public static final class HitItem extends ListItem {
        @NonNull public final SearchHit hit;

        public HitItem(@NonNull SearchHit hit) {
            this.hit = hit;
        }

        @Override
        int viewType() {
            return TYPE_HIT;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof HitItem)) {
                return false;
            }
            HitItem that = (HitItem) o;
            return hit.lectureId == that.hit.lectureId
                    && hit.segmentId == that.hit.segmentId
                    && hit.startMs == that.hit.startMs
                    && Objects.equals(hit.sourceType, that.hit.sourceType)
                    && hit.snippet.equals(that.hit.snippet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hit.lectureId, hit.segmentId, hit.startMs,
                    hit.sourceType, hit.snippet);
        }
    }

    private static final DiffUtil.ItemCallback<ListItem> DIFF =
            new DiffUtil.ItemCallback<ListItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull ListItem oldItem,
                                               @NonNull ListItem newItem) {
                    if (oldItem instanceof HeaderItem && newItem instanceof HeaderItem) {
                        return ((HeaderItem) oldItem).lectureId
                                == ((HeaderItem) newItem).lectureId;
                    }
                    if (oldItem instanceof HitItem && newItem instanceof HitItem) {
                        SearchHit a = ((HitItem) oldItem).hit;
                        SearchHit b = ((HitItem) newItem).hit;
                        return a.lectureId == b.lectureId
                                && a.segmentId == b.segmentId
                                && Objects.equals(a.sourceType, b.sourceType)
                                && a.startMs == b.startMs;
                    }
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ListItem oldItem,
                                                  @NonNull ListItem newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull private final Listener listener;

    public SearchResultsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).viewType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(ItemSearchHeaderBinding.inflate(inflater, parent, false));
        }
        return new HitViewHolder(
                ItemSearchHitBinding.inflate(inflater, parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = getItem(position);
        if (holder instanceof HeaderViewHolder && item instanceof HeaderItem) {
            ((HeaderViewHolder) holder).bind((HeaderItem) item);
        } else if (holder instanceof HitViewHolder && item instanceof HitItem) {
            ((HitViewHolder) holder).bind((HitItem) item);
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchHeaderBinding binding;

        HeaderViewHolder(@NonNull ItemSearchHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull HeaderItem item) {
            binding.textHeader.setText(item.title);
        }
    }

    static class HitViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchHitBinding binding;
        @NonNull private SearchHit hit = new SearchHit();

        HitViewHolder(@NonNull ItemSearchHitBinding binding, @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(v -> listener.onHitClicked(hit));
        }

        void bind(@NonNull HitItem item) {
            this.hit = item.hit;
            String type = hit.sourceType != null ? hit.sourceType : SearchHit.SOURCE_TRANSCRIPT;
            binding.imageHitType.setImageResource(iconFor(type));
            int tint = MaterialColors.getColor(binding.getRoot(),
                    androidx.appcompat.R.attr.colorPrimary);
            binding.imageHitType.setColorFilter(tint);
            String label = hit.sourceLabel != null && !hit.sourceLabel.isEmpty()
                    ? hit.sourceLabel
                    : labelFor(type);
            binding.textHitType.setText(label);
            if (hit.startMs >= 0 && SearchHit.SOURCE_TRANSCRIPT.equals(type)) {
                binding.textHitTime.setVisibility(View.VISIBLE);
                binding.textHitTime.setText(TranscriptAdapter.formatTimestamp(hit.startMs));
            } else {
                binding.textHitTime.setVisibility(View.GONE);
            }
            binding.textHitSnippet.setText(highlightSnippet(hit.snippet));
        }

        private static int iconFor(@NonNull String type) {
            switch (type) {
                case SearchHit.SOURCE_CHAT:
                    return R.drawable.ic_chat_24;
                case SearchHit.SOURCE_NOTES:
                case SearchHit.SOURCE_KEY_TERM:
                case SearchHit.SOURCE_ACTION:
                    return R.drawable.ic_notes_24;
                default:
                    return R.drawable.ic_mic_24;
            }
        }

        @NonNull
        private static String labelFor(@NonNull String type) {
            switch (type) {
                case SearchHit.SOURCE_CHAT:
                    return "Ask AI";
                case SearchHit.SOURCE_NOTES:
                    return "Notes";
                case SearchHit.SOURCE_KEY_TERM:
                    return "Key term";
                case SearchHit.SOURCE_ACTION:
                    return "Action item";
                default:
                    return "Transcript";
            }
        }
    }

    @NonNull
    static Spanned highlightSnippet(@NonNull String snippet) {
        String safe = snippet == null ? "" : snippet;
        if (!safe.contains("<b>")) {
            // Escape HTML for LIKE hits, keep plain text readable.
            safe = Html.escapeHtml(safe);
        }
        return Html.fromHtml(safe, Html.FROM_HTML_MODE_LEGACY);
    }
}
