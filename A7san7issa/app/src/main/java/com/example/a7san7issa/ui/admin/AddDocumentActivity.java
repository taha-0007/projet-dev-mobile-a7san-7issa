package com.example.a7san7issa.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.models.Document;
import com.example.a7san7issa.utils.TokenManager;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddDocumentActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 1;

    private TextInputEditText etTitre, etAnnee;
    private Spinner spinnerFiliere, spinnerLangue, spinnerMatiere, spinnerType;
    private Button btnSelectFile, btnSubmit;
    private TextView tvSelectedFile;
    private ProgressBar progressBar;
    private Uri selectedFileUri;
    private ApiService apiService;

    // Stocker la structure académique complète
    private Map<String, List<String>> matieresParFiliere = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_document);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        initViews();
        setupSpinners();
        setupListeners();
        loadAcademicStructure();
    }

    private void initViews() {
        etTitre = findViewById(R.id.etTitre);
        etAnnee = findViewById(R.id.etAnnee);
        spinnerFiliere = findViewById(R.id.spinnerFiliere);
        spinnerLangue = findViewById(R.id.spinnerLangue);
        spinnerMatiere = findViewById(R.id.spinnerMatiere);
        spinnerType = findViewById(R.id.spinnerType);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSubmit = findViewById(R.id.btnSubmit);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupSpinners() {
        // Langues
        String[] langues = {"ar", "fr"};
        ArrayAdapter<String> langueAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, langues);
        langueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangue.setAdapter(langueAdapter);

        // Types
        String[] types = {"cours", "national", "rattrapage"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);

        // Filières (chargées depuis la structure)
        ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFiliere.setAdapter(filiereAdapter);

        // Matières (dépend de la filière)
        ArrayAdapter<String> matiereAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>());
        matiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMatiere.setAdapter(matiereAdapter);
    }

    private void setupListeners() {
        // Sélection fichier
        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Sélectionnez un PDF"), PICK_PDF_REQUEST);
        });

        // Envoi
        btnSubmit.setOnClickListener(v -> uploadDocument());

        // Changement de filière → mise à jour des matières
        spinnerFiliere.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String filiere = parent.getItemAtPosition(position).toString();
                List<String> matieres = matieresParFiliere.get(filiere);
                if (matieres != null && !matieres.isEmpty()) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(AddDocumentActivity.this,
                            android.R.layout.simple_spinner_item, matieres);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerMatiere.setAdapter(adapter);
                } else {
                    // Aucune matière pour cette filière
                    spinnerMatiere.setAdapter(new ArrayAdapter<>(AddDocumentActivity.this,
                            android.R.layout.simple_spinner_item, new ArrayList<>()));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * Récupère la structure académique depuis le backend
     */
    private void loadAcademicStructure() {
        apiService.getAcademicStructure().enqueue(new Callback<Map<String, Map<String, List<String>>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, List<String>>>> call,
                                   Response<Map<String, Map<String, List<String>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Map<String, List<String>>> structure = response.body();
                    List<String> filieres = new ArrayList<>(structure.keySet());
                    matieresParFiliere.clear();
                    for (Map.Entry<String, Map<String, List<String>>> entry : structure.entrySet()) {
                        matieresParFiliere.put(entry.getKey(), entry.getValue().get("matieres"));
                    }

                    // Mettre à jour le spinner des filières
                    ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(AddDocumentActivity.this,
                            android.R.layout.simple_spinner_item, filieres);
                    filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFiliere.setAdapter(filiereAdapter);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, List<String>>>> call, Throwable t) {
                Toast.makeText(AddDocumentActivity.this, "Erreur chargement structure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            tvSelectedFile.setText("Fichier : " + selectedFileUri.getLastPathSegment());
        }
    }

    private void uploadDocument() {
        String titre = etTitre.getText().toString().trim();
        String annee = etAnnee.getText().toString().trim();
        String matiere = spinnerMatiere.getSelectedItem() != null ?
                spinnerMatiere.getSelectedItem().toString() : "";
        String filiere = spinnerFiliere.getSelectedItem() != null ?
                spinnerFiliere.getSelectedItem().toString() : "";
        String langue = spinnerLangue.getSelectedItem() != null ?
                spinnerLangue.getSelectedItem().toString() : "";
        String type = spinnerType.getSelectedItem() != null ?
                spinnerType.getSelectedItem().toString() : "";

        if (titre.isEmpty() || annee.isEmpty() || matiere.isEmpty() || filiere.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedFileUri == null) {
            Toast.makeText(this, "Veuillez sélectionner un fichier PDF", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        try {
            RequestBody titreBody = RequestBody.create(MediaType.parse("text/plain"), titre);
            RequestBody filiereBody = RequestBody.create(MediaType.parse("text/plain"), filiere);
            RequestBody langueBody = RequestBody.create(MediaType.parse("text/plain"), langue);
            RequestBody anneeBody = RequestBody.create(MediaType.parse("text/plain"), annee);
            RequestBody matiereBody = RequestBody.create(MediaType.parse("text/plain"), matiere);
            RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), type);

            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            File tempFile = File.createTempFile("upload", ".pdf", getCacheDir());
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.close();
            inputStream.close();

            RequestBody fileBody = RequestBody.create(MediaType.parse("application/pdf"), tempFile);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("fichier", "document.pdf", fileBody);

            apiService.createDocument(titreBody, filiereBody, langueBody, anneeBody, matiereBody, typeBody, filePart)
                    .enqueue(new Callback<Document>() {
                        @Override
                        public void onResponse(Call<Document> call, Response<Document> response) {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            if (response.isSuccessful()) {
                                Toast.makeText(AddDocumentActivity.this, "Document ajouté avec succès !", Toast.LENGTH_SHORT).show();
                                setResult(RESULT_OK);
                                finish();
                            } else {
                                Toast.makeText(AddDocumentActivity.this, "Erreur lors de l'ajout", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Document> call, Throwable t) {
                            progressBar.setVisibility(View.GONE);
                            btnSubmit.setEnabled(true);
                            Toast.makeText(AddDocumentActivity.this, "Erreur réseau : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Erreur de fichier : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}