package com.lecturelens.ui.search;

import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.data.local.SearchHit;
import com.lecturelens.databinding.ItemSearchHeaderBinding;
import com.lecturelens.databinding.ItemSearchHitBinding;
import com.lecturelens.ui.lecture.TranscriptAdapter;

import java.util.Objects;

/**
 * Track 5 — search results grouped by lecture (header + highlighted snippets).
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
            return hit.segmentId == that.hit.segmentId
                    && hit.startMs == that.hit.startMs
                    && hit.snippet.equals(that.hit.snippet);
        }

        @Override
        public int hashCode() {
            return Objects.hash(hit.segmentId, hit.startMs, hit.snippet);
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
                        return ((HitItem) oldItem).hit.segmentId
                                == ((HitItem) newItem).hit.segmentId;
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
            binding.textHitTime.setText(TranscriptAdapter.formatTimestamp(hit.startMs));
            binding.textHitSnippet.setText(highlightSnippet(hit.snippet));
        }
    }

    @NonNull
    static Spanned highlightSnippet(@NonNull String snippet) {
        // FTS snippet() wraps matches in <b>…</b>
        return Html.fromHtml(snippet, Html.FROM_HTML_MODE_LEGACY);
    }
}
