package com.example.a7san7issa.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a7san7issa.R;
import com.example.a7san7issa.adapters.DocumentAdapter;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.Document;
import com.example.a7san7issa.models.Favori;
import com.example.a7san7issa.models.PaginatedResponse;
import com.example.a7san7issa.ui.document.PdfViewerActivity;
import com.example.a7san7issa.utils.TokenManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritesFragment extends Fragment {

    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private ApiService apiService;
    private List<Favori> favorisList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_favorites, container, false);

        TokenManager tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DocumentAdapter(new DocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onItemClick(Document document) {
                Intent i = new Intent(getActivity(), PdfViewerActivity.class);
                String pdfUrl = document.getFichier();
                if (pdfUrl != null && !pdfUrl.startsWith("http")) {
                    pdfUrl = "http://10.0.2.2:8000" + pdfUrl;
                }
                i.putExtra("pdf_url", pdfUrl);
                i.putExtra("titre", document.getTitre());
                i.putExtra("document_id", document.getId());
                startActivity(i);
            }

            @Override
            public void onFavoriteClick(Document document) {
                for (Favori f : favorisList) {
                    if (f.getDocument().getId() == document.getId()) {
                        apiService.removeFavorite(f.getId()).enqueue(new Callback<Void>() {
                            @Override
                            public void onResponse(Call<Void> call, Response<Void> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(getContext(), "Retiré des favoris", Toast.LENGTH_SHORT).show();
                                    loadFavorites();
                                } else {
                                    Toast.makeText(getContext(), "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override
                            public void onFailure(Call<Void> call, Throwable t) {
                                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                            }
                        });
                        break;
                    }
                }
            }
        });
        recyclerView.setAdapter(adapter);

        loadFavorites();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites();
    }

    private void loadFavorites() {
        apiService.getFavorites().enqueue(new Callback<PaginatedResponse<Favori>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Favori>> call,
                                   Response<PaginatedResponse<Favori>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    favorisList = response.body().getResults();
                    List<Document> docs = new ArrayList<>();
                    for (Favori f : favorisList) {
                        docs.add(f.getDocument());
                    }
                    adapter.setDocuments(docs);
                } else {
                    Toast.makeText(getContext(), "Erreur chargement favoris", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Favori>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}