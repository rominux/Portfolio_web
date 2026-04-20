#!/bin/bash
clear
echo -e "\nLe programme compile.sh a été testé uniquement sur les postes de travail de l'IUT \n\n"

echo "=== COMPILATION ==="

javac -encoding UTF8 -cp .:src/ijava2.jar -d src src/*.java

# Vérification si la compilation a réussi
if [ $? -eq 0 ]; then
    echo "✅ Compilation réussie !"
else
    echo "❌ Erreur de compilation."
    echo "faire :\ncd src\nijava compile Treflexion.java"
fi
