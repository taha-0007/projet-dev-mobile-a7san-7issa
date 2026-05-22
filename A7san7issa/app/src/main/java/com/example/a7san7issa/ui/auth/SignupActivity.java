package com.example.a7san7issa.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
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

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerFiliere, spinnerLangue;
    private MaterialButton btnSignup;
    private ProgressBar progressBar;
    private ApiService apiService;

    // ---------- Nouveaux champs Firebase ----------
    private FirebaseAuthManager firebaseAuthManager;
    private static final int RC_SIGN_IN = 9002;  // code différent de LoginActivity (9001)
    // --------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        apiService = ApiClient.getClient(new TokenManager(this)).create(ApiService.class);

        // Initialiser Firebase Auth Manager
        firebaseAuthManager = new FirebaseAuthManager(this);

        initViews();
        setupSpinners();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        spinnerFiliere = findViewById(R.id.spinnerFiliere);
        spinnerLangue = findViewById(R.id.spinnerLangue);
        btnSignup = findViewById(R.id.btnSignup);
        progressBar = findViewById(R.id.progressBar);

        // ---------- Bouton Google Sign-In ----------
        SignInButton googleSignInButton = findViewById(R.id.googleSignInButton);
        googleSignInButton.setOnClickListener(v -> {
            Intent signInIntent = firebaseAuthManager.getGoogleSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
        // ------------------------------------------
    }

    private void setupSpinners() {
        String[] filieres = {"SM A", "SM B", "PC", "SVT", "ECO"};
        ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filieres);
        filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiliere.setAdapter(filiereAdapter);

        String[] langues = {"fr", "ar"};
        ArrayAdapter<String> langueAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, langues);
        langueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangue.setAdapter(langueAdapter);
    }

    private void setupListeners() {
        btnSignup.setOnClickListener(v -> performSignup());
        findViewById(R.id.tvGoLogin).setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
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
                    Toast.makeText(SignupActivity.this, "Échec connexion Google : " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                    TokenManager tokenManager = new TokenManager(SignupActivity.this);
                    tokenManager.saveTokens(tokens.getAccess(), tokens.getRefresh());
                    // Sauvegarde immédiate (statut admin = false par défaut)
                    tokenManager.saveUserInfo(false, displayName != null ? displayName : email, email);

                    // Récupérer le profil pour le vrai statut admin
                    apiService.getProfile().enqueue(new Callback<User>() {
                        @Override
                        public void onResponse(Call<User> call, Response<User> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                User user = response.body();
                                tokenManager.saveUserInfo(user.isStaff(), user.getUsername(), user.getEmail());
                            }
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        }

                        @Override
                        public void onFailure(Call<User> call, Throwable t) {
                            startActivity(new Intent(SignupActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                } else {
                    Toast.makeText(SignupActivity.this, "Erreur échange JWT (code " + response.code() + ")", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(SignupActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    // -------------------------------------------------------------------

    // ====== LE RESTE DE VOTRE CODE RESTE IDENTIQUE ======
    private void performSignup() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm = etConfirmPassword.getText().toString().trim();
        String filiere = spinnerFiliere.getSelectedItem().toString();
        String langue = spinnerLangue.getSelectedItem().toString();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirm)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setFiliere(filiere);
        user.setLangue(langue);

        apiService.signup(user).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                showLoading(false);
                if (response.isSuccessful()) {
                    Toast.makeText(SignupActivity.this, "Compte créé avec succès !", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                } else {
                    Toast.makeText(SignupActivity.this, "Erreur : " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                showLoading(false);
                Toast.makeText(SignupActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSignup.setEnabled(!show);
    }
}