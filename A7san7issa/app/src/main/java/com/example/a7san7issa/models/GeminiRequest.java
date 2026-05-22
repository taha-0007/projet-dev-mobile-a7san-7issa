package com.example.a7san7issa.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class GeminiRequest {
    @SerializedName("system_instruction")
    private Content systemInstruction;

    @SerializedName("contents")
    private List<Content> contents;

    // Constructeur pour un message texte simple
    public GeminiRequest(String userMessage) {
        this.systemInstruction = new Content(
                "user",
                List.of(new Part("Tu es un assistant éducatif spécialisé pour les étudiants marocains de bacalaureat. Explique toujours en français est en arabe de manière claire et pédagogue. Ne réponds jamais en anglais."))
        );
        Content userContent = new Content("user", List.of(new Part(userMessage)));
        this.contents = List.of(userContent);
    }

    // Constructeur pour un message avec image (base64)
    public GeminiRequest(String userMessage, String imageBase64, String mimeType) {
        this.systemInstruction = new Content(
                "user",
                List.of(new Part("Tu es un assistant éducatif spécialisé pour les étudiants marocains. Explique toujours en français de manière claire et pédagogue. Ne réponds jamais en anglais."))
        );
        Part textPart = new Part(userMessage);
        Part imagePart = new Part(imageBase64, mimeType);
        Content userContent = new Content("user", List.of(textPart, imagePart));
        this.contents = List.of(userContent);
    }

    static class Content {
        String role;
        List<Part> parts;

        Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }
    }

    static class Part {
        String text;
        @SerializedName("inline_data")
        InlineData inlineData;

        // Constructeur pour texte
        Part(String text) {
            this.text = text;
        }

        // Constructeur pour image
        Part(String base64Data, String mimeType) {
            this.inlineData = new InlineData(mimeType, base64Data);
        }

        static class InlineData {
            @SerializedName("mime_type")
            String mimeType;
            @SerializedName("data")
            String data;

            InlineData(String mimeType, String data) {
                this.mimeType = mimeType;
                this.data = data;
            }
        }
    }
}