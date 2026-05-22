package com.example.a7san7issa.models;

import com.google.gson.annotations.SerializedName;

public class Document {
    private int id;
    private String titre;
    private String filiere;
    private String langue;
    private String annee;
    private String matiere;

    @SerializedName("type_document")
    private String typeDocument;

    private String fichier;
    private int vues;
    private int telechargements;

    // Getters existants…
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getFiliere() { return filiere; }
    public String getLangue() { return langue; }
    public String getAnnee() { return annee; }
    public String getMatiere() { return matiere; }
    public String getTypeDocument() { return typeDocument; }
    public String getFichier() { return fichier; }
    public int getVues() { return vues; }
    public int getTelechargements() { return telechargements; }

    // Setters (ajoutés)
    public void setTitre(String titre) { this.titre = titre; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    public void setLangue(String langue) { this.langue = langue; }
    public void setAnnee(String annee) { this.annee = annee; }
    public void setMatiere(String matiere) { this.matiere = matiere; }
    public void setTypeDocument(String typeDocument) { this.typeDocument = typeDocument; }
}