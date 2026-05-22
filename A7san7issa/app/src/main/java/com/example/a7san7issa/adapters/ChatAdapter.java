package com.example.a7san7issa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a7san7issa.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.textView.setText(msg.getContent());

        // Appliquer les couleurs dynamiques selon le thème
        if (msg.isUser()) {
            // Bulle de l'utilisateur : fond couleur primary, texte blanc
            holder.textView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.primary));
            holder.textView.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));
        } else {
            // Bulle de l'assistant : fond semi-transparent adapté au thème, texte adaptatif
            // On utilise une couleur avec alpha pour rester visible sur n'importe quel fond
            holder.textView.setBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.primary_light));
            holder.textView.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
        }

        // Marges pour aligner à droite ou à gauche
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.textView.getLayoutParams();
        if (msg.isUser()) {
            params.setMarginStart(100);
            params.setMarginEnd(16);
        } else {
            params.setMarginStart(16);
            params.setMarginEnd(100);
        }
        holder.textView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textView;

        ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.tvMessage);
        }
    }

    // Classe interne pour les messages (inchangée)
    public static class ChatMessage {
        private final String content;
        private final boolean isUser;

        public ChatMessage(String content, boolean isUser) {
            this.content = content;
            this.isUser = isUser;
        }

        public String getContent() {
            return content;
        }

        public boolean isUser() {
            return isUser;
        }
    }
}