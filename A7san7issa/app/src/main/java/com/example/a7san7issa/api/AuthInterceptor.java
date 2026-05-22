package com.example.a7san7issa.api;

import com.example.a7san7issa.utils.TokenManager;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenManager.getAccessToken();
        if (token != null && !original.url().encodedPath().contains("/auth/login/")) {
            Request.Builder builder = original.newBuilder()
                    .header("Authorization", "Bearer " + token);
            return chain.proceed(builder.build());
        }
        return chain.proceed(original);
    }
}