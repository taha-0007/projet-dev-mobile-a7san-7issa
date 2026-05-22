package com.example.a7san7issa.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.a7san7issa.R;
import com.example.a7san7issa.adapters.DocumentAdapter;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.Document;
import com.example.a7san7issa.models.PaginatedResponse;
import com.example.a7san7issa.ui.document.PdfViewerActivity;
import com.example.a7san7issa.utils.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentsFragment extends Fragment {

    private Spinner spinnerFiliere, spinnerMatiere, spinnerAnnee, spinnerType;
    private RecyclerView recyclerView;
    private DocumentAdapter adapter;
    private ApiService apiService;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvEmpty;

    private List<String> filieres = new ArrayList<>();
    private List<String> matieres = new ArrayList<>();
    private List<String> annees = new ArrayList<>();
    private List<String> types = new ArrayList<>();

    private Map<String, List<String>> matieresParFiliere = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_documents, container, false);

        TokenManager tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        spinnerFiliere = v.findViewById(R.id.spinnerFiliere);
        spinnerMatiere = v.findViewById(R.id.spinnerMatiere);
        spinnerAnnee = v.findViewById(R.id.spinnerAnnee);
        spinnerType = v.findViewById(R.id.spinnerType);
        recyclerView = v.findViewById(R.id.recyclerView);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        tvEmpty = v.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DocumentAdapter(new DocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onItemClick(Document document) {
                Intent i = new Intent(getActivity(), PdfViewerActivity.class);
                // Construction correcte de l'URL
                String pdfUrl = document.getFichier();
                if (pdfUrl != null && !pdfUrl.startsWith("http")) {
                    pdfUrl = "http://10.0.2.2:8000" + pdfUrl;
                }
                i.putExtra("pdf_url", pdfUrl);
                i.putExtra("titre", document.getTitre());
                i.putExtra("document_id", document.getId());   // <-- indispensable pour l'historique
                startActivity(i);
            }

            @Override
            public void onFavoriteClick(Document document) {
                Map<String, Integer> body = new HashMap<>();
                body.put("document_id", document.getId());
                apiService.addFavorite(body).enqueue(new Callback<com.example.a7san7issa.models.Favori>() {
                    @Override
                    public void onResponse(Call<com.example.a7san7issa.models.Favori> call,
                                           Response<com.example.a7san7issa.models.Favori> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Ajouté aux favoris", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Déjà en favori ou erreur", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.a7san7issa.models.Favori> call, Throwable t) {
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(() -> {
            applyFilters();
            swipeRefresh.setRefreshing(false);
        });

        // Initialisation des listes statiques
        types.add("");
        types.add("national");
        types.add("rattrapage");
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        annees.add("");
        for (int annee = 2014; annee <= 2024; annee++) {
            annees.add(annee + "-" + (annee + 1));
        }
        ArrayAdapter<String> anneeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, annees);
        anneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(anneeAdapter);

        loadAcademicStructure();

        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spinnerFiliere) {
                    updateMatieresForSelectedFiliere();
                }
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };
        spinnerFiliere.setOnItemSelectedListener(filterListener);
        spinnerMatiere.setOnItemSelectedListener(filterListener);
        spinnerAnnee.setOnItemSelectedListener(filterListener);
        spinnerType.setOnItemSelectedListener(filterListener);

        applyFilters();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        applyFilters();
    }

    private void loadAcademicStructure() {
        apiService.getAcademicStructure().enqueue(new Callback<Map<String, Map<String, List<String>>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, List<String>>>> call,
                                   Response<Map<String, Map<String, List<String>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Map<String, List<String>>> structure = response.body();
                    filieres.clear();
                    filieres.add("");
                    filieres.addAll(structure.keySet());

                    matieresParFiliere.clear();
                    for (Map.Entry<String, Map<String, List<String>>> entry : structure.entrySet()) {
                        matieresParFiliere.put(entry.getKey(), entry.getValue().get("matieres"));
                    }

                    ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, filieres);
                    filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFiliere.setAdapter(filiereAdapter);

                    updateMatieresForSelectedFiliere();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, List<String>>>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur chargement structure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateMatieresForSelectedFiliere() {
        String filiere = spinnerFiliere.getSelectedItem() != null ?
                spinnerFiliere.getSelectedItem().toString() : "";
        List<String> matieresFiliere = new ArrayList<>();
        matieresFiliere.add("");
        if (!filiere.isEmpty() && matieresParFiliere.containsKey(filiere)) {
            matieresFiliere.addAll(matieresParFiliere.get(filiere));
        } else if (filiere.isEmpty()) {
            for (List<String> list : matieresParFiliere.values()) {
                for (String mat : list) {
                    if (!matieresFiliere.contains(mat)) {
                        matieresFiliere.add(mat);
                    }
                }
            }
        }

        ArrayAdapter<String> matiereAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, matieresFiliere);
        matiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatiere.setAdapter(matiereAdapter);
    }

    private void applyFilters() {
        Map<String, String> queries = new HashMap<>();

        if (spinnerFiliere.getSelectedItem() != null && !spinnerFiliere.getSelectedItem().toString().isEmpty())
            queries.put("filiere", spinnerFiliere.getSelectedItem().toString());
        if (spinnerMatiere.getSelectedItem() != null && !spinnerMatiere.getSelectedItem().toString().isEmpty())
            queries.put("matiere", spinnerMatiere.getSelectedItem().toString());
        if (spinnerAnnee.getSelectedItem() != null && !spinnerAnnee.getSelectedItem().toString().isEmpty())
            queries.put("annee", spinnerAnnee.getSelectedItem().toString());
        if (spinnerType.getSelectedItem() != null && !spinnerType.getSelectedItem().toString().isEmpty())
            queries.put("type", spinnerType.getSelectedItem().toString());

        apiService.getDocuments(queries).enqueue(new Callback<PaginatedResponse<Document>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Document>> call,
                                   Response<PaginatedResponse<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Document> docs = response.body().getResults();
                    adapter.setDocuments(docs);
                    tvEmpty.setVisibility(docs.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(docs.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    Toast.makeText(getContext(), "Erreur de chargement", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Document>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}