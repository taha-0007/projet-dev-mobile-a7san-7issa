package com.example.a7san7issa.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a7san7issa.R;
import com.example.a7san7issa.models.Document;
import java.util.List;

public class AdminDocumentAdapter extends RecyclerView.Adapter<AdminDocumentAdapter.ViewHolder> {

    private List<Document> documents;
    private OnDocumentActionListener listener;

    public interface OnDocumentActionListener {
        void onEdit(int documentId);
        void onDelete(int documentId);
    }

    public AdminDocumentAdapter(List<Document> documents, OnDocumentActionListener listener) {
        this.documents = documents;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_document, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = documents.get(position);
        holder.tvTitre.setText(doc.getTitre());
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(doc.getId()));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(doc.getId()));
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitre;
        ImageButton btnEdit, btnDelete;

        ViewHolder(View itemView) {
            super(itemView);
            tvTitre = itemView.findViewById(R.id.tvAdminDocTitre);
            btnEdit = itemView.findViewById(R.id.btnEditDoc);
            btnDelete = itemView.findViewById(R.id.btnDeleteDoc);
        }
    }
}