package com.example.a7san7issa.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.a7san7issa.R;
import com.example.a7san7issa.adapters.AdminDocumentAdapter;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.Document;
import com.example.a7san7issa.models.PaginatedResponse;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDocumentsFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminDocumentAdapter adapter;
    private ApiService apiService;
    private List<Document> documents = new ArrayList<>();
    private TextView tvEmpty;
    private EditText etSearch;
    private Spinner spinnerFiliere, spinnerMatiere, spinnerAnnee, spinnerType;
    private SwipeRefreshLayout swipeRefresh;

    private Map<String, List<String>> matieresParFiliere = new HashMap<>();

    private ActivityResultLauncher<Intent> addDocumentLauncher;
    private ActivityResultLauncher<Intent> editDocumentLauncher;

    private Handler searchHandler = new Handler();
    private Runnable searchRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_documents, container, false);

        TokenManager tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        recyclerView = v.findViewById(R.id.recyclerView);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        FloatingActionButton fab = v.findViewById(R.id.fabAddDocument);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);
        etSearch = v.findViewById(R.id.etSearch);

        spinnerFiliere = v.findViewById(R.id.spinnerFiliere);
        spinnerMatiere = v.findViewById(R.id.spinnerMatiere);
        spinnerAnnee = v.findViewById(R.id.spinnerAnnee);
        spinnerType = v.findViewById(R.id.spinnerType);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminDocumentAdapter(documents, new AdminDocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onEdit(int documentId) {
                Intent intent = new Intent(getActivity(), DocumentUpdateActivity.class);
                intent.putExtra("document_id", documentId);
                editDocumentLauncher.launch(intent);
            }

            @Override
            public void onDelete(int documentId) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Supprimer le document")
                        .setMessage("Êtes-vous sûr de vouloir supprimer ce document ?")
                        .setPositiveButton("Supprimer", (dialog, which) -> deleteDocument(documentId))
                        .setNegativeButton("Annuler", null)
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        // SwipeRefresh
        if (swipeRefresh != null) {
            swipeRefresh.setOnRefreshListener(() -> {
                if (etSearch != null) etSearch.setText("");
                loadDocuments(new HashMap<>());
                swipeRefresh.setRefreshing(false);
            });
        }

        // Recherche en temps réel avec délai
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    searchHandler.removeCallbacks(searchRunnable);
                    searchRunnable = () -> applyFilters();
                    searchHandler.postDelayed(searchRunnable, 500);
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // Initialisation des spinners
        setupSpinners();

        // Launchers
        addDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == AddDocumentActivity.RESULT_OK) {
                        loadDocuments(new HashMap<>());
                    }
                });

        editDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == DocumentUpdateActivity.RESULT_OK) {
                        loadDocuments(new HashMap<>());
                    }
                });

        fab.setOnClickListener(view -> {
            Intent intent = new Intent(getActivity(), AddDocumentActivity.class);
            addDocumentLauncher.launch(intent);
        });

        // Chargement initial
        loadDocuments(new HashMap<>());
        return v;
    }

    private void setupSpinners() {
        // 1. Année : liste statique complète
        List<String> annees = new ArrayList<>();
        annees.add("");   // option "Toutes"
        for (int annee = 2014; annee <= 2024; annee++) {
            annees.add(annee + "-" + (annee + 1));
        }
        ArrayAdapter<String> anneeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, annees);
        anneeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAnnee.setAdapter(anneeAdapter);

        // 2. Type
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, new String[]{"", "cours", "national", "rattrapage"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // 3. Filière (depuis l'API) + mise à jour des matières
        apiService.getAcademicStructure().enqueue(new Callback<Map<String, Map<String, List<String>>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, List<String>>>> call,
                                   Response<Map<String, Map<String, List<String>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Map<String, List<String>>> structure = response.body();
                    List<String> filieres = new ArrayList<>(structure.keySet());

                    // Stocker les matières par filière
                    matieresParFiliere.clear();
                    for (Map.Entry<String, Map<String, List<String>>> entry : structure.entrySet()) {
                        matieresParFiliere.put(entry.getKey(), entry.getValue().get("matieres"));
                    }

                    // Adapter pour la filière
                    ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(requireContext(),
                            android.R.layout.simple_spinner_item, filieres);
                    filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFiliere.setAdapter(filiereAdapter);

                    // Initialiser les matières (toutes les matières fusionnées)
                    updateMatieresForFiliere("");
                }
            }
            @Override
            public void onFailure(Call<Map<String, Map<String, List<String>>>> call, Throwable t) {}
        });

        // Listeners
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (parent == spinnerFiliere) {
                    String filiere = (String) parent.getSelectedItem();
                    updateMatieresForFiliere(filiere != null ? filiere : "");
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
    }

    /**
     * Met à jour le spinner Matière en fonction de la filière sélectionnée.
     */
    private void updateMatieresForFiliere(String filiere) {
        List<String> matieres = new ArrayList<>();
        matieres.add("");   // option "Toutes"

        if (filiere.isEmpty()) {
            // Si aucune filière, on fusionne toutes les matières (sans doublons)
            for (List<String> list : matieresParFiliere.values()) {
                for (String mat : list) {
                    if (!matieres.contains(mat)) {
                        matieres.add(mat);
                    }
                }
            }
        } else {
            List<String> liste = matieresParFiliere.get(filiere);
            if (liste != null) {
                matieres.addAll(liste);
            }
        }

        ArrayAdapter<String> matiereAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, matieres);
        matiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatiere.setAdapter(matiereAdapter);
    }

    private void applyFilters() {
        Map<String, String> queries = getCurrentFilters();
        if (etSearch != null) {
            String search = etSearch.getText().toString().trim();
            if (!search.isEmpty()) {
                queries.put("search", search);
            }
        }
        loadDocuments(queries);
    }

    private Map<String, String> getCurrentFilters() {
        Map<String, String> queries = new HashMap<>();
        if (spinnerFiliere.getSelectedItem() != null && !spinnerFiliere.getSelectedItem().toString().isEmpty())
            queries.put("filiere", spinnerFiliere.getSelectedItem().toString());
        if (spinnerMatiere.getSelectedItem() != null && !spinnerMatiere.getSelectedItem().toString().isEmpty())
            queries.put("matiere", spinnerMatiere.getSelectedItem().toString());
        if (spinnerAnnee.getSelectedItem() != null && !spinnerAnnee.getSelectedItem().toString().isEmpty())
            queries.put("annee", spinnerAnnee.getSelectedItem().toString());
        if (spinnerType.getSelectedItem() != null && !spinnerType.getSelectedItem().toString().isEmpty())
            queries.put("type", spinnerType.getSelectedItem().toString());
        return queries;
    }

    private void loadDocuments(Map<String, String> filters) {
        apiService.getDocuments(filters).enqueue(new Callback<PaginatedResponse<Document>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Document>> call, Response<PaginatedResponse<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    documents.clear();
                    documents.addAll(response.body().getResults());
                    adapter.notifyDataSetChanged();
                    if (tvEmpty != null)
                        tvEmpty.setVisibility(documents.isEmpty() ? View.VISIBLE : View.GONE);
                } else {
                    Toast.makeText(getContext(), "Erreur chargement", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<PaginatedResponse<Document>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteDocument(int id) {
        apiService.deleteDocument(id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Document supprimé", Toast.LENGTH_SHORT).show();
                    loadDocuments(new HashMap<>());
                } else {
                    Toast.makeText(getContext(), "Erreur suppression", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}