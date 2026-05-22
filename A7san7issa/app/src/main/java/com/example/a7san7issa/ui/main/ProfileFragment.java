package com.example.a7san7issa.ui.main;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.User;
import com.example.a7san7issa.ui.admin.AdminMainActivity;
import com.example.a7san7issa.ui.auth.LoginActivity;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private TextInputEditText etUsername, etEmail;
    private ImageView ivAvatar;
    private Spinner spinnerFiliere, spinnerLangue;
    private MaterialButton btnSave, btnAdmin, btnLogout;
    private ProgressBar progressBar;
    private ApiService apiService;
    private TokenManager tokenManager;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private File selectedImageFile;   // fichier à envoyer

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        etUsername = v.findViewById(R.id.etUsername);
        etEmail = v.findViewById(R.id.etEmail);
        ivAvatar = v.findViewById(R.id.ivAvatar);
        spinnerFiliere = v.findViewById(R.id.spinnerFiliere);
        spinnerLangue = v.findViewById(R.id.spinnerLangue);
        btnSave = v.findViewById(R.id.btnSave);
        btnAdmin = v.findViewById(R.id.btnAdmin);
        btnLogout = v.findViewById(R.id.btnLogout);
        progressBar = v.findViewById(R.id.progressBar);

        // Spinners
        String[] filieres = {"SM A", "SM B", "PC", "SVT", "ECO"};
        ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, filieres);
        filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiliere.setAdapter(filiereAdapter);

        String[] langues = {"ar", "fr"};
        ArrayAdapter<String> langueAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, langues);
        langueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangue.setAdapter(langueAdapter);

        btnSave.setOnClickListener(view -> saveProfile());

        btnAdmin.setVisibility(tokenManager.isStaff() ? View.VISIBLE : View.GONE);
        btnAdmin.setOnClickListener(view -> startActivity(new Intent(getActivity(), AdminMainActivity.class)));

        btnLogout.setOnClickListener(view -> {
            tokenManager.clearTokens();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });

        // Sélection de la photo de profil
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            String savedPath = copyImageToInternalStorage(imageUri);
                            if (savedPath != null) {
                                selectedImageFile = new File(savedPath);
                                // Aperçu local immédiat
                                Glide.with(ProfileFragment.this)
                                        .load(selectedImageFile)
                                        .placeholder(R.mipmap.ic_launcher)
                                        .circleCrop()
                                        .into(ivAvatar);
                                Toast.makeText(getContext(), "Photo prête, enregistrez le profil", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });

        ivAvatar.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImageLauncher.launch(intent);
        });

        loadProfile();
        return v;
    }

    private String copyImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            String fileName = "profile_" + tokenManager.getUsername() + ".jpg";
            File outputFile = new File(requireContext().getFilesDir(), fileName);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadProfile() {
        apiService.getProfile().enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    User user = response.body();
                    etUsername.setText(user.getUsername());
                    etEmail.setText(user.getEmail());
                    setSpinnerSelection(spinnerFiliere, user.getFiliere());
                    setSpinnerSelection(spinnerLangue, user.getLangue());

                    // Priorité : URL sauvegardée > URL serveur
                    String avatarUrl = tokenManager.getProfileImagePath();
                    if (avatarUrl == null || avatarUrl.isEmpty()) {
                        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                            avatarUrl = user.getAvatar();
                            if (!avatarUrl.startsWith("http")) {
                                avatarUrl = "http://10.0.2.2:8000" + avatarUrl;
                            }
                            tokenManager.saveProfileImagePath(avatarUrl);
                        }
                    }

                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        Glide.with(ProfileFragment.this)
                                .load(avatarUrl)
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
            public void onFailure(Call<User> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur chargement profil", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String filiere = spinnerFiliere.getSelectedItem().toString();
        String langue = spinnerLangue.getSelectedItem().toString();

        if (username.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSave.setEnabled(false);

        RequestBody usernameBody = RequestBody.create(MediaType.parse("text/plain"), username);
        RequestBody emailBody = RequestBody.create(MediaType.parse("text/plain"), email);
        RequestBody filiereBody = RequestBody.create(MediaType.parse("text/plain"), filiere);
        RequestBody langueBody = RequestBody.create(MediaType.parse("text/plain"), langue);

        MultipartBody.Part avatarPart = null;
        if (selectedImageFile != null) {
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), selectedImageFile);
            avatarPart = MultipartBody.Part.createFormData("avatar", "profile.jpg", fileBody);
        }

        apiService.updateProfile(usernameBody, emailBody, filiereBody, langueBody, avatarPart)
                .enqueue(new Callback<User>() {
                    @Override
                    public void onResponse(Call<User> call, Response<User> response) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null) {
                            User user = response.body();
                            if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                                String avatarUrl = user.getAvatar();
                                if (!avatarUrl.startsWith("http")) {
                                    avatarUrl = "http://10.0.2.2:8000" + avatarUrl;
                                }
                                tokenManager.saveProfileImagePath(avatarUrl);
                                Glide.with(ProfileFragment.this)
                                        .load(avatarUrl)
                                        .placeholder(R.mipmap.ic_launcher)
                                        .circleCrop()
                                        .into(ivAvatar);
                            }
                            Toast.makeText(getContext(), "Profil mis à jour", Toast.LENGTH_SHORT).show();

                            if (getActivity() instanceof MainActivity) {
                                ((MainActivity) getActivity()).updateDrawerProfile(
                                        username, email, tokenManager.getProfileImagePath());
                            }
                        } else {
                            Toast.makeText(getContext(), "Erreur lors de la mise à jour", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<User> call, Throwable t) {
                        progressBar.setVisibility(View.GONE);
                        btnSave.setEnabled(true);
                        Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        for (int i = 0; i < spinner.getCount(); i++) {
            if (spinner.getItemAtPosition(i).toString().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }
}