package com.example.a7san7issa.ui.admin;

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
import com.example.a7san7issa.adapters.AdminUserAdapter;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.User;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUsersFragment extends Fragment {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private ApiService apiService;
    private List<User> users = new ArrayList<>();
    private String currentUsername;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_users, container, false);

        TokenManager tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
        currentUsername = tokenManager.getUsername();

        recyclerView = v.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminUserAdapter(users, new AdminUserAdapter.OnUserActionListener() {
            @Override
            public void onToggleAdmin(int userId, boolean currentStatus) {
                // Protection anti‑rétrogradation
                if (!currentStatus && currentUsername != null && currentUsername.equals(getUsernameById(userId))) {
                    Toast.makeText(getContext(), "Vous ne pouvez pas vous rétrograder vous‑même.", Toast.LENGTH_SHORT).show();
                    return;
                }
                Map<String, Object> fields = new HashMap<>();
                fields.put("is_staff", !currentStatus);
                apiService.updateUser(userId, fields).enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Statut modifié", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        } else {
                            Toast.makeText(getContext(), "Erreur", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onDelete(int userId) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Supprimer l'utilisateur")
                        .setMessage("Cette action est irréversible. Continuer ?")
                        .setPositiveButton("Supprimer", (dialog, which) -> {
                            apiService.deleteUser(userId).enqueue(new Callback<Void>() {
                                @Override
                                public void onResponse(Call<Void> call, Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        Toast.makeText(getContext(), "Utilisateur supprimé", Toast.LENGTH_SHORT).show();
                                        loadUsers();
                                    } else {
                                        Toast.makeText(getContext(), "Erreur suppression", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Void> call, Throwable t) {
                                    Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                                }
                            });
                        })
                        .setNegativeButton("Annuler", null)
                        .show();
            }
        });
        recyclerView.setAdapter(adapter);

        loadUsers();
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    private void loadUsers() {
        apiService.getUsers().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    users.clear();
                    users.addAll(response.body());
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(getContext(), "Erreur chargement", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getUsernameById(int userId) {
        for (User u : users) {
            if (u.getId() == userId) return u.getUsername();
        }
        return null;
    }
}