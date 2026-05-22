package com.example.a7san7issa.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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

import com.bumptech.glide.Glide;
import com.example.a7san7issa.R;
import com.example.a7san7issa.adapters.DocumentAdapter;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.Document;
import com.example.a7san7issa.models.Historique;
import com.example.a7san7issa.models.PaginatedResponse;
import com.example.a7san7issa.models.User;
import com.example.a7san7issa.ui.document.PdfViewerActivity;
import com.example.a7san7issa.utils.TokenManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private ApiService apiService;
    private TokenManager tokenManager;
    private RecyclerView rvRecent;
    private DocumentAdapter adapter;
    private TextView tvEmptyRecent, tvGreeting;
    private ImageView ivHomeAvatar;
    private EditText etSearch;
    private ActivityResultLauncher<Intent> speechLauncher;
    private boolean isSearching = false;
    private SwipeRefreshLayout swipeRefresh;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);
        tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        ivHomeAvatar = v.findViewById(R.id.ivHomeAvatar);
        tvGreeting = v.findViewById(R.id.tvGreeting);
        rvRecent = v.findViewById(R.id.rvRecentDocuments);
        tvEmptyRecent = v.findViewById(R.id.tvEmptyRecent);
        etSearch = v.findViewById(R.id.etSearch);
        ImageButton btnMicro = v.findViewById(R.id.btnMicro);
        swipeRefresh = v.findViewById(R.id.swipeRefresh);

        rvRecent.setLayoutManager(new LinearLayoutManager(getContext()));

        swipeRefresh.setOnRefreshListener(() -> {
            if (tokenManager.isLoggedIn()) {
                loadRecentDocuments();
            }
            swipeRefresh.setRefreshing(false);
        });

        adapter = new DocumentAdapter(new DocumentAdapter.OnDocumentActionListener() {
            @Override
            public void onItemClick(Document doc) {
                Intent i = new Intent(getActivity(), PdfViewerActivity.class);
                String pdfUrl = doc.getFichier();
                if (pdfUrl != null && !pdfUrl.startsWith("http")) {
                    pdfUrl = "http://10.0.2.2:8000" + pdfUrl;
                }
                i.putExtra("pdf_url", pdfUrl);
                i.putExtra("titre", doc.getTitre());
                i.putExtra("document_id", doc.getId());
                startActivity(i);
            }

            @Override
            public void onFavoriteClick(Document doc) {
                Map<String, Integer> body = new HashMap<>();
                body.put("document_id", doc.getId());
                apiService.addFavorite(body).enqueue(new Callback<com.example.a7san7issa.models.Favori>() {
                    @Override
                    public void onResponse(Call<com.example.a7san7issa.models.Favori> call,
                                           Response<com.example.a7san7issa.models.Favori> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Ajouté aux favoris", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Déjà en favori", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<com.example.a7san7issa.models.Favori> call, Throwable t) {}
                });
            }
        });
        rvRecent.setAdapter(adapter);

        etSearch.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String query = textView.getText().toString().trim();
                if (!query.isEmpty()) {
                    searchDocuments(query);
                }
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty() && isSearching) {
                    isSearching = false;
                    loadRecentDocuments();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        speechLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            etSearch.setText(results.get(0));
                            searchDocuments(results.get(0));
                        }
                    }
                });

        btnMicro.setOnClickListener(view -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez votre recherche...");
            speechLauncher.launch(intent);
        });

        loadUserInfo();

        if (tokenManager.isLoggedIn()) {
            loadRecentDocuments();
        } else {
            tvEmptyRecent.setVisibility(View.VISIBLE);
            tvEmptyRecent.setText("Connectez-vous pour voir votre historique.");
        }

        v.findViewById(R.id.cardDocuments).setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(new DocumentsFragment(), "Documents");
            }
        });
        v.findViewById(R.id.cardAI).setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(new AIFragment(), "Assistant IA");
            }
        });

        v.findViewById(R.id.tvViewAllHistory).setOnClickListener(view -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateTo(new DocumentsFragment(), "Documents");
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isSearching && tokenManager.isLoggedIn()) {
            loadRecentDocuments();
        }
        // Recharger l'avatar et le message d'accueil
        loadUserInfo();
    }

    private void loadUserInfo() {
        apiService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvGreeting.setText("Bonjour, " + user.getUsername());

                    // 1. Essayer l'URL déjà enregistrée localement
                    String imagePath = tokenManager.getProfileImagePath();
                    // 2. Sinon, utiliser l'URL renvoyée par l'API
                    if (imagePath == null || imagePath.isEmpty()) {
                        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            imagePath = user.getAvatar();
                            if (!imagePath.startsWith("http")) {
                                imagePath = "http://10.0.2.2:8000" + imagePath;
                            }
                            tokenManager.saveProfileImagePath(imagePath);
                        }
                    }

                    // Afficher avec Glide (ou l'icône par défaut)
                    if (imagePath != null && !imagePath.isEmpty()) {
                        Glide.with(HomeFragment.this)
                                .load(imagePath)
                                .placeholder(R.mipmap.ic_launcher)
                                .error(R.mipmap.ic_launcher)
                                .circleCrop()
                                .into(ivHomeAvatar);
                    } else {
                        ivHomeAvatar.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            }
            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }

    private void loadRecentDocuments() {
        isSearching = false;
        apiService.getHistory().enqueue(new Callback<List<Historique>>() {
            @Override
            public void onResponse(Call<List<Historique>> call, Response<List<Historique>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Document> docs = new ArrayList<>();
                    for (Historique h : response.body()) {
                        if (h.getDocument() != null) docs.add(h.getDocument());
                    }
                    adapter.setDocuments(docs);
                    if (docs.isEmpty()) {
                        tvEmptyRecent.setVisibility(View.VISIBLE);
                        rvRecent.setVisibility(View.GONE);
                        tvEmptyRecent.setText("Aucun document consulté récemment.");
                    } else {
                        tvEmptyRecent.setVisibility(View.GONE);
                        rvRecent.setVisibility(View.VISIBLE);
                    }
                } else {
                    tvEmptyRecent.setVisibility(View.VISIBLE);
                    tvEmptyRecent.setText("Aucun document récent.");
                }
            }

            @Override
            public void onFailure(Call<List<Historique>> call, Throwable t) {
                tvEmptyRecent.setVisibility(View.VISIBLE);
                tvEmptyRecent.setText("Impossible de charger l'historique.");
            }
        });
    }

    private void searchDocuments(String query) {
        isSearching = true;
        Map<String, String> filters = new HashMap<>();
        filters.put("search", query);
        apiService.getDocuments(filters).enqueue(new Callback<PaginatedResponse<Document>>() {
            @Override
            public void onResponse(Call<PaginatedResponse<Document>> call,
                                   Response<PaginatedResponse<Document>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Document> docs = response.body().getResults();
                    adapter.setDocuments(docs);
                    if (docs.isEmpty()) {
                        tvEmptyRecent.setVisibility(View.VISIBLE);
                        rvRecent.setVisibility(View.GONE);
                        tvEmptyRecent.setText("Aucun résultat pour \"" + query + "\"");
                    } else {
                        tvEmptyRecent.setVisibility(View.GONE);
                        rvRecent.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(getContext(), "Recherche indisponible", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PaginatedResponse<Document>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }
}