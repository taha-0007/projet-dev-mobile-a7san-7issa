package com.example.a7san7issa.ui.document;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.utils.TokenManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PdfViewerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        ImageView imageView = findViewById(R.id.imageView);
        String pdfUrl = getIntent().getStringExtra("pdf_url");
        String titre = getIntent().getStringExtra("titre");
        int documentId = getIntent().getIntExtra("document_id", -1);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(titre != null ? titre : "PDF");
        }

        // Vérification immédiate de l'URL
        if (pdfUrl == null || pdfUrl.isEmpty()) {
            Toast.makeText(this, "Ce document n’a pas encore de fichier PDF.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Appel API pour l’historique (silencieux)
        if (documentId != -1) {
            TokenManager tokenManager = new TokenManager(this);
            ApiService apiService = ApiClient.getClient(tokenManager).create(ApiService.class);
            apiService.getDocumentDetail(documentId).enqueue(new Callback<com.example.a7san7issa.models.Document>() {
                @Override
                public void onResponse(Call<com.example.a7san7issa.models.Document> call, Response<com.example.a7san7issa.models.Document> response) {}
                @Override
                public void onFailure(Call<com.example.a7san7issa.models.Document> call, Throwable t) {}
            });
        }

        new Thread(() -> {
            try {
                URL url = new URL(pdfUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();
                InputStream inputStream = connection.getInputStream();

                File tempFile = File.createTempFile("temp_pdf", ".pdf", getCacheDir());
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }
                outputStream.close();
                inputStream.close();

                PdfRenderer renderer = new PdfRenderer(
                        ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY));
                PdfRenderer.Page page = renderer.openPage(0);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                renderer.close();

                runOnUiThread(() -> imageView.setImageBitmap(bitmap));
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(PdfViewerActivity.this,
                                "Impossible d’ouvrir le PDF (vérifiez votre connexion).", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}