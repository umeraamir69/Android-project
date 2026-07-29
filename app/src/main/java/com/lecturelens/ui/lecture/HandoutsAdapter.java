package com.lecturelens.ui.lecture;

import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemHandoutBinding;
import com.lecturelens.domain.model.Handout;

import java.io.File;
import java.util.Objects;

/** Handout images / PDFs / docs with thumbnail + open/delete. */
public class HandoutsAdapter extends ListAdapter<Handout, HandoutsAdapter.Holder> {

    public interface Listener {
        void onOpen(@NonNull Handout handout);

        void onDelete(@NonNull Handout handout);
    }

    @NonNull private final Listener listener;

    public HandoutsAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Handout> DIFF =
            new DiffUtil.ItemCallback<Handout>() {
                @Override
                public boolean areItemsTheSame(@NonNull Handout a, @NonNull Handout b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Handout a, @NonNull Handout b) {
                    return a.id == b.id
                            && a.localPath.equals(b.localPath)
                            && a.mimeType.equals(b.mimeType)
                            && a.displayName.equals(b.displayName)
                            && Objects.equals(a.remoteUrl, b.remoteUrl)
                            && a.extractedText.equals(b.extractedText);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemHandoutBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false), listener);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemHandoutBinding binding;
        private final Listener listener;
        @NonNull private Handout handout = new Handout(0, 0, "", "", "", "", null, 0);

        Holder(@NonNull ItemHandoutBinding binding, @NonNull Listener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
            binding.buttonHandoutOpen.setOnClickListener(v -> listener.onOpen(handout));
            binding.getRoot().setOnClickListener(v -> listener.onOpen(handout));
            binding.buttonHandoutDelete.setOnClickListener(v -> listener.onDelete(handout));
        }

        void bind(@NonNull Handout item) {
            this.handout = item;
            String name = item.displayName.isEmpty()
                    ? new File(item.localPath).getName()
                    : item.displayName;
            binding.textHandoutName.setText(name);

            String kind = item.isPdf() ? "PDF"
                    : item.isImage() ? "Image"
                    : item.mimeType.startsWith("text/") ? "Text"
                    : "Document";
            String cloud = item.remoteUrl != null && !item.remoteUrl.isEmpty()
                    ? " · cloud"
                    : " · on device";
            String snippet = item.extractedText.trim();
            if (snippet.length() > 80) {
                snippet = snippet.substring(0, 80) + "…";
            }
            binding.textHandoutMeta.setText(kind + cloud
                    + (snippet.isEmpty() ? "" : "\n" + snippet));

            if (item.isImage()) {
                File file = new File(item.localPath);
                if (file.exists()) {
                    binding.imageHandoutThumb.setImageBitmap(
                            BitmapFactory.decodeFile(file.getAbsolutePath()));
                    binding.imageHandoutThumb.setScaleType(
                            android.widget.ImageView.ScaleType.CENTER_CROP);
                } else {
                    binding.imageHandoutThumb.setImageResource(R.drawable.ic_notes_24);
                    binding.imageHandoutThumb.setScaleType(
                            android.widget.ImageView.ScaleType.CENTER);
                }
            } else {
                binding.imageHandoutThumb.setImageResource(
                        item.isPdf() ? R.drawable.ic_notes_24 : R.drawable.ic_folder_shared_24);
                binding.imageHandoutThumb.setScaleType(
                        android.widget.ImageView.ScaleType.CENTER);
            }
        }
    }
}
