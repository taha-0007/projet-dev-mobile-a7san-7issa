package com.example.a7san7issa.ui.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.User;
import com.example.a7san7issa.ui.auth.LoginActivity;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private TokenManager tokenManager;
    private ApiService apiService;
    private ImageView ivAvatar;
    private TextView tvUsernameMenu, tvEmailMenu;
    private Switch switchDarkMode;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        tokenManager = new TokenManager(this);

        String currentUsername = tokenManager.getUsername();
        if (currentUsername == null || currentUsername.isEmpty()) {
            currentUsername = "default";
        }
        final String finalCurrentUsername = currentUsername;

        boolean isDark = prefs.getBoolean("dark_mode_" + finalCurrentUsername, false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.open_drawer, R.string.close_drawer);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        ivAvatar = headerView.findViewById(R.id.ivAvatar);
        tvUsernameMenu = headerView.findViewById(R.id.tvUsernameMenu);
        tvEmailMenu = headerView.findViewById(R.id.tvEmailMenu);

        // Footer du drawer
        View footerView = findViewById(R.id.footer_nav);
        if (footerView != null) {
            switchDarkMode = footerView.findViewById(R.id.switchDarkMode);
            if (switchDarkMode != null) {
                switchDarkMode.setChecked(isDark);
                switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean("dark_mode_" + finalCurrentUsername, isChecked).apply();
                    AppCompatDelegate.setDefaultNightMode(
                            isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
                });
            }

            footerView.findViewById(R.id.btnDrawerLogout).setOnClickListener(v -> {
                tokenManager.clearTokens();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
            });
        }

        loadProfileForDrawer();

        if (savedInstanceState == null) {
            navigateTo(new HomeFragment(), "Accueil");
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    private void loadProfileForDrawer() {
        apiService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    tvUsernameMenu.setText(user.getUsername());
                    tvEmailMenu.setText(user.getEmail());

                    // 1. URL en cache
                    String imagePath = tokenManager.getProfileImagePath();
                    // 2. URL depuis l'API (fallback)
                    if (imagePath == null || imagePath.isEmpty()) {
                        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            imagePath = user.getAvatar();
                            if (!imagePath.startsWith("http")) {
                                imagePath = "http://10.0.2.2:8000" + imagePath;
                            }
                            tokenManager.saveProfileImagePath(imagePath);
                        }
                    }

                    // Affichage avec Glide
                    if (imagePath != null && !imagePath.isEmpty()) {
                        Glide.with(MainActivity.this)
                                .load(imagePath)
                                .placeholder(R.mipmap.ic_launcher)
                                .error(R.mipmap.ic_launcher)
                                .circleCrop()
                                .into(ivAvatar);
                    } else {
                        ivAvatar.setImageResource(R.mipmap.ic_launcher);
                    }
                }
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) { }
        });
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;
        String title = "A7san 7issa";
        int id = item.getItemId();

        if (id == R.id.nav_home) { fragment = new HomeFragment(); title = "Accueil"; }
        else if (id == R.id.nav_documents) { fragment = new DocumentsFragment(); title = "Documents"; }
        else if (id == R.id.nav_ai) { fragment = new AIFragment(); title = "Assistant IA"; }
        else if (id == R.id.nav_favorites) { fragment = new FavoritesFragment(); title = "Favoris"; }
        else if (id == R.id.nav_maps) { fragment = new MapsFragment(); title = "Bibliothèques"; }
        else if (id == R.id.nav_profile) { fragment = new ProfileFragment(); title = "Profil"; }

        if (fragment != null) {
            navigateTo(fragment, title);
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void navigateTo(Fragment fragment, String title) {
        getSupportFragmentManager()
                .beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragment_container, fragment)
                .commit();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
        }
    }

    public void updateDrawerProfile(String username, String email, String imagePath) {
        if (tvUsernameMenu != null) {
            tvUsernameMenu.setText(username);
        }
        if (tvEmailMenu != null) {
            tvEmailMenu.setText(email);
        }
        if (ivAvatar != null) {
            if (imagePath != null && !imagePath.isEmpty()) {
                Glide.with(this)
                        .load(imagePath)
                        .placeholder(R.mipmap.ic_launcher)
                        .error(R.mipmap.ic_launcher)
                        .circleCrop()
                        .into(ivAvatar);
            } else {
                ivAvatar.setImageResource(R.mipmap.ic_launcher);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}