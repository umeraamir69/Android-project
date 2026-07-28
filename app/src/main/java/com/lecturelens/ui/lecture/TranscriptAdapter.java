package com.lecturelens.ui.lecture;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemTranscriptSegmentBinding;
import com.lecturelens.domain.model.TranscriptSegment;

import java.util.concurrent.TimeUnit;

/**
 * Track 5 — transcript segments; highlights the active row and seeks on tap.
 * Shows a speaker chip when STT diarization assigned a tag.
 */
public class TranscriptAdapter
        extends ListAdapter<TranscriptSegment, TranscriptAdapter.SegmentViewHolder> {

    public interface Listener {
        void onSegmentClicked(@NonNull TranscriptSegment segment);
    }

    private static final DiffUtil.ItemCallback<TranscriptSegment> DIFF =
            new DiffUtil.ItemCallback<TranscriptSegment>() {
                @Override
                public boolean areItemsTheSame(@NonNull TranscriptSegment oldItem,
                                               @NonNull TranscriptSegment newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull TranscriptSegment oldItem,
                                                  @NonNull TranscriptSegment newItem) {
                    return oldItem.getStartMs() == newItem.getStartMs()
                            && oldItem.getEndMs() == newItem.getEndMs()
                            && oldItem.getSpeakerTag() == newItem.getSpeakerTag()
                            && oldItem.getText().equals(newItem.getText());
                }
            };

    @NonNull private final Listener listener;
    private int activeIndex = -1;

    public TranscriptAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    public void setActiveIndex(int index) {
        int previous = activeIndex;
        activeIndex = index;
        if (previous >= 0 && previous < getItemCount()) {
            notifyItemChanged(previous);
        }
        if (activeIndex >= 0 && activeIndex < getItemCount()) {
            notifyItemChanged(activeIndex);
        }
    }

    @NonNull
    @Override
    public SegmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTranscriptSegmentBinding binding = ItemTranscriptSegmentBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new SegmentViewHolder(binding, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull SegmentViewHolder holder, int position) {
        holder.bind(getItem(position), position == activeIndex);
    }

    static class SegmentViewHolder extends RecyclerView.ViewHolder {

        private final ItemTranscriptSegmentBinding binding;
        @NonNull private TranscriptSegment segment =
                new TranscriptSegment(-1L, -1L, 0L, 0L, "");

        SegmentViewHolder(@NonNull ItemTranscriptSegmentBinding binding,
                          @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            binding.getRoot().setOnClickListener(v -> listener.onSegmentClicked(segment));
        }

        void bind(@NonNull TranscriptSegment segment, boolean active) {
            this.segment = segment;
            binding.textTimestamp.setText(formatTimestamp(segment.getStartMs()));
            binding.textSegment.setText(segment.getText());
            if (segment.getSpeakerTag() > 0) {
                binding.textSpeaker.setVisibility(View.VISIBLE);
                binding.textSpeaker.setText(binding.getRoot().getContext().getString(
                        R.string.transcript_speaker_label, segment.getSpeakerTag()));
            } else {
                binding.textSpeaker.setVisibility(View.GONE);
            }
            int bg = active
                    ? ContextCompat.getColor(binding.getRoot().getContext(),
                    R.color.md_secondary_container)
                    : android.graphics.Color.TRANSPARENT;
            binding.getRoot().setBackgroundColor(bg);
        }
    }

    @NonNull
    public static String formatTimestamp(long ms) {
        long totalSec = TimeUnit.MILLISECONDS.toSeconds(Math.max(0L, ms));
        long minutes = totalSec / 60;
        long seconds = totalSec % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }
}
