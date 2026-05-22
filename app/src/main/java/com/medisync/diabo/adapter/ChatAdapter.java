package com.medisync.diabo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medisync.diabo.databinding.ItemChatMessageBinding;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    public static class Message {
        public String text;
        public boolean isUser;
        public Message(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private final List<Message> messages;

    public ChatAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatMessageBinding binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message msg = messages.get(position);
        if (msg.isUser) {
            holder.binding.cardUser.setVisibility(View.VISIBLE);
            holder.binding.cardAssistant.setVisibility(View.GONE);
            holder.binding.tvUserMessage.setText(msg.text);
        } else {
            holder.binding.cardUser.setVisibility(View.GONE);
            holder.binding.cardAssistant.setVisibility(View.VISIBLE);
            holder.binding.tvAssistantMessage.setText(msg.text);
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemChatMessageBinding binding;
        public ViewHolder(ItemChatMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
