package com.example.a7san7issa.ui.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.a7san7issa.R;
import com.example.a7san7issa.adapters.ChatAdapter;
import com.example.a7san7issa.api.GeminiService;
import com.example.a7san7issa.data.ChatDatabase;
import com.example.a7san7issa.data.ChatDao;
import com.example.a7san7issa.data.ChatMessageEntity;
import com.example.a7san7issa.models.GeminiRequest;
import com.example.a7san7issa.models.GeminiResponse;
import com.example.a7san7issa.utils.TokenManager;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AIFragment extends Fragment {

    private RecyclerView chatRecyclerView;
    private EditText etMessage;
    private ImageButton btnSend, btnCamera, btnPdf, btnMicro;
    private ChatAdapter chatAdapter;
    private GeminiService geminiService;
    private List<ChatAdapter.ChatMessage> messages = new ArrayList<>();
    private static final String GEMINI_API_KEY = "AIzaSyCnMv1xWpdDFvzdrmsKUdrRo4Vs6sGjyiY"; // ⚠️ À régénérer !
    private static final String TAG = "AIFragment";
    private ChatDao chatDao;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private String currentUserId;

    // Launchers
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_ai, container, false);

        // Identifiant utilisateur
        TokenManager tokenManager = new TokenManager(requireContext());
        currentUserId = tokenManager.getUsername();
        if (currentUserId.isEmpty()) {
            currentUserId = "anonymous";
        }

        // Room
        chatDao = ChatDatabase.getInstance(requireContext()).chatDao();

        // ----------  Timeout fix + Logging ----------
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient geminiClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();

        geminiService = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .client(geminiClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GeminiService.class);
        // -------------------------------------------

        chatRecyclerView = v.findViewById(R.id.chatRecyclerView);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        chatAdapter = new ChatAdapter(messages);
        chatRecyclerView.setAdapter(chatAdapter);

        etMessage = v.findViewById(R.id.etMessage);
        btnSend = v.findViewById(R.id.btnSend);
        btnCamera = v.findViewById(R.id.btnCamera);
        btnPdf = v.findViewById(R.id.btnPdf);
        btnMicro = v.findViewById(R.id.btnMicro);

        loadHistory();

        btnSend.setOnClickListener(view -> sendTextMessage());

        // Image picker (galerie)
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        handleImageUri(imageUri);
                    }
                });

        // Camera
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Bundle extras = result.getData().getExtras();
                        if (extras != null) {
                            Bitmap bitmap = (Bitmap) extras.get("data");
                            if (bitmap != null) {
                                sendImageToAI(bitmap, "image/jpeg");
                            }
                        }
                    }
                });

        // PDF
        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri pdfUri = result.getData().getData();
                        extractTextFromPdf(pdfUri);
                    }
                });

        // Voice
        speechRecognizerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        if (results != null && !results.isEmpty()) {
                            etMessage.setText(results.get(0));
                        }
                    }
                });

        btnCamera.setOnClickListener(view -> {
            Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(pickIntent);
        });

        btnPdf.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("application/pdf");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            pdfPickerLauncher.launch(intent);
        });

        btnMicro.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, 100);
            } else {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parlez...");
                speechRecognizerLauncher.launch(intent);
            }
        });

        return v;
    }

    private void loadHistory() {
        executor.execute(() -> {
            List<ChatMessageEntity> entities = chatDao.getMessagesByUser(currentUserId);
            requireActivity().runOnUiThread(() -> {
                messages.clear();
                for (ChatMessageEntity entity : entities) {
                    boolean isUser = entity.role.equals("user");
                    messages.add(new ChatAdapter.ChatMessage(entity.content, isUser));
                }
                chatAdapter.notifyDataSetChanged();
                if (!messages.isEmpty()) {
                    chatRecyclerView.scrollToPosition(messages.size() - 1);
                } else {
                    messages.add(new ChatAdapter.ChatMessage("Bonjour ! Posez-moi une question sur vos cours.", false));
                    chatAdapter.notifyDataSetChanged();
                }
            });
        });
    }

    private void sendTextMessage() {
        String text = etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        addMessage(text, true);
        etMessage.setText("");

        GeminiRequest request = new GeminiRequest(text);
        callGemini(request);
    }

    private void handleImageUri(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            if (bitmap != null) {
                sendImageToAI(bitmap, "image/jpeg");
            } else {
                Toast.makeText(getContext(), "Impossible de charger l'image", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendImageToAI(Bitmap bitmap, String mimeType) {
        int maxSize = 1024;
        if (bitmap.getWidth() > maxSize || bitmap.getHeight() > maxSize) {
            float ratio = (float) bitmap.getWidth() / bitmap.getHeight();
            int newW, newH;
            if (bitmap.getWidth() > bitmap.getHeight()) {
                newW = maxSize;
                newH = (int) (maxSize / ratio);
            } else {
                newH = maxSize;
                newW = (int) (maxSize * ratio);
            }
            bitmap = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        String base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);

        String userMessage = etMessage.getText().toString().trim();
        if (userMessage.isEmpty()) userMessage = "Analyse cette image.";

        addMessage("[Image] " + userMessage, true);
        etMessage.setText("");

        GeminiRequest request = new GeminiRequest(userMessage, base64Image, mimeType);
        callGemini(request);
    }

    private void extractTextFromPdf(Uri pdfUri) {
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(pdfUri);
            android.graphics.pdf.PdfRenderer renderer = new android.graphics.pdf.PdfRenderer(
                    android.os.ParcelFileDescriptor.open(
                            new java.io.File(pdfUri.getPath()),
                            android.os.ParcelFileDescriptor.MODE_READ_ONLY));
            if (renderer.getPageCount() > 0) {
                android.graphics.pdf.PdfRenderer.Page page = renderer.openPage(0);
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();
                renderer.close();

                InputImage image = InputImage.fromBitmap(bitmap, 0);
                recognizer.process(image)
                        .addOnSuccessListener(visionText -> {
                            StringBuilder extractedText = new StringBuilder();
                            for (com.google.mlkit.vision.text.Text.TextBlock block : visionText.getTextBlocks()) {
                                extractedText.append(block.getText());
                                extractedText.append("\n");
                            }
                            String result = extractedText.toString().trim();
                            if (!result.isEmpty()) {
                                addMessage("[Texte extrait du PDF] : " + result, true);
                                GeminiRequest request = new GeminiRequest(
                                        "Voici le texte d'un PDF : " + result + " . Explique-moi ou résume.");
                                callGemini(request);
                            } else {
                                Toast.makeText(getContext(), "Aucun texte détecté dans le PDF", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(),
                                "Échec OCR : " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(getContext(), "Le PDF est vide", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erreur lecture PDF : " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void callGemini(GeminiRequest request) {
        geminiService.sendMessage(GEMINI_API_KEY, request).enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String reply = response.body().getReply();
                    addMessage(reply, false);
                } else {
                    String errorMsg;
                    int code = response.code();
                    Log.e(TAG, "onResponse error: " + code + " " + response.message());
                    if (code == 429) {
                        errorMsg = "Trop de requêtes, patientez quelques secondes.";
                    } else if (code >= 500) {
                        errorMsg = "Le service est temporairement indisponible.";
                    } else if (code == 400) {
                        errorMsg = "Requête invalide. Essayez de reformuler.";
                    } else {
                        errorMsg = "Erreur IA (code " + code + ")";
                    }
                    addMessage(errorMsg, false);
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e(TAG, "onFailure", t);  // stacktrace complet pour debug
                String msg;
                if (t instanceof java.net.SocketTimeoutException) {
                    msg = "L'IA met trop de temps à répondre, réessayez.";
                } else if (t instanceof java.net.UnknownHostException || t instanceof java.net.ConnectException) {
                    msg = "Pas de connexion Internet. Vérifiez votre réseau.";
                } else {
                    msg = "Erreur réseau : " + t.getMessage();
                }
                addMessage(msg, false);
            }
        });
    }

    private void addMessage(String content, boolean isUser) {
        ChatAdapter.ChatMessage msg = new ChatAdapter.ChatMessage(content, isUser);
        messages.add(msg);
        chatAdapter.notifyItemInserted(messages.size() - 1);
        chatRecyclerView.scrollToPosition(messages.size() - 1);

        String role = isUser ? "user" : "assistant";
        executor.execute(() -> {
            chatDao.insertMessage(new ChatMessageEntity(currentUserId, role, content, System.currentTimeMillis()));
        });
    }
}