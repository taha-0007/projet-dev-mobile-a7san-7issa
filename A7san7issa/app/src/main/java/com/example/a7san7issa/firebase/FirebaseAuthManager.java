package com.example.a7san7issa.firebase;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private final FirebaseAuth mAuth;
    private final GoogleSignInClient googleSignInClient;

    public FirebaseAuthManager(Activity activity) {
        mAuth = FirebaseAuth.getInstance();

        // Configurer Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(com.example.a7san7issa.R.string.default_web_client_id)) // Vous devez créer cette ressource (voir note)
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    // Retourne l'intent pour lancer la fenêtre de choix de compte Google
    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    // Appelé depuis onActivityResult après que l'utilisateur ait choisi son compte
    public void handleGoogleSignInResult(Intent data, OnGoogleSignInCompleteListener listener) {
        try {
            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account.getIdToken(), listener);
            } else {
                listener.onFailure(new Exception("Google account is null"));
            }
        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed", e);
            listener.onFailure(e);
        }
    }

    private void firebaseAuthWithGoogle(String idToken, OnGoogleSignInCompleteListener listener) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Récupérer l'ID Token Firebase (pas le même que le token Google)
                            user.getIdToken(true).addOnCompleteListener(tokenTask -> {
                                if (tokenTask.isSuccessful()) {
                                    String firebaseIdToken = tokenTask.getResult().getToken();
                                    listener.onSuccess(firebaseIdToken, user.getEmail(), user.getDisplayName());
                                } else {
                                    listener.onFailure(tokenTask.getException());
                                }
                            });
                        } else {
                            listener.onFailure(new Exception("FirebaseUser is null after auth"));
                        }
                    } else {
                        listener.onFailure(task.getException());
                    }
                });
    }

    // Interface de callback
    public interface OnGoogleSignInCompleteListener {
        void onSuccess(String firebaseIdToken, String email, String displayName);
        void onFailure(Exception e);
    }

    // Déconnexion Firebase (appelé lors de la déconnexion de l'app)
    public void signOut() {
        mAuth.signOut();
        googleSignInClient.signOut();
    }
}