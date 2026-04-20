#!/bin/bash
clear
echo -e "\nLe programme run.sh a été testé uniquement sur les postes de travail de l'IUT \n\n"
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mate-terminal --working-directory="$DIR" --zoom=0.6 --full-screen --hide-menubar --command="bash -c '
    cd src; 
    java -cp .:ijava2.jar ijava2.clitools.MainCLI execute Treflexion;    
    exit
'" 2>/dev/null

# Si la commande précédente a échoué (code de retour différent de 0), on affiche le message d'aide
if [ $? -ne 0 ]; then
    echo -e "\033[0;31m>> Erreur : Environnement IUT non détecté (mate-terminal introuvable).\033[0m"
    echo ""
    echo "Pour lancer le jeu manuellement, veuillez suivre ces étapes :"
    echo "1. cd src"
    echo "2. ijava execute Treflexion"
    echo "3. Dézoomez le terminal jusqu'à ce que l'affichage soit correct"
    echo ""
fi