package com.example.a7san7issa.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.firebase.FirebaseAuthManager;
import com.example.a7san7issa.models.FirebaseTokenRequest;
import com.example.a7san7issa.models.LoginResponse;
import com.example.a7san7issa.models.User;
import com.example.a7san7issa.ui.main.MainActivity;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.gms.common.SignInButton;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private ApiService apiService;
    private TokenManager tokenManager;

    // ---------- Nouveaux champs Firebase ----------
    private FirebaseAuthManager firebaseAuthManager;
    private static final int RC_SIGN_IN = 9001;
    // --------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tokenManager = new TokenManager(this);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        // Initialiser Firebase Auth Manager
        firebaseAuthManager = new FirebaseAuthManager(this);

        if (tokenManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        progressBar = findViewById(R.id.progressBar);

        // ---------- Bouton Google Sign-In ----------
        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> {
            Intent signInIntent = firebaseAuthManager.getGoogleSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
        // ------------------------------------------
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());
        findViewById(R.id.tvGoSignup).setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class)));
    }

    // ---------- Nouvelle méthode : réception du résultat Google ----------
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            firebaseAuthManager.handleGoogleSignInResult(data, new FirebaseAuthManager.OnGoogleSignInCompleteListener() {
                @Override
                public void onSuccess(String firebaseIdToken, String email, String displayName) {
                    // Échange du token Firebase contre un JWT Django
                    exchangeFirebaseTokenForJwt(firebaseIdToken, email, displayName);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(LoginActivity.this, "Échec connexion Google : " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    // -------------------------------------------------------------------

    // ---------- Nouvelle méthode d'échange Firebase -> JWT ----------
    private void exchangeFirebaseTokenForJwt(String firebaseIdToken, String email, String displayName) {
        showLoading(true);
        FirebaseTokenRequest request = new FirebaseTokenRequest(firebaseIdToken);
        apiService.firebaseLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse tokens = response.body();
                    tokenManager.saveTokens(tokens.getAccess(), tokens.getRefresh());
                    // Sauvegarde immédiate (statut admin = false par défaut)
                    tokenManager.saveUserInfo(false, displayName != null ? displayName : email, email);

                    // Récupérer le profil pour obtenir le vrai statut admin
                    apiService.getProfile().enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                User user = response.body();
                                // Met à jour le statut admin et le username réel
                                tokenManager.saveUserInfo(user.isStaff(), user.getUsername(), user.getEmail());
                            }
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                } else {
                    Toast.makeText(LoginActivity.this, "Erreur échange JWT (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // -------------------------------------------------------------------

    // ====== LE RESTE DE VOTRE CODE RESTE IDENTIQUE ======
    private void performLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", username);
        credentials.put("password", password);

        apiService.login(credentials).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse tokens = response.body();
                    tokenManager.saveTokens(tokens.getAccess(), tokens.getRefresh());

                    // Récupérer le profil pour le statut admin ET l'email
                    apiService.getProfile().enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                User user = response.body();
                                tokenManager.saveUserInfo(user.isStaff(), user.getUsername(), user.getEmail());
                            }
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                } else {
                    showLoading(false);
                    Toast.makeText(LoginActivity.this, "Identifiants incorrects", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
        etUsername.setEnabled(!show);
        etPassword.setEnabled(!show);
    }
}