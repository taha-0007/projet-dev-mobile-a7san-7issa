package com.example.a7san7issa.ui.admin;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DocumentUpdateActivity extends AppCompatActivity {

    private static final int PICK_PDF_REQUEST = 100;

    private TextInputEditText etTitre, etAnnee;
    private Spinner spinnerFiliere, spinnerLangue, spinnerMatiere, spinnerType;
    private MaterialButton btnSelectFile, btnUpdate;
    private TextView tvSelectedFile;
    private ProgressBar progressBar;
    private ApiService apiService;
    private int documentId;
    private Uri selectedFileUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_document);

        TokenManager tokenManager = new TokenManager(this);
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        documentId = getIntent().getIntExtra("document_id", -1);
        if (documentId == -1) {
            Toast.makeText(this, "Document introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupStaticSpinners();
        loadDocumentData();

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Sélectionnez un PDF"), PICK_PDF_REQUEST);
        });

        btnUpdate.setOnClickListener(v -> updateDocument());
    }

    private void initViews() {
        etTitre = findViewById(R.id.etTitre);
        etAnnee = findViewById(R.id.etAnnee);
        spinnerFiliere = findViewById(R.id.spinnerFiliere);
        spinnerLangue = findViewById(R.id.spinnerLangue);
        spinnerMatiere = findViewById(R.id.spinnerMatiere);
        spinnerType = findViewById(R.id.spinnerType);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpdate = findViewById(R.id.btnUpdate);
        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupStaticSpinners() {
        ArrayAdapter<String> langueAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"ar", "fr"});
        langueAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLangue.setAdapter(langueAdapter);

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"national", "rattrapage", "cours"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerType.setAdapter(typeAdapter);
    }

    private void loadDocumentData() {
        apiService.getDocumentDetail(documentId).enqueue(new Callback<Document>() {
            @Override
            public void onResponse(Call<Document> call, Response<Document> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Document doc = response.body();
                    etTitre.setText(doc.getTitre());
                    etAnnee.setText(doc.getAnnee());
                    loadFilieresAndSelect(doc);
                } else {
                    Toast.makeText(DocumentUpdateActivity.this, "Erreur chargement", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Document> call, Throwable t) {
                Toast.makeText(DocumentUpdateActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    // ------------------ méthodes de chargement des spinners ------------------
    private void loadFilieresAndSelect(Document doc) {
        apiService.getAcademicStructure().enqueue(new Callback<Map<String, Map<String, List<String>>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, List<String>>>> call,
                                   Response<Map<String, Map<String, List<String>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> filieres = new ArrayList<>(response.body().keySet());
                    ArrayAdapter<String> filiereAdapter = new ArrayAdapter<>(DocumentUpdateActivity.this,
                            android.R.layout.simple_spinner_item, filieres);
                    filiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerFiliere.setAdapter(filiereAdapter);

                    setSpinnerSelection(spinnerFiliere, doc.getFiliere());
                    loadMatieresForFiliere(doc.getFiliere(), doc.getMatiere());
                    setSpinnerSelection(spinnerLangue, doc.getLangue());
                    setSpinnerSelection(spinnerType, doc.getTypeDocument());
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, List<String>>>> call, Throwable t) {}
        });
    }

    private void loadMatieresForFiliere(String filiere, String selectedMatiere) {
        apiService.getAcademicStructure().enqueue(new Callback<Map<String, Map<String, List<String>>>>() {
            @Override
            public void onResponse(Call<Map<String, Map<String, List<String>>>> call,
                                   Response<Map<String, Map<String, List<String>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, List<String>> filiereData = response.body().get(filiere);
                    if (filiereData != null && filiereData.get("matieres") != null) {
                        List<String> matieres = filiereData.get("matieres");
                        ArrayAdapter<String> matiereAdapter = new ArrayAdapter<>(DocumentUpdateActivity.this,
                                android.R.layout.simple_spinner_item, matieres);
                        matiereAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerMatiere.setAdapter(matiereAdapter);
                        setSpinnerSelection(spinnerMatiere, selectedMatiere);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Map<String, List<String>>>> call, Throwable t) {}
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            tvSelectedFile.setText("Fichier : " + selectedFileUri.getLastPathSegment());
        }
    }

    private void updateDocument() {
        String titre = etTitre.getText().toString().trim();
        String filiere = spinnerFiliere.getSelectedItem() != null ? spinnerFiliere.getSelectedItem().toString() : "";
        String langue = spinnerLangue.getSelectedItem() != null ? spinnerLangue.getSelectedItem().toString() : "";
        String annee = etAnnee.getText().toString().trim();
        String matiere = spinnerMatiere.getSelectedItem() != null ? spinnerMatiere.getSelectedItem().toString() : "";
        String type = spinnerType.getSelectedItem() != null ? spinnerType.getSelectedItem().toString() : "";

        if (titre.isEmpty() || annee.isEmpty() || matiere.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnUpdate.setEnabled(false);

        if (selectedFileUri != null) {
            try {
                RequestBody titreBody = RequestBody.create(MediaType.parse("text/plain"), titre);
                RequestBody filiereBody = RequestBody.create(MediaType.parse("text/plain"), filiere);
                RequestBody langueBody = RequestBody.create(MediaType.parse("text/plain"), langue);
                RequestBody anneeBody = RequestBody.create(MediaType.parse("text/plain"), annee);
                RequestBody matiereBody = RequestBody.create(MediaType.parse("text/plain"), matiere);
                RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), type);

                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
                File tempFile = File.createTempFile("update_pdf", ".pdf", getCacheDir());
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

                apiService.updateDocumentWithFile(documentId, titreBody, filiereBody, langueBody, anneeBody, matiereBody, typeBody, filePart)
                        .enqueue(new Callback<Document>() {
                            @Override
                            public void onResponse(Call<Document> call, Response<Document> response) {
                                progressBar.setVisibility(View.GONE);
                                btnUpdate.setEnabled(true);
                                if (response.isSuccessful()) {
                                    Toast.makeText(DocumentUpdateActivity.this, "Document modifié", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                } else {
                                    Toast.makeText(DocumentUpdateActivity.this, "Erreur lors de la modification", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onFailure(Call<Document> call, Throwable t) {
                                progressBar.setVisibility(View.GONE);
                                btnUpdate.setEnabled(true);
                                Toast.makeText(DocumentUpdateActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                            }
                        });
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE);
                btnUpdate.setEnabled(true);
                Toast.makeText(this, "Erreur de fichier", Toast.LENGTH_SHORT).show();
            }
        } else {
            Document updatedDoc = new Document();
            updatedDoc.setTitre(titre);
            updatedDoc.setFiliere(filiere);
            updatedDoc.setLangue(langue);
            updatedDoc.setAnnee(annee);
            updatedDoc.setMatiere(matiere);
            updatedDoc.setTypeDocument(type);

            apiService.updateDocument(documentId, updatedDoc).enqueue(new Callback<Document>() {
                @Override
                public void onResponse(Call<Document> call, Response<Document> response) {
                    progressBar.setVisibility(View.GONE);
                    btnUpdate.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(DocumentUpdateActivity.this, "Document modifié", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(DocumentUpdateActivity.this, "Erreur modification", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Document> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnUpdate.setEnabled(true);
                    Toast.makeText(DocumentUpdateActivity.this, "Erreur réseau", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}