package com.example.a7san7issa.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.a7san7issa.R;
import com.example.a7san7issa.ui.main.MainActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Administration");
        }

        BottomNavigationView nav = findViewById(R.id.bottom_nav_admin);
        nav.setOnNavigationItemSelectedListener(item -> {
            Fragment f = null;
            int id = item.getItemId();
            if (id == R.id.nav_admin_docs) {
                f = new AdminDocumentsFragment();
            } else if (id == R.id.nav_admin_users) {
                f = new AdminUsersFragment();
            } else if (id == R.id.nav_admin_stats) {
                f = new AdminStatsFragment();
            } else if (id == R.id.nav_home) {
                // Retour à l'accueil principal
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            }
            if (f != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.admin_fragment_container, f).commit();
            }
            return true;
        });
        nav.setSelectedItemId(R.id.nav_admin_docs);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}