package com.example.a7san7issa.api;

import com.example.a7san7issa.models.*;
import java.util.List;
import java.util.Map;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    @GET("documents/admin-stats-by-matiere/")
    Call<List<Map<String, Object>>> getStatsByMatiere();

    @Multipart
    @PUT("documents/admin/{id}/")
    Call<Document> updateDocumentWithFile(
            @Path("id") int id,
            @Part("titre") RequestBody titre,
            @Part("filiere") RequestBody filiere,
            @Part("langue") RequestBody langue,
            @Part("annee") RequestBody annee,
            @Part("matiere") RequestBody matiere,
            @Part("type_document") RequestBody typeDocument,
            @Part MultipartBody.Part fichier
    );

    // Dans votre interface ApiService
    @POST("auth/firebase-login/")
    Call<LoginResponse> firebaseLogin(@Body FirebaseTokenRequest tokenRequest);
    @GET("documents/admin-stats-by-filiere/")
    Call<List<Map<String, Object>>> getStatsByFiliere();

    // Auth
    @POST("auth/signup/")
    Call<User> signup(@Body User user);

    @POST("auth/login/")
    Call<LoginResponse> login(@Body Map<String, String> credentials);

    @GET("auth/profile/")
    Call<User> getProfile();

    @Multipart
    @PATCH("auth/profile/")
    Call<User> updateProfile(
            @Part("username") RequestBody username,
            @Part("email") RequestBody email,
            @Part("filiere") RequestBody filiere,
            @Part("langue") RequestBody langue,
            @Part MultipartBody.Part avatar
    );

    // Admin users
    @GET("auth/users/")
    Call<List<User>> getUsers();

    @DELETE("auth/users/{id}/delete/")
    Call<Void> deleteUser(@Path("id") int id);

    @PATCH("auth/users/{id}/update/")
    Call<User> updateUser(@Path("id") int id, @Body Map<String, Object> fields);

    // Documents
    @GET("documents/")
    Call<PaginatedResponse<Document>> getDocuments(@QueryMap Map<String, String> filters);

    @GET("documents/{id}/")
    Call<Document> getDocumentDetail(@Path("id") int id);

    @GET("documents/{id}/download/")
    Call<ResponseBody> downloadDocument(@Path("id") int id);

    @GET("documents/structure/")
    Call<Map<String, Map<String, List<String>>>> getAcademicStructure();

    // Favoris (corrigé pour la pagination)
    @GET("documents/favoris/")
    Call<PaginatedResponse<Favori>> getFavorites();   // ← ICI

    @POST("documents/favoris/")
    Call<Favori> addFavorite(@Body Map<String, Integer> body);

    @DELETE("documents/favoris/{id}/")
    Call<Void> removeFavorite(@Path("id") int id);

    // Historique (reste inchangé si le backend ne le pagine pas)
    @GET("documents/historique/")
    Call<List<Historique>> getHistory();

    // Admin documents
    @Multipart
    @POST("documents/admin/create/")
    Call<Document> createDocument(
            @Part("titre") RequestBody titre,
            @Part("filiere") RequestBody filiere,
            @Part("langue") RequestBody langue,
            @Part("annee") RequestBody annee,
            @Part("matiere") RequestBody matiere,
            @Part("type_document") RequestBody typeDocument,
            @Part MultipartBody.Part fichier
    );

    @PUT("documents/admin/{id}/")
    Call<Document> updateDocument(@Path("id") int id, @Body Document document);

    @DELETE("documents/admin/{id}/")
    Call<Void> deleteDocument(@Path("id") int id);

    // Admin stats
    @GET("documents/admin-stats/")
    Call<Map<String, Object>> getAdminStats();
}