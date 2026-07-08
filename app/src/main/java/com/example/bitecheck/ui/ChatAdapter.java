package com.example.bitecheck.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.bitecheck.R;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.BubbleViewHolder> {

    public static class ChatMessage {
        public String text;
        public final boolean isUser;

        public ChatMessage(String text, boolean isUser) {
            this.text = text;
            this.isUser = isUser;
        }
    }

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;

    private final List<ChatMessage> messages = new ArrayList<>();

    /** Adds a message and returns its position. */
    public int add(String text, boolean isUser) {
        messages.add(new ChatMessage(text, isUser));
        int position = messages.size() - 1;
        notifyItemInserted(position);
        return position;
    }

    /** Replaces the text of an existing bubble (e.g. "Thinking…" → result). */
    public void replace(int position, String text) {
        if (position >= 0 && position < messages.size()) {
            messages.get(position).text = text;
            notifyItemChanged(position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser ? TYPE_USER : TYPE_BOT;
    }

    @NonNull
    @Override
    public BubbleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = viewType == TYPE_USER
                ? R.layout.item_chat_user : R.layout.item_chat_bot;
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);
        return new BubbleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BubbleViewHolder holder, int position) {
        holder.message.setText(messages.get(position).text);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class BubbleViewHolder extends RecyclerView.ViewHolder {
        final TextView message;

        BubbleViewHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.text_message);
        }
    }
}
