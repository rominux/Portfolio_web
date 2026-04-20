#!/bin/bash

clear
echo -e "\nLe programme test.sh a été testé uniquement sur les postes de travail de l'IUT \n\n"

cd src
java -cp .:ijava2.jar ijava2.clitools.MainCLI test Treflexion;    