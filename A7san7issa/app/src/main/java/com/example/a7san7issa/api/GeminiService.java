package com.example.a7san7issa.api;

import com.example.a7san7issa.models.GeminiRequest;
import com.example.a7san7issa.models.GeminiResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface GeminiService {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> sendMessage(
            @Query("key") String apiKey,
            @Body GeminiRequest request
    );
}