#!/bin/bash

# Vérification des dépendances
if ! command -v asciidoctor &> /dev/null || ! command -v asciidoctor-pdf &> /dev/null; then
  echo "Erreur : asciidoctor ou asciidoctor-pdf n'est pas installé."
  echo "Installez-les avec : gem install --user-install asciidoctor asciidoctor-pdf rouge"
  exit 1
fi

echo "Suppression des anciens rendus..."
rm -f *.html *.pdf

echo "Génération du Tutoriel..."
asciidoctor -r asciidoctor-kroki -a allow-uri-read tutoriel.adoc -o SAE203_Tutoriel.html && echo "SAE203_Tutoriel.html terminée"
asciidoctor-pdf -r asciidoctor-kroki -a allow-uri-read -a pdf-theme=css/mon-theme.yml tutoriel.adoc -o SAE203_Tutoriel.pdf && echo "SAE203_Tutoriel.pdf terminée"

echo "Génération de la FAQ..."
asciidoctor -r asciidoctor-kroki -a allow-uri-read faq.adoc -o SAE203_FAQ.html && echo "SAE203_FAQ.html terminée"
asciidoctor-pdf -r asciidoctor-kroki -a allow-uri-read -a pdf-theme=css/mon-theme.yml faq.adoc -o SAE203_FAQ.pdf && echo "SAE203_FAQ.pdf terminée"

echo "Conversion terminée avec succès ! Vos fichiers finaux sont prêts."
