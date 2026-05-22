package com.example.a7san7issa.models;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    public String getReply() {
        if (candidates != null && !candidates.isEmpty()) {
            Candidate candidate = candidates.get(0);
            if (candidate.content != null && candidate.content.parts != null && !candidate.content.parts.isEmpty()) {
                return candidate.content.parts.get(0).text;
            }
        }
        return "Aucune réponse.";
    }

    static class Candidate {
        Content content;
    }

    static class Content {
        List<Part> parts;
    }

    static class Part {
        String text;
    }
}