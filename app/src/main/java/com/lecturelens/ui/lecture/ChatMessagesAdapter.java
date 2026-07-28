package com.lecturelens.ui.lecture;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.lecturelens.R;
import com.lecturelens.databinding.ItemChatMessageBinding;
import com.lecturelens.domain.model.ChatMessage;
import com.lecturelens.ui.util.MarkdownSpans;

import java.util.Objects;

/** Full Ask AI thread (user + assistant). */
public class ChatMessagesAdapter
        extends ListAdapter<ChatMessage, ChatMessagesAdapter.Holder> {

    public ChatMessagesAdapter() {
        super(DIFF);
    }

    private static final DiffUtil.ItemCallback<ChatMessage> DIFF =
            new DiffUtil.ItemCallback<ChatMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                    return a.id == b.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ChatMessage a, @NonNull ChatMessage b) {
                    return a.id == b.id
                            && a.role.equals(b.role)
                            && a.text.equals(b.text)
                            && Objects.equals(a.citationsJson, b.citationsJson);
                }
            };

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemChatMessageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    static class Holder extends RecyclerView.ViewHolder {
        private final ItemChatMessageBinding binding;

        Holder(@NonNull ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull ChatMessage message) {
            boolean user = message.isUser();
            binding.textChatRole.setText(user
                    ? R.string.notes_chat_you
                    : R.string.notes_chat_ai);
            binding.textChatBody.setText(user
                    ? message.text
                    : MarkdownSpans.fromLiteMarkdown(message.text));

            LinearLayout root = (LinearLayout) binding.getRoot();
            root.setGravity(user ? Gravity.END : Gravity.START);
            ViewGroup.LayoutParams lp = binding.cardChatBubble.getLayoutParams();
            if (lp instanceof LinearLayout.LayoutParams) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
                params.gravity = user ? Gravity.END : Gravity.START;
                params.setMarginStart(user ? 48 : 0);
                params.setMarginEnd(user ? 0 : 48);
                binding.cardChatBubble.setLayoutParams(params);
            }
        }
    }
}
