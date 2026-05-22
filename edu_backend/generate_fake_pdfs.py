import os
from reportlab.pdfgen import canvas

FILIERES = {
    "Sciences Mathématiques A (SM A)": [
        "Mathématiques", "Mathématiques (BIOF)",
        "Physique et Chimie", "Physique et Chimie (BIOF)",
        "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
        "Anglais", "Philosophie"
    ],
    "Sciences Mathématiques B (SM B)": [
        "Mathématiques", "Mathématiques (BIOF)",
        "Physique et Chimie", "Physique et Chimie (BIOF)",
        "Sciences de l’ingénieur",
        "Anglais", "Philosophie"
    ],
    "Sciences Physiques (PC)": [
        "Mathématiques", "Mathématiques (BIOF)",
        "Physique et Chimie", "Physique et Chimie (BIOF)",
        "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
        "Anglais", "Philosophie"
    ],
    "Sciences de la Vie et de la Terre (SVT)": [
        "Mathématiques", "Mathématiques (BIOF)",
        "Physique et Chimie", "Physique et Chimie (BIOF)",
        "Sciences de la Vie et de la Terre (SVT)", "Sciences de la Vie et de la Terre (SVT BIOF)",
        "Anglais", "Philosophie"
    ],
    "Sciences Économiques (ECO)": [
        "Mathématiques appliquées", "Économie générale",
        "Comptabilité", "Gestion", "Anglais", "Philosophie"
    ]
}

TYPES = ["national", "rattrapage"]
BASE_DIR = "pdfs_import"

# Années de 2014-2015 à 2024-2025
ANNEES = [f"{annee}-{annee+1}" for annee in range(2014, 2025)]

def create_pdf(path, titre):
    """Crée un PDF simple avec un titre."""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    c = canvas.Canvas(path)
    c.drawString(100, 750, titre)
    c.save()

total = 0
for annee in ANNEES:
    for filiere, matieres in FILIERES.items():
        for matiere in matieres:
            for type_doc in TYPES:
                dossier = os.path.join(BASE_DIR, filiere, matiere, annee)
                os.makedirs(dossier, exist_ok=True)
                fichier = os.path.join(dossier, f"{type_doc}.pdf")
                titre = f"{matiere} - {type_doc} ({annee})"
                create_pdf(fichier, titre)
                total += 1
                print(f"✅ {fichier}")

print(f"\n🎉 {total} faux PDF ont été générés de 2014-2015 à 2024-2025.")