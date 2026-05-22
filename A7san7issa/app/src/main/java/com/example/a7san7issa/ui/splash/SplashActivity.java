package com.example.a7san7issa.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.a7san7issa.R;
import com.example.a7san7issa.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.logo);
        TextView slogan = findViewById(R.id.tvSlogan);

        // Animation fade‑in + zoom
        logo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(900)
                .withEndAction(() ->
                        slogan.animate()
                                .alpha(1f)
                                .setDuration(600)
                                .withEndAction(() -> {
                                    new Handler().postDelayed(() -> {
                                        startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                                        finish();
                                    }, 1000);
                                })
                );
    }
}