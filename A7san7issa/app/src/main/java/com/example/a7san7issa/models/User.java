package com.example.a7san7issa.models;

import com.google.gson.annotations.SerializedName;

public class User {
    private int id;
    private String username;
    private String email;
    private String password;      // uniquement pour signup
    private String filiere;
    private String langue;

    @SerializedName("is_staff")
    private boolean isStaff;

    @SerializedName("avatar")
    private String avatar;        // URL relative (ex: /media/avatars/monimage.jpg)

    // Getters et setters existants...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    public String getLangue() { return langue; }
    public void setLangue(String langue) { this.langue = langue; }
    public boolean isStaff() { return isStaff; }
    public void setStaff(boolean staff) { isStaff = staff; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}