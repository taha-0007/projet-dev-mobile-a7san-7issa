package com.example.a7san7issa.adapters;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a7san7issa.R;
import com.example.a7san7issa.models.Document;

import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.ViewHolder> {

    private List<Document> documents = new ArrayList<>();
    private OnDocumentActionListener listener;

    public interface OnDocumentActionListener {
        void onItemClick(Document document);
        void onFavoriteClick(Document document);
    }

    public DocumentAdapter(OnDocumentActionListener listener) {
        this.listener = listener;
    }

    public void setDocuments(List<Document> docs) {
        documents = docs != null ? docs : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_document, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Document doc = documents.get(position);
        holder.titre.setText(doc.getTitre());
        holder.matiere.setText(doc.getMatiere());
        holder.type.setText(doc.getTypeDocument());
        holder.filiere.setText(doc.getFiliere());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(doc));

        // Téléchargement
        holder.btnDownload.setOnClickListener(v -> {
            String url = "http://10.0.2.2:8000/api/documents/" + doc.getId() + "/download/";
            downloadFile(holder.itemView.getContext(), url, doc.getTitre() + ".pdf");
        });

        // Favori
        holder.btnFavorite.setOnClickListener(v -> listener.onFavoriteClick(doc));
    }

    @Override
    public int getItemCount() {
        return documents.size();
    }

    private void downloadFile(Context context, String url, String fileName) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(fileName);
            request.setDescription("Téléchargement du PDF");
            request.setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, fileName);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (manager != null) {
                manager.enqueue(request);
                Toast.makeText(context, "Téléchargement démarré", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Erreur de téléchargement", Toast.LENGTH_SHORT).show();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView titre, matiere, type, filiere;
        ImageButton btnDownload, btnFavorite;

        ViewHolder(View itemView) {
            super(itemView);
            titre = itemView.findViewById(R.id.tvTitre);
            matiere = itemView.findViewById(R.id.tvMatiere);
            type = itemView.findViewById(R.id.tvType);
            filiere = itemView.findViewById(R.id.tvFiliere);
            btnDownload = itemView.findViewById(R.id.btnDownload);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }
    }
}