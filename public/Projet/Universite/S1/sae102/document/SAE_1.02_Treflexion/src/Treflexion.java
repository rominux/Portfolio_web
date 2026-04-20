import extensions.File;
import extensions.CSVFile;

class Treflexion extends Program {
    // ========================================================================================================================
    // CONSTANTES
    // ========================================================================================================================
    final int V = 11;
    final int D = 12;
    final int R = 13;
    final int A = 14;
    final int NB_JOKERS_MAX = 15;

    final String[] LISTE_COULEUR = new String[]{"♦", "♥", "♣", "♠"};
    final String[] LISTE_VALEUR = new String[]{"", "", "2", "3", "4", "5", "6", "7", "8", "9", "10", "V", "D", "R", "A"};

    // PARAMETRES HISTORIQUE
    final int COL_HISTORIQUE = 162; 
    final int LIG_HISTORIQUE_START = 6;
    final int NB_LIGNES_HISTO = 35; 

    // GLOBALES
    String seedActuelle;
    long seedNumber;
    Question[] baseDeQuestions;
    String[] historiqueJeu; 

    // Variables pour l'affichage persistant
    String derniereQuestionEnonce = "";
    String derniereReponseJusteLettre = "";
    String derniereReponseJusteTexte = ""; 
    String derniereReponseJoueurLettre = "";
    String derniereReponseJoueurTexte = "";
    String derniereExplication = "";

    // ========================================================================================================================
    // ALGORITHME PRINCIPAL
    // ========================================================================================================================
    
    // Point d'entrée du programme. Gère le menu principal et la boucle globale de l'application.
    void algorithm() {
        chargerQuestions();
        boolean continuer = true;
        while (continuer) {
            clear();
            afficherPageAccueuil();

            println("\n=== MENU PRINCIPAL ===");
            println("1. Lire les règles");
            println("2. Nouvelle Partie (Aléatoire)");
            println("3. Charger une Partie (Via Seed)");
            println("4. Voir le Leaderboard");
            println("5. Quitter");

            int choix = lireEntier("Votre choix : ", 1, 5);

            if (choix == 1) {
                afficherRegles();
            } else if (choix == 2) {
                String seed = genererRandomSeedString(10);
                lancerPartieAvecSeed(seed);
            } else if (choix == 3) {
                print("Entrez la seed : ");
                String seed = readString();
                lancerPartieAvecSeed(seed);
            } else if (choix == 4) {
                afficherEcranLeaderboardMenu();
            } else if (choix == 5) {
                continuer = false;
                println("Au revoir !");
            }
        }
    }

    // ========================================================================================================================
    // LOGIQUE JEU
    // ========================================================================================================================

    // Crée et initialise un tableau vide pour stocker les messages de l'historique du jeu.
    String[] initialiserHistorique() {
        String[] h = new String[NB_LIGNES_HISTO];
        for (int i = 0; i < length(h); i += 1) {
            h[i] = "";
        }
        return h;
    }

    // Recalcule les scores de toutes les lignes et colonnes et met à jour les tableaux correspondants.
    void mettreAJourScores(Carte[][] grille, int[] ptsLignes, int[] ptsCols) {
        for (int i = 0; i < 5; i += 1) {
            ptsLignes[i] = calculerScoreLigne(grille, i);
        }
        for (int j = 0; j < 5; j += 1) {
            ptsCols[j] = calculerScoreColonne(grille, j);
        }
    }

    // Vérifie si une case est libre et y place la carte si c'est le cas. Retourne true si succès.
    boolean tenterPlacerCarte(Carte[][] grille, int lig, int col, Carte c) {
        if (grille[lig][col] == null) {
            grille[lig][col] = c;
            return true;
        }
        return false;
    }

    // Gère la fin de partie : calcul du score total, affichage final et sauvegarde du score si demandé.
    void gererFinDePartie(Carte[][] grille, int[] ptsLignesFin, int[] ptsColsFin) {
        int scoreTotal = 0;
        mettreAJourScores(grille, ptsLignesFin, ptsColsFin);
        
        for (int i = 0; i < 5; i += 1) {
            scoreTotal += ptsLignesFin[i];
        }
        for (int j = 0; j < 5; j += 1) {
            scoreTotal += ptsColsFin[j];
        }

        afficherEcranResultat(grille, ptsLignesFin, ptsColsFin, scoreTotal, "", "", "");

        println("\n=== PARTIE TERMINÉE ===");
        print("Voulez-vous enregistrer votre score ? (o/n) : ");
        String rep = readString();
        
        String pseudoFinal = "";
        String msgSucces = "";

        if (!equals(rep, "n") && !equals(rep, "N")) {
            print("Entrez votre pseudo : ");
            String saisiePseudo = readString();
            pseudoFinal = nettoyerPseudo(saisiePseudo);

            sauvegarderNouveauScore(pseudoFinal, scoreTotal, seedActuelle);
            msgSucces = GREEN + "Score enregistré avec succès (" + pseudoFinal + ") !" + RESET + WHITE + BG_BLACK;
        }
        
        if (length(msgSucces) > 0) {
            afficherEcranResultat(grille, ptsLignesFin, ptsColsFin, scoreTotal, msgSucces, pseudoFinal, seedActuelle);
        }
        
        println("\n=== FIN DE PARTIE ===");
        println("Seed : " + seedActuelle);
        println("Appuyez sur Entrée...");
        readString();
    }

    // Lance une question Joker. Si le joueur répond juste, ajoute un message à l'historique et retourne true.
    boolean jouerSequenceJoker(Carte cActuelle) {
        boolean gagne = poserQuestion();
        if (gagne) {
            ajouterHistorique(GREEN + "Joker : " + nomCarte(cActuelle) + " défaussée." + RESET + WHITE + BG_BLACK);
            return true;
        }
        return false;
    }

    // Vérifie si la valeur saisie correspond à une erreur (hors limites ou invalide) et retourne le message d'erreur associé.
    String verifierErreurSaisie(int valeur, String type) {
        if (valeur == -3) {
            return RED + ">> ERREUR : " + type + " hors limites (1-5) !" + RESET + WHITE + BG_BLACK;
        }
        if (valeur == -1) {
            return RED + ">> ERREUR : Saisie invalide !" + RESET + WHITE + BG_BLACK;
        }
        return "";
    }

    // Tente de placer une carte aux coordonnées indiquées. Met à jour l'historique en cas de succès ou de ligne complétée.
    boolean traiterPlacement(Carte[][] grille, int lig, int col, Carte cActuelle) {
        boolean place = tenterPlacerCarte(grille, lig, col, cActuelle);
        if (place) {
            ajouterHistorique("Posé " + nomCarte(cActuelle) + " en (" + (lig + 1) + "," + (col + 1) + ")");
            if (estLigneComplete(grille, lig)) {
                ajouterHistorique(YELLOW + "Ligne " + (lig + 1) + " complétée !" + RESET + WHITE + BG_BLACK);
            }
            if (estColonneComplete(grille, col)) {
                ajouterHistorique(YELLOW + "Colonne " + (col + 1) + " complétée !" + RESET + WHITE + BG_BLACK);
            }
            return true;
        }
        return false;
    }

    // Gère la logique complète d'un tour : demande de coordonnées, gestion des jokers et placement de la carte.
    // etatJeu est un tableau pour passer les variables par référence : [0]idxPaquet, [1]cartesPosees, [2]nbJokers, [3]peutPasser (1=true), [4]tourJoue (1=true)
    void traiterTourDeJeu(Carte[][] grille, Carte cActuelle, int[] etatJeu, String[] messageBox) {
        int lig = lireCoordonneeOuJoker("Ligne (1-5) ou 'J' : ");
        String errLig = verifierErreurSaisie(lig, "Ligne");
        
        if (length(errLig) > 0) {
            messageBox[0] = errLig;
        } else if (lig == -2) { // CAS DU JOKER DEMANDÉ
            if (etatJeu[2] > 0 && etatJeu[3] == 1) {
                if (jouerSequenceJoker(cActuelle)) {
                    messageBox[0] = GREEN + ">> CORRECT ! Carte défaussée." + RESET + WHITE + BG_BLACK;
                    etatJeu[2] -= 1; // On décrémente les jokers
                    etatJeu[3] = 0;  // Le joueur ne peut plus passer ce tour-ci
                    etatJeu[0] += 1; // On passe à la carte suivante du paquet
                    etatJeu[4] = 1;  // Le tour est considéré comme joué
                } else {
                    messageBox[0] = RED + ">> ERREUR ! Vous devez jouer la carte." + RESET + WHITE + BG_BLACK;
                    etatJeu[3] = 0;
                }
            } else {
                messageBox[0] = RED + ">> Joker impossible !" + RESET + WHITE + BG_BLACK;
            }
        } else { // CAS DU PLACEMENT (Colonne)
            int col = lireCoordonneeOuJoker("Colonne (1-5) ou 'J' : ");
            String errCol = verifierErreurSaisie(col, "Colonne");
            if (length(errCol) > 0) {
                messageBox[0] = errCol;
            } else if (col == -2) {
                messageBox[0] = RED + ">> Joker doit être fait au choix de la ligne !" + RESET + WHITE + BG_BLACK;
            } else {
                if (traiterPlacement(grille, lig, col, cActuelle)) {
                    etatJeu[1] += 1; // Une carte de plus posée
                    etatJeu[0] += 1; // On passe à la carte suivante
                    etatJeu[3] = 1;  // Reset du droit de passer pour le prochain tour
                    etatJeu[4] = 1;  // Tour validé
                } else {
                    messageBox[0] = RED + ">> ERREUR : Case occupée !" + RESET + WHITE + BG_BLACK;
                }
            }
        }
    }

    // Orchestre la partie : initialise le jeu, boucle sur les 25 tours et appelle la fin de partie.
    void lancerJeu() {
        Carte[] paquet = creeNouveauJeu();
        melanger(paquet);
        Carte[][] grille = new Carte[5][5];
        historiqueJeu = initialiserHistorique();
        
        ajouterHistorique("Nouvelle partie lancée.");
        ajouterHistorique("Seed : " + seedActuelle);
        ajouterHistorique("-----------------------");

        // Tableau d'état pour modification dans les sous-fonctions :
        // [0]idxPaquet, [1]cartesPosees, [2]nbJokers, [3]peutPasser(1=vrai), [4]tourJoue(1=vrai)
        int[] etatJeu = new int[]{0, 0, NB_JOKERS_MAX, 1, 0}; 
        
        // Reset variables questions
        derniereQuestionEnonce = ""; derniereReponseJusteLettre = ""; derniereExplication = "";
        
        String[] messageBox = new String[]{""};
        int[] ptsLignes = new int[5];
        int[] ptsCols = new int[5];

        while (etatJeu[1] < 25 && etatJeu[0] < 52) {
            Carte cActuelle = paquet[etatJeu[0]];
            Carte cSuivante = (etatJeu[0] < 51) ? paquet[etatJeu[0] + 1] : null;

            mettreAJourScores(grille, ptsLignes, ptsCols);
            etatJeu[4] = 0; // Reset tourJoue

            while (etatJeu[4] == 0) {
                afficherEcranJeu(grille, cActuelle, cSuivante, messageBox[0], etatJeu[2], ptsLignes, ptsCols);
                if (length(messageBox[0]) > 0) {
                    messageBox[0] = "";
                }
                println("\n=== TOUR " + (etatJeu[1] + 1) + "/25 ===");
                
                traiterTourDeJeu(grille, cActuelle, etatJeu, messageBox);
            }
        }
        gererFinDePartie(grille, ptsLignes, ptsCols);
    }

    // Nettoie la chaîne pseudo (enlève , et ;), gère le cas vide et tronque si trop long.
    String nettoyerPseudo(String p) {
        String res = "";
        for (int i = 0; i < length(p); i += 1) {
            char c = charAt(p, i);
            if (c != ',' && c != ';') {
                res = res + c;
            }
        }
        boolean vide = true;
        for (int i = 0; i < length(res); i += 1) {
            if (charAt(res, i) != ' ') {
                vide = false;
            }
        }
        if (vide) {
            return "Anonyme";
        }
        if (length(res) > 20) {
            res = substring(res, 0, 20);
        }
        return res;
    }

    // Ajoute un message en bas de l'historique et décale les anciens vers le haut.
    void ajouterHistorique(String msg) {
        for (int i = 0; i < length(historiqueJeu) - 1; i += 1) {
            historiqueJeu[i] = historiqueJeu[i + 1];
        }
        historiqueJeu[length(historiqueJeu) - 1] = msg;
    }

    // ========================================================================================================================
    // CALCUL SCORE
    // ========================================================================================================================

    // Retourne la valeur en jetons (Chips) d'une carte (ex: As = 11, Têtes = 10, autres = valeur faciale).
    int jetonsPourValeur(int valeurNum) {
        if (valeurNum >= 2 && valeurNum <= 9) {
            return valeurNum;
        }
        if (valeurNum >= 10 && valeurNum <= 13) {
            return 10;
        }
        if (valeurNum == 14) {
            return 11;
        }
        return 0;
    }

    // Calcule la somme des jetons de toutes les cartes d'une main.
    int calculerSommeTotale(Carte[] triee) {
        int somme = 0;
        for (int i = 0; i < length(triee); i += 1) {
            somme += jetonsPourValeur(triee[i].num);
        }
        return somme;
    }

    // Calcule la somme des jetons uniquement pour les cartes apparaissant n fois (ex: somme des 3 cartes d'un brelan).
    int calculerSommeMultiples(int[] counts, int n) {
        int somme = 0;
        for (int v = 2; v <= 14; v += 1) {
            if (counts[v] == n) {
                somme += (jetonsPourValeur(v) * n);
            }
        }
        return somme;
    }

    // Identifie la meilleure combinaison de poker (Carré, Suite, etc.) et calcule le score (Base + Somme) * Multiplicateur.
    int getCombinaison(Carte[] triee, int[] counts) {
        boolean flush = estCouleur(triee);
        boolean straight = estSuite(triee);
        boolean carre = false;
        boolean brelan = false;
        int paires = 0;

        for (int i = 0; i < length(counts); i += 1) {
            if (counts[i] == 4) {
                carre = true;
            }
            if (counts[i] == 3) {
                brelan = true;
            }
            if (counts[i] == 2) {
                paires += 1;
            }
        }

        int baseChips = 0;
        int mult = 0;
        int sommeJetonsCartes = 0;

        if (flush && straight) {
            baseChips = 100; mult = 8;
            sommeJetonsCartes = calculerSommeTotale(triee);
        } else if (carre) {
            baseChips = 60; mult = 7;
            sommeJetonsCartes = calculerSommeMultiples(counts, 4);
        } else if (brelan && paires >= 1) {
            baseChips = 40; mult = 4;
            sommeJetonsCartes = calculerSommeTotale(triee);
        } else if (flush) {
            baseChips = 35; mult = 4;
            sommeJetonsCartes = calculerSommeTotale(triee);
        } else if (straight) {
            baseChips = 30; mult = 4;
            sommeJetonsCartes = calculerSommeTotale(triee);
        } else if (brelan) {
            baseChips = 30; mult = 3;
            sommeJetonsCartes = calculerSommeMultiples(counts, 3);
        } else if (paires == 2) {
            baseChips = 20; mult = 2;
            sommeJetonsCartes = calculerSommeMultiples(counts, 2);
        } else if (paires == 1) {
            baseChips = 10; mult = 2;
            sommeJetonsCartes = calculerSommeMultiples(counts, 2);
        } else {
            baseChips = 5; mult = 1;
            sommeJetonsCartes = jetonsPourValeur(triee[4].num);
        }
        return (sommeJetonsCartes + baseChips) * mult;
    }

    // Fonction enveloppe qui prépare les données (tri, comptage) avant d'appeler le calcul de combinaison.
    int calculerPointsMain(Carte[] main) {
        for (int i = 0; i < length(main); i += 1) {
            if (main[i] == null) {
                return 0;
            }
        }
        Carte[] triee = copierTableau(main);
        trierMain(triee);
        int[] counts = compterValeurs(triee);
        return getCombinaison(triee, counts);
    }

    // ========================================================================================================================
    // OUTILS ET AFFICHAGE
    // ========================================================================================================================

    // Demande une saisie utilisateur et gère les cas spéciaux (J pour Joker, nombres 1-5).
    int lireCoordonneeOuJoker(String message) {
        print(message);
        String s = readString();
        if (equals(s, "j") || equals(s, "J")) {
            return -2;
        }
        if (estUnNombre(s)) {
            int val = stringToInt(s);
            if (val >= 1 && val <= 5) {
                return val - 1;
            } else {
                return -3;
            }
        }
        return -1;
    }

    // Lit le fichier CSV des questions et charge les données dans la mémoire.
    void chargerQuestions() {
        CSVFile f = loadCSV("../ressource/questions.csv");
        int nb = rowCount(f);
        baseDeQuestions = new Question[nb];
        for (int i = 0; i < nb; i += 1) {
            Question q = new Question();
            q.enonce = getCell(f, i, 0);
            q.repA = getCell(f, i, 1);
            q.repB = getCell(f, i, 2);
            q.repC = getCell(f, i, 3);
            q.repD = getCell(f, i, 4);
            q.bonneReponse = getCell(f, i, 5);
            q.explication = getCell(f, i, 6);
            baseDeQuestions[i] = q;
        }
        println(nb + " questions chargées.");
    }

    // Retourne le texte de la réponse correspondant à la lettre (A, B, C ou D).
    String getTexteReponse(Question q, String lettre) {
        if (equals(lettre, "A")) {
            return q.repA;
        }
        if (equals(lettre, "B")) {
            return q.repB;
        }
        if (equals(lettre, "C")) {
            return q.repC;
        }
        if (equals(lettre, "D")) {
            return q.repD;
        }
        return "Inconnu";
    }

    // Sélectionne une question aléatoire, l'affiche et vérifie la réponse du joueur.
    boolean poserQuestion() {
        int idx = genererNombrePseudoAleatoire(length(baseDeQuestions));
        Question q = baseDeQuestions[idx];
        String rep = "";
        boolean valid = false;
        String msgErreur = "";

        derniereQuestionEnonce = q.enonce;

        while (!valid) {
            afficherEcranQuestion(q, msgErreur, false);
            print("Votre réponse (A/B/C/D) : ");
            rep = readString();
            if (length(rep) == 1) {
                char c = charAt(rep, 0);
                if (c >= 'a' && c <= 'd') {
                    valid = true;
                }
                if (c >= 'A' && c <= 'D') {
                    valid = true;
                }
            }
            if (!valid) {
                msgErreur = RED + "Réponse invalide (A, B, C ou D attendu)" + RESET + WHITE + BG_BLACK;
            }
        }

        derniereReponseJoueurLettre = toUpperCase(rep);
        derniereReponseJoueurTexte = getTexteReponse(q, derniereReponseJoueurLettre);
        
        derniereReponseJusteLettre = q.bonneReponse;
        derniereReponseJusteTexte = getTexteReponse(q, derniereReponseJusteLettre);
        
        derniereExplication = q.explication;
        
        boolean correct = equals(derniereReponseJoueurLettre, derniereReponseJusteLettre);

        String msgResultat = "";
        if (correct) {
            msgResultat = GREEN + "BONNE RÉPONSE ! 1 Joker Utilisé et carte defaussée" + RESET + WHITE + BG_BLACK;
        } else {
            msgResultat = RED + "MAUVAISE RÉPONSE... (" + q.bonneReponse + ")" + RESET + WHITE + BG_BLACK;
        }
        
        afficherEcranQuestion(q, msgResultat, true);
        println("\nAppuyez sur Entrée pour revenir au jeu...");
        readString();
        return correct;
    }

    // Affiche l'interface de la question (énoncé, réponses possibles).
    void afficherEcranQuestion(Question q, String message, boolean montrerSolution) {
        clear();
        String[] ecran = lireTemplate("../ressource/question.txt");
        ecrireDansBuffer(ecran, 15, 10, q.enonce);
        ecrireDansBuffer(ecran, 23, 8, "A) " + q.repA);
        ecrireDansBuffer(ecran, 23, 90, "B) " + q.repB);
        ecrireDansBuffer(ecran, 29, 8, "C) " + q.repC);
        ecrireDansBuffer(ecran, 29, 90, "D) " + q.repD);
        if (length(message) > 0) {
            ecrireDansBuffer(ecran, 18, 8, message);
        }
        if (montrerSolution) {
            ecrireDansBuffer(ecran, 18, 90, "Explication : " + q.explication);
        }
        rendreBuffer(ecran);
    }

    // Affiche l'interface principale du jeu (Grille, Scores, Carte courante, Historique).
    void afficherEcranJeu(Carte[][] grille, Carte cActuelle, Carte cSuivante, String message, int nbJokers, int[] ptsLignes, int[] ptsCols) {
        clear();
        String[] ecran = lireTemplate("../ressource/jeu.txt");
        int startLigne = 7;
        int startCol = 4;

        for (int i = 0; i < length(historiqueJeu); i += 1) {
            ecrireDansBuffer(ecran, LIG_HISTORIQUE_START + i, COL_HISTORIQUE, historiqueJeu[i]);
        }
        if (length(message) > 0) {
            ecrireDansBuffer(ecran, 45, 162, message);
        }
        if (length(derniereReponseJusteLettre) > 0) {
            ecrireDansBuffer(ecran, 39, 66, CYAN + "DERNIERE QUESTION :" + RESET + WHITE + BG_BLACK);
            
            if (length(derniereQuestionEnonce) > 90) {
                String l1 = substring(derniereQuestionEnonce, 0, 90);
                String l2 = substring(derniereQuestionEnonce, 90, length(derniereQuestionEnonce));
                ecrireDansBuffer(ecran, 40, 66, l1);
                ecrireDansBuffer(ecran, 41, 66, l2);
            } else {
                ecrireDansBuffer(ecran, 40, 66, derniereQuestionEnonce);
            }

            String texteBonne = GREEN + "Reponse Correcte : " + derniereReponseJusteLettre + " - " + derniereReponseJusteTexte + RESET + WHITE + BG_BLACK;
            ecrireDansBuffer(ecran, 42, 66, texteBonne);

            String coulJoueur = "";
            if (equals(derniereReponseJusteLettre, derniereReponseJoueurLettre)) {
                coulJoueur = GREEN;
            } else {
                coulJoueur = RED;
            }
            String texteJoueur = coulJoueur + "Votre Reponse    : " + derniereReponseJoueurLettre + " - " + derniereReponseJoueurTexte + RESET + WHITE + BG_BLACK;
            ecrireDansBuffer(ecran, 43, 66, texteJoueur);

            ecrireDansBuffer(ecran, 44, 66, "Explication : " + derniereExplication);
        }
        for (int i = 0; i < 5; i += 1) {
            for (int j = 0; j < 5; j += 1) {
                if (grille[i][j] != null) {
                    int l = startLigne + (i * 7);
                    int c = startCol + (j * 10);
                    dessinerCarteGrande(ecran, l, c, grille[i][j]);
                }
            }
        }
        for (int i = 0; i < 5; i += 1) {
            if (estLigneComplete(grille, i)) {
                int colScore = 58;
                if (ptsLignes[i] > 999) {
                    colScore -= 2;
                } else if (ptsLignes[i] > 99) {
                    colScore -= 1;
                }
                ecrireDansBuffer(ecran, startLigne + (i * 7) + 3, colScore, "=" + ptsLignes[i]);
            }
        }
        for (int j = 0; j < 5; j += 1) {
            if (estColonneComplete(grille, j)) {
                ecrireDansBuffer(ecran, 42, 4 + (j * 10), "=" + ptsCols[j]);
            }
        }
        if (cActuelle != null) {
            ecrireDansBuffer(ecran, 45, 21, nomCarte(cActuelle));
        }
        if (cSuivante != null) {
            ecrireDansBuffer(ecran, 45, 49, nomCarte(cSuivante));
        }
        ecrireDansBuffer(ecran, 35, 117, "(" + nbJokers + "/" + NB_JOKERS_MAX + ")");
        rendreBuffer(ecran);
    }

    // Calcule la longueur visuelle d'une chaîne en ignorant les codes couleurs ANSI (pour un alignement correct).
    int longueurVisuelle(String s) {
        int len = 0;
        boolean inAnsi = false;
        for (int i = 0; i < length(s); i += 1) {
            char c = charAt(s, i);
            if (c == '\u001B') {
                inAnsi = true;
            } else if (inAnsi && c == 'm') {
                inAnsi = false;
            } else if (!inAnsi) {
                len += 1;
            }
        }
        return len;
    }

    // Insère une chaîne de caractères dans le buffer d'écran aux coordonnées (ligne, colonne) spécifiées.
    void ecrireDansBuffer(String[] ecran, int l, int c, String texte) {
        if (l >= length(ecran)) {
            return;
        }
        String ligne = ecran[l];
        int lenV = longueurVisuelle(texte);
        if (c + lenV > length(ligne)) {
            return;
        }
        ecran[l] = substring(ligne, 0, c) + texte + substring(ligne, c + lenV, length(ligne));
    }

    // Lit un entier saisi par l'utilisateur en s'assurant qu'il est compris entre min et max.
    int lireEntier(String message, int min, int max) {
        int val = -1;
        boolean valide = false;
        while (!valide) {
            print(message);
            String s = readString();
            if (estUnNombre(s)) {
                val = stringToInt(s);
                if (val >= min && val <= max) {
                    valide = true;
                } else {
                    println(">> Erreur : Hors limites");
                }
            } else {
                println(">> Erreur : Nombre attendu");
            }
        }
        return val;
    }

    // Vérifie si une chaîne contient uniquement des chiffres.
    boolean estUnNombre(String s) {
        if (length(s) == 0) {
            return false;
        }
        for (int i = 0; i < length(s); i += 1) {
            char c = charAt(s, i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    // Initialise la graine (seed) aléatoire et lance la partie.
    void lancerPartieAvecSeed(String seed) {
        seedActuelle = seed;
        seedNumber = stringToLong(seed);
        println("Initialisation avec Seed : " + seedActuelle);
        lancerJeu();
    }

    // Génère un nombre pseudo-aléatoire basé sur la graine actuelle (LCG algorithm).
    int genererNombrePseudoAleatoire(int max) {
        if (max <= 0) {
            return 0;
        }
        seedNumber = (seedNumber * 1664525 + 1013904223);
        long resultat = seedNumber;
        if (resultat < 0) {
            resultat = resultat * -1;
        }
        return (int) (resultat % max);
    }

    // Convertit une chaîne de caractères en un nombre long pour l'initialisation de la seed.
    long stringToLong(String s) {
        long h = 0;
        for (int i = 0; i < length(s); i += 1) {
            h = 31 * h + charAt(s, i);
        }
        return h;
    }

    // Génère une chaîne aléatoire de caractères alphanumériques de la longueur demandée.
    String genererRandomSeedString(int longueur) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        String res = "";
        for (int i = 0; i < longueur; i += 1) {
            int idx = (int) (random() * length(chars));
            res = res + charAt(chars, idx);
        }
        return res;
    }

    // ========================================================================================================================
    // LEADERBOARD ET RECORDS
    // ========================================================================================================================

    // Sauvegarde le score du joueur dans le fichier CSV 'record.csv' et maintient le classement à jour.
    void sauvegarderNouveauScore(String pseudo, int score, String seed) {
        ScoreRecord[] records = chargerLeaderboard();
        ScoreRecord[] nouveauxRecords = new ScoreRecord[length(records) + 1];
        
        for (int i = 0; i < length(records); i += 1) {
            nouveauxRecords[i] = records[i];
        }
        
        ScoreRecord nouv = new ScoreRecord();
        nouv.pseudo = pseudo;
        nouv.score = score;
        nouv.seed = seed;
        nouveauxRecords[length(records)] = nouv;

        trierRecords(nouveauxRecords);

        String[][] data = new String[length(nouveauxRecords) + 1][3]; 
        data[0][0] = "Pseudo";
        data[0][1] = "Score";
        data[0][2] = "Seed";
        
        for (int i = 0; i < length(nouveauxRecords); i += 1) {
            String p = nouveauxRecords[i].pseudo;
            String s = nouveauxRecords[i].seed;
            if (p == null) {
                p = "Inconnu";
            }
            if (s == null) {
                s = "?";
            }
            data[i + 1][0] = p;
            data[i + 1][1] = "" + nouveauxRecords[i].score;
            data[i + 1][2] = s;
        }
        saveCSV(data, "../ressource/record.csv");
    }

    // Charge la liste des scores depuis le fichier CSV, en filtrant les entrées invalides.
    ScoreRecord[] chargerLeaderboard() {
        CSVFile f = loadCSV("../ressource/record.csv");
        int nbLignesTotal = rowCount(f);
        int nbLignesValides = 0;
        for (int i = 1; i < nbLignesTotal; i += 1) {
            String p = getCell(f, i, 0);
            if (p != null && length(p) > 0 && !equals(p, "null")) {
                nbLignesValides += 1;
            }
        }
        ScoreRecord[] tab = new ScoreRecord[nbLignesValides];
        int idx = 0;
        
        for (int i = 1; i < nbLignesTotal; i += 1) {
            String p = getCell(f, i, 0);
            if (p != null && length(p) > 0 && !equals(p, "null")) {
                ScoreRecord r = new ScoreRecord();
                r.pseudo = p;
                String sc = getCell(f, i, 1);
                if (estUnNombre(sc)) {
                    r.score = stringToInt(sc);
                } else {
                    r.score = 0;
                }
                r.seed = getCell(f, i, 2);
                if (r.seed == null) {
                    r.seed = "?";
                }
                tab[idx] = r;
                idx += 1;
            }
        }
        trierRecords(tab);
        return tab;
    }

    // Trie le tableau de records par score décroissant (Tri à bulles).
    void trierRecords(ScoreRecord[] tab) {
        for (int i = 0; i < length(tab) - 1; i += 1) {
            for (int j = 0; j < length(tab) - i - 1; j += 1) {
                if (tab[j].score < tab[j + 1].score) {
                    ScoreRecord tmp = tab[j];
                    tab[j] = tab[j + 1];
                    tab[j + 1] = tmp;
                }
            }
        }
    }

    // Affiche le menu du classement (Leaderboard) avec les meilleurs scores.
    void afficherEcranLeaderboardMenu() {
        clear();
        println("=== LEADERBOARD (TOP SCORES) ===");
        ScoreRecord[] recs = chargerLeaderboard();
        println("Rang | Pseudo          | Score  | Seed");
        println("----------------------------------------");
        int max = 100;
        if (length(recs) < max) {
            max = length(recs);
        }
        for (int i = 0; i < max; i += 1) {
            String p = recs[i].pseudo;
            while (length(p) < 15) {
                p = p + " ";
            }
            String s = "" + recs[i].score;
            while (length(s) < 6) {
                s = s + " ";
            }
            println((i + 1) + ".   | " + p + " | " + s + " | " + recs[i].seed);
        }
        println("\nAppuyez sur Entrée...");
        readString();
    }

    // ========================================================================================================================
    // ECRAN RESULTAT
    // ========================================================================================================================

    // Affiche l'écran de fin de partie avec la grille, le score final, l'historique et le classement.
    void afficherEcranResultat(Carte[][] grille, int[] ptsLignes, int[] ptsCols, int scoreTotal, String messageInfo, String pseudoJoueur, String seedJoueur) {
        clear();
        String[] ecran = lireTemplate("../ressource/resultat.txt");
        int startLigne = 7;
        int startCol = 4;

        for (int i = 0; i < 5; i += 1) {
            for (int j = 0; j < 5; j += 1) {
                if (grille[i][j] != null) {
                    int l = startLigne + (i * 7);
                    int c = startCol + (j * 10);
                    dessinerCarteGrande(ecran, l, c, grille[i][j]);
                }
            }
        }
        for (int i = 0; i < 5; i += 1) {
            int colScore = 58;
            if (ptsLignes[i] > 999) {
                colScore -= 2; 
            } else if (ptsLignes[i] > 99) {
                colScore -= 1; 
            }
            ecrireDansBuffer(ecran, startLigne + (i * 7) + 3, colScore, "=" + ptsLignes[i]);
        }
        for (int j = 0; j < 5; j += 1) {
            ecrireDansBuffer(ecran, 42, 4 + (j * 10), "=" + ptsCols[j]);
        }
        ecrireDansBuffer(ecran, 45, 23, "TOTAL = " + scoreTotal);

        for (int i = 0; i < length(historiqueJeu); i += 1) {
            ecrireDansBuffer(ecran, LIG_HISTORIQUE_START + i, COL_HISTORIQUE, historiqueJeu[i]);
        }
        if (length(messageInfo) > 0) {
            ecrireDansBuffer(ecran, 45, 162, messageInfo);
        }

        ScoreRecord[] recs = chargerLeaderboard();
        int ligStart = 25; 
        int maxLignes = 20; 
        boolean joueurAffiche = false; 

        for (int i = 0; i < length(recs) && i < maxLignes; i += 1) {
            ScoreRecord r = recs[i];
            
            String sRang = "" + (i + 1);
            String sPseudo = r.pseudo;
            if (length(sPseudo) > 20) {
                sPseudo = substring(sPseudo, 0, 20);
            }
            String sScore = "" + r.score;
            String sSeed = r.seed;

            String color = "";
            boolean estLeJoueur = length(pseudoJoueur) > 0 && equals(r.pseudo, pseudoJoueur) && equals(sSeed, seedJoueur);
            if (estLeJoueur) {
                color = YELLOW;
                joueurAffiche = true;
            }
            
            ecrireDansBuffer(ecran, ligStart + i, 140, color + sSeed + RESET + WHITE + BG_BLACK);
            ecrireDansBuffer(ecran, ligStart + i, 116, color + sScore + RESET + WHITE + BG_BLACK);
            ecrireDansBuffer(ecran, ligStart + i, 93, color + sPseudo + RESET + WHITE + BG_BLACK);
            ecrireDansBuffer(ecran, ligStart + i, 68, color + sRang + RESET + WHITE + BG_BLACK);
        }

        if (length(pseudoJoueur) > 0 && !joueurAffiche) {
            int rangReel = -1;
            for (int k = maxLignes; k < length(recs); k += 1) {
                ScoreRecord r = recs[k];
                if (equals(r.pseudo, pseudoJoueur) && equals(r.seed, seedJoueur)) {
                    rangReel = k + 1;
                }
            }

            if (rangReel != -1) {
                String color = YELLOW;
                String sPseudo = pseudoJoueur;
                 if (length(sPseudo) > 20) {
                    sPseudo = substring(sPseudo, 0, 20);
                }
                ecrireDansBuffer(ecran, 45, 140, color + seedJoueur + RESET + WHITE + BG_BLACK);
                ecrireDansBuffer(ecran, 45, 116, color + scoreTotal + RESET + WHITE + BG_BLACK);
                ecrireDansBuffer(ecran, 45, 93, color + sPseudo + RESET + WHITE + BG_BLACK);
                ecrireDansBuffer(ecran, 45, 68, color + rangReel + RESET + WHITE + BG_BLACK);
            }
        }
        rendreBuffer(ecran);
    }

    // Dessine une carte en grand format dans le buffer d'écran.
    void dessinerCarteGrande(String[] ecran, int l, int c, Carte card) {
        String val = LISTE_VALEUR[card.num];
        String sym = LISTE_COULEUR[card.couleur];
        ecrireDansBuffer(ecran, l + 1, c + 2, val);
        ecrireDansBuffer(ecran, l + 3, c + 4, sym);
        int decalage = 0;
        if (length(val) == 2) {
            decalage = 5;
        } else {
            decalage = 6;
        }
        ecrireDansBuffer(ecran, l + 5, c + decalage, val);
    }

    // Vérifie si une ligne de la grille est complète (contient 5 cartes).
    boolean estLigneComplete(Carte[][] grille, int lig) {
        for (int j = 0; j < 5; j += 1) {
            if (grille[lig][j] == null) {
                return false;
            }
        }
        return true;
    }

    // Vérifie si une colonne de la grille est complète (contient 5 cartes).
    boolean estColonneComplete(Carte[][] grille, int col) {
        for (int i = 0; i < 5; i += 1) {
            if (grille[i][col] == null) {
                return false;
            }
        }
        return true;
    }

    // Wrapper pour calculer le score d'une ligne spécifique.
    int calculerScoreLigne(Carte[][] grille, int ligneIdx) {
        return calculerPointsMain(grille[ligneIdx]);
    }

    // Wrapper pour extraire une colonne et calculer son score.
    int calculerScoreColonne(Carte[][] grille, int colIdx) {
        Carte[] col = new Carte[5];
        for (int i = 0; i < 5; i += 1) {
            col[i] = grille[i][colIdx];
        }
        return calculerPointsMain(col);
    }

    // Vérifie si toutes les cartes du tableau sont de la même couleur (Flush).
    boolean estCouleur(Carte[] m) {
        int c = m[0].couleur;
        for (int i = 1; i < 5; i += 1) {
            if (m[i].couleur != c) {
                return false;
            }
        }
        return true;
    }

    // Vérifie si les cartes forment une suite numérique consécutive (Straight). Gère le cas de l'As (14) en début de suite (A-2-3-4-5).
    boolean estSuite(Carte[] m) {
        if (m[0].num == 2 && m[1].num == 3 && m[2].num == 4 && m[3].num == 5 && m[4].num == 14) {
            return true;
        }
        for (int i = 0; i < 4; i += 1) {
            if (m[i + 1].num != m[i].num + 1) {
                return false;
            }
        }
        return true;
    }

    // Compte les occurrences de chaque valeur de carte dans la main (pour détecter paires, brelans, carrés).
    int[] compterValeurs(Carte[] m) {
        int[] c = new int[15];
        for (int i = 0; i < 5; i += 1) {
            c[m[i].num] += 1;
        }
        return c;
    }

    // Trie le tableau de cartes par ordre croissant de valeur numérique en utilisant un Tri à Bulles.
    void trierMain(Carte[] t) {
        for (int i = 0; i < length(t) - 1; i += 1) {
            for (int j = 0; j < length(t) - i - 1; j += 1) {
                if (t[j].num > t[j + 1].num) {
                    Carte temp = t[j];
                    t[j] = t[j + 1];
                    t[j + 1] = temp;
                }
            }
        }
    }

    // Crée une copie indépendante d'un tableau de cartes.
    Carte[] copierTableau(Carte[] src) {
        Carte[] dest = new Carte[length(src)];
        for (int i = 0; i < length(src); i += 1) {
            dest[i] = src[i];
        }
        return dest;
    }

    // Affiche le contenu du buffer d'écran ligne par ligne, en interprétant certains caractères spéciaux pour l'affichage (barré, fond blanc).
    void rendreBuffer(String[] ecran) {
        for (int i = 0; i < length(ecran); i += 1) {
            print(WHITE + BG_BLACK); 
            String ligne = ecran[i];
            for (int j = 0; j < length(ligne); j += 1) {
                char c = charAt(ligne, j);
                if (c == '§') {
                    print(STRIKETHROUGH + BG_BLACK + WHITE + " " + RESET);
                } else if (c == '¤') {
                    print(BG_WHITE + " " + BG_BLACK);
                } else {
                    print(c);
                }
            }
            println();
        }
        print(RESET);
    }

    // Charge un fichier texte template en mémoire sous forme de tableau de chaînes.
    String[] lireTemplate(String chemin) {
        File f = newFile(chemin);
        int nb = 0;
        while (ready(f)) {
            readLine(f);
            nb += 1;
        }
        f = newFile(chemin);
        String[] tab = new String[nb];
        int idx = 0;
        while (ready(f)) {
            tab[idx] = readLine(f);
            idx += 1;
        }
        return tab;
    }

    // Affiche la page d'accueil ASCII.
    void afficherPageAccueuil() {
        String[] ecran = lireTemplate("../ressource/acceuil.txt");
        rendreBuffer(ecran);
    }

    // Affiche les règles du jeu.
    void afficherRegles() {
        clear();
        String[] regles = lireTemplate("../ressource/regle.txt");
        rendreBuffer(regles);
        println("\nAppuyez sur Entrée pour revenir au menu...");
        readString();
    }

    // Initialise un paquet standard de 52 cartes triées.
    Carte[] creeNouveauJeu() {
        Carte[] p = new Carte[52];
        int idx = 0;
        for (int c = 0; c < 4; c += 1) {
            for (int v = 2; v <= 14; v += 1) {
                Carte n = new Carte();
                n.couleur = c;
                n.num = v;
                p[idx] = n;
                idx += 1;
            }
        }
        return p;
    }

    // Mélange le paquet de cartes de manière aléatoire (Algorithme de Fisher-Yates simplifié).
    void melanger(Carte[] p) {
        for (int i = 0; i < length(p); i += 1) {
            int r = genererNombrePseudoAleatoire(length(p));
            Carte t = p[i];
            p[i] = p[r];
            p[r] = t;
        }
    }

    // Retourne une représentation textuelle courte d'une carte (ex: "10♥").
    String nomCarte(Carte c) {
        return LISTE_VALEUR[c.num] + LISTE_COULEUR[c.couleur];
    }

    // Efface le terminal en utilisant des codes ANSI.
    void clear() {
        print("\u001B[H\u001B[2J" + RESET);
    }

    // ========================================================================================================================
    // TESTS UNITAIRES
    // ========================================================================================================================
    
    // Helper pour créer facilement une carte dans les tests.
    Carte newCarte(int v, int c) {
        Carte a = new Carte();
        a.num = v;
        a.couleur = c;
        return a;
    }

    void test_calculerPointsMain() {
        Carte[] royal = new Carte[]{newCarte(10, 0), newCarte(V, 0), newCarte(D, 0), newCarte(R, 0), newCarte(A, 0)};
        assertEquals(1208, calculerPointsMain(royal)); 
        
        Carte[] rien = new Carte[]{newCarte(2, 0), newCarte(4, 1), newCarte(6, 2), newCarte(8, 3), newCarte(10, 0)};
        assertEquals(15, calculerPointsMain(rien)); 

        Carte[] full = new Carte[]{newCarte(R, 0), newCarte(R, 1), newCarte(R, 2), newCarte(A, 0), newCarte(A, 1)};
        assertEquals(368, calculerPointsMain(full));

        Carte[] paire = new Carte[]{newCarte(5, 0), newCarte(5, 1), newCarte(2, 2), newCarte(3, 3), newCarte(4, 0)};
        assertEquals(40, calculerPointsMain(paire));
    }

    void test_jetonsPourValeur() {
        assertEquals(5, jetonsPourValeur(5));
        assertEquals(10, jetonsPourValeur(10));
        assertEquals(11, jetonsPourValeur(14));
        assertEquals(0, jetonsPourValeur(99)); 
    }
    
    void test_estLigneComplete() {
        Carte[][] grille = new Carte[5][5];
        for (int j = 0; j < 5; j += 1) {
            grille[0][j] = newCarte(2, 0);
        }
        assertTrue(estLigneComplete(grille, 0));
        assertFalse(estLigneComplete(grille, 1));
    }

    void test_estColonneComplete() {
        Carte[][] grille = new Carte[5][5];
        for (int i = 0; i < 5; i += 1) {
            grille[i][0] = newCarte(2, 0);
        }
        assertTrue(estColonneComplete(grille, 0));
        assertFalse(estColonneComplete(grille, 1));
    }

    void test_calculerScoreLigne() {
        Carte[][] grille = new Carte[5][5];
        for (int j = 0; j < 5; j += 1) {
            grille[0][j] = newCarte(j + 2, 0);
        }
        assertTrue(calculerScoreLigne(grille, 0) > 0);
        assertEquals(0, calculerScoreLigne(grille, 1)); 
    }

    void test_calculerScoreColonne() {
        Carte[][] grille = new Carte[5][5];
        for (int i = 0; i < 5; i += 1) {
            grille[i][0] = newCarte(A, 0);
        }
        assertTrue(calculerScoreColonne(grille, 0) > 0);
        assertEquals(0, calculerScoreColonne(grille, 1));
    }

    void test_estCouleur() {
        Carte[] oui = new Carte[]{newCarte(2, 0), newCarte(5, 0), newCarte(7, 0), newCarte(9, 0), newCarte(R, 0)};
        assertTrue( estCouleur(oui));
        Carte[] non = new Carte[]{newCarte(2, 0), newCarte(5, 1), newCarte(7, 0), newCarte(9, 0), newCarte(R, 0)};
        assertFalse( estCouleur(non));
    }

    void test_estSuite() {
        Carte[] oui = new Carte[]{newCarte(2, 0), newCarte(3, 1), newCarte(4, 0), newCarte(5, 2), newCarte(6, 0)};
        assertTrue(estSuite(oui));
        Carte[] non = new Carte[]{newCarte(2, 0), newCarte(3, 1), newCarte(4, 0), newCarte(5, 2), newCarte(7, 0)};
        assertFalse(estSuite(non));
    }

    void test_compterValeurs() {
        Carte[] main = new Carte[]{newCarte(5, 0), newCarte(5, 1), newCarte(5, 2), newCarte(2, 3), newCarte(9, 0)};
        int[] counts = compterValeurs(main);
        assertEquals(3, counts[5]);
        assertEquals(1, counts[2]);
        assertEquals(1, counts[9]);
        assertEquals(0, counts[10]);
    }

    void test_copierTableau() {
        Carte[] src = new Carte[]{newCarte(1, 1)};
        Carte[] dest = copierTableau(src);
        assertEquals(1, length(dest));
        assertFalse(src == dest); 
    }

    void test_creeNouveauJeu() {
        Carte[] p = creeNouveauJeu();
        assertEquals(52, length(p));
        assertFalse(p[0] == null);
    }

    void test_nomCarte() {
        Carte c = newCarte(14, 3); 
        assertEquals("A♠", nomCarte(c));
        Carte c2 = newCarte(10, 1); 
        assertEquals("10♥", nomCarte(c2));
    }

    void test_longueurVisuelle() {
        assertEquals(3, longueurVisuelle("abc"));
        assertEquals(0, longueurVisuelle(""));
        assertEquals(4, longueurVisuelle("test" + RED));
    }

    void test_estUnNombre() {
        assertTrue(estUnNombre("123"));
        assertFalse(estUnNombre("12a"));
        assertFalse(estUnNombre(""));
    }

    void test_genererNombrePseudoAleatoire() {
        seedNumber = 12345;
        int n = genererNombrePseudoAleatoire(10);
        assertTrue(n >= 0);
        assertTrue(n < 10);
    }

    void test_stringToLong() {
        assertTrue(stringToLong("A") > 0);
        assertEquals(0, stringToLong(""));
    }

    void test_genererRandomSeedString() {
        assertEquals(5, length(genererRandomSeedString(5)));
        assertEquals(10, length(genererRandomSeedString(10)));
    }

    void test_getTexteReponse() {
        Question q = new Question();
        q.repA = "Reponse A";
        q.repB = "Reponse B";
        assertEquals("Reponse A", getTexteReponse(q, "A"));
        assertEquals("Reponse B", getTexteReponse(q, "B"));
        assertEquals("Inconnu", getTexteReponse(q, "Z"));
    }

    void test_nettoyerPseudo() {
        assertEquals("Toto", nettoyerPseudo("Toto"));
        assertEquals("Tata", nettoyerPseudo("Ta,ta;")); // Test suppression virgules
        assertEquals("Anonyme", nettoyerPseudo("   ")); // Test vide
        assertEquals(20, length(nettoyerPseudo("UnPseudoVraimentTresTresLongPourRien"))); // Test troncature
    }

    void test_getCombinaison() {
        Carte[] main = new Carte[]{newCarte(5, 0), newCarte(5, 1), newCarte(5, 2), newCarte(2, 3), newCarte(9, 0)};
        int[] counts = compterValeurs(main);
        assertEquals(135, getCombinaison(main, counts));

        Carte[] flush = new Carte[]{newCarte(2, 0), newCarte(5, 0), newCarte(7, 0), newCarte(9, 0), newCarte(R, 0)};
        int[] countsFlush = compterValeurs(flush);
        assertEquals(272, getCombinaison(flush, countsFlush));
    }

    void test_initialiserHistorique() {
        String[] h = initialiserHistorique();
        assertEquals(NB_LIGNES_HISTO, length(h));
        assertEquals("", h[0]);
        assertEquals("", h[NB_LIGNES_HISTO - 1]);
    }

    void test_tenterPlacerCarte() {
        Carte[][] grille = new Carte[5][5];
        Carte c1 = newCarte(10, 0);
        
        assertTrue(tenterPlacerCarte(grille, 0, 0, c1));
        assertFalse(grille[0][0] == null);
        
        Carte c2 = newCarte(11, 1);
        assertFalse(tenterPlacerCarte(grille, 0, 0, c2));
        assertEquals(10, grille[0][0].num);
    }

    void test_calculerSommeTotale() {
        Carte[] mains = new Carte[]{newCarte(2, 0), newCarte(2, 1), newCarte(2, 2), newCarte(2, 3), newCarte(2, 0)};
        assertEquals(10, calculerSommeTotale(mains));

        Carte[] mains2 = new Carte[]{newCarte(2, 0), newCarte(R, 0), newCarte(D, 0), newCarte(10, 0), newCarte(A, 0)};
        assertEquals(43, calculerSommeTotale(mains2));
    }

    void test_calculerSommeMultiples() {
        int[] counts = new int[15];
        counts[5] = 4; 
        counts[2] = 1;
        assertEquals(20, calculerSommeMultiples(counts, 4));

        int[] counts2 = new int[15];
        counts2[2] = 2;
        counts2[3] = 2;
        counts2[10] = 1;
        assertEquals(10, calculerSommeMultiples(counts2, 2));
    }

    void test_verifierErreurSaisie() {
        String err1 = verifierErreurSaisie(-3, "Ligne");
        assertTrue(length(err1) > 0);
        
        String err2 = verifierErreurSaisie(2, "Ligne");
        assertEquals("", err2);
    }

    void test_traiterPlacement() {
        // Init global nécessaire pour le test (historiqueJeu)
        historiqueJeu = initialiserHistorique();
        
        Carte[][] grille = new Carte[5][5];
        Carte c = newCarte(10, 0);

        // 1. Placement valide
        boolean res = traiterPlacement(grille, 0, 0, c);
        assertTrue(res);
        assertFalse(grille[0][0] == null);
        
        // 2. Placement invalide
        boolean res2 = traiterPlacement(grille, 0, 0, c);
        assertFalse(res2);
    }
}