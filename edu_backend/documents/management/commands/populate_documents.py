import os
from django.core.management.base import BaseCommand
from django.core.files import File
from documents.models import Document

class Command(BaseCommand):
    help = 'Ajoute les PDF (national/rattrapage) pour toutes les années de 2014-2015 à 2024-2025'

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
    ANNEES = [f"{annee}-{annee+1}" for annee in range(2014, 2025)]
    BASE_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', '..', 'pdfs_import')

    def handle(self, *args, **options):
        created = 0
        for annee in self.ANNEES:
            for filiere, matieres in self.FILIERES.items():
                for matiere in matieres:
                    # Détermination de la langue
                    if "(BIOF)" in matiere:
                        langue = "fr"
                    elif filiere == "Sciences Économiques (ECO)":
                        langue = "fr"
                    else:
                        langue = "ar"

                    for type_doc in self.TYPES:
                        path = os.path.join(self.BASE_DIR, filiere, matiere, annee, f"{type_doc}.pdf")
                        titre = f"{matiere} - {type_doc.capitalize()} ({annee})"

                        doc = Document(
                            titre=titre,
                            filiere=filiere,
                            langue=langue,
                            annee=annee,
                            matiere=matiere,
                            type_document=type_doc,
                        )

                        if os.path.exists(path):
                            with open(path, 'rb') as f:
                                doc.fichier.save(
                                    f"{filiere}_{matiere}_{type_doc}_{annee}.pdf",
                                    File(f)
                                )
                            self.stdout.write(self.style.SUCCESS(f"✅ {titre}"))
                        else:
                            self.stdout.write(self.style.WARNING(f"⚠️ Fichier manquant : {path}"))

                        doc.save()
                        created += 1

        self.stdout.write(self.style.SUCCESS(f"\n🎉 {created} documents créés."))