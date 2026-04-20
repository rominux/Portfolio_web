# SAÉ 1.01/1.02 - Tréflexion

**Tréflexion** est un jeu de réflexion développée en iJava. Le but est de placer 25 cartes sur une grille de 5x5 pour former les meilleures combinaisons de poker possibles sur les 5 lignes et les 5 colonnes.

Ce projet intègre un système de questions "Joker" pédagogique et une interface graphique en ASCII Art.

Installation et Compilation

Le point d'entrée du programme est le fichier `Treflexion.java`.

### 1. Compilation
Ouvrez votre terminal à la racine du dossier du projet et exécutez la commande suivante :

```bash
./compile.sh

```

### 2. Exécution

Une fois la compilation terminée sans erreur, lancez le jeu avec la commande :

```bash
./run.sh

```

*(Note : Si vous souhaitez lancer les tests pour vérifier le bon fonctionnement des fonctions)* :

```bash
./test.sh

```

Comment Jouer ?

# Le But du Jeu

Vous disposez d'un paquet de 52 cartes. Vous devez en placer 25, une par une, sur une grille de 5x5 cases.
Une fois une carte posée, **elle ne peut plus être déplacée**.

À la fin de la partie, des points sont attribués pour chaque ligne et chaque colonne selon les combinaisons de poker formées (Paire, Brelan, Suite, Couleur, etc.).

### Commandes en jeu

* **Saisie des coordonnées :** Entrez le numéro de la LIGNE (1-5) puis le numéro de la COLONNE (1-5).
* **Utiliser un Joker :** Au moment de saisir une ligne ou une colonne, tapez `J` (ou `j`).
* Une question de culture générale/mathématiques vous sera posée.
* **Bonne réponse :** La carte actuelle est défaussée (vous ne la jouez pas).
* **Mauvaise réponse :** Vous êtes obligé de jouer la carte.
* Il n'est pas possible d'utiliser un joker 2 fois de suite.



### Système de Points (Base + Main) x Multiplicateur
| Main de Poker | Base | Multtiplicateur | Cartes Actives (qui comptent) |
| :--- | :--- | :--- | :--- |
| **Quinte Flush Royale** | **100** | **x8** | Les 5 cartes |
| **Quinte Flush** | **100** | **x8** | Les 5 cartes |
| **Carré** (4 of a Kind) | **60** | **x7** | Seulement les 4 cartes identiques |
| **Full House** | **40** | **x4** | Les 5 cartes (Brelan + Paire) |
| **Couleur** (Flush) | **35** | **x4** | Les 5 cartes |
| **Suite** (Straight) | **30** | **x4** | Les 5 cartes |
| **Brelan** (3 of a Kind) | **30** | **x3** | Seulement les 3 cartes identiques |
| **Double Paire** | **20** | **x2** | Seulement les 4 cartes (2 Paires) |
| **Paire** | **10** | **x2** | Seulement les 2 cartes de la paire |
| **Carte Haute** | **5** | **x1** | Seulement la carte la plus forte |

Structure du Projet

Voici l'organisation des fichiers source :

* `src/Treflexion.java` : Cœur du programme. Contient l'algorithme principal, la gestion de l'affichage, la logique du jeu et les tests unitaires.
* `ressource/*` : Dossier contenant les ressources externes.

Fonctionnalités Clés

* **Interface ASCII Art avancée :** Utilisation de templates et de positionnement précis du curseur.
* **Système de Seed (Graine) :** Chaque partie possède un identifiant unique (Seed). Vous pouvez entrer une seed spécifique pour rejouer exactement la même distribution de cartes.
* **Jokers Pédagogiques :** Intégration d'un fichier CSV pour charger dynamiquement des questions.

Auteurs

* Projet réalisé dans le cadre de la SAÉ 1.01/1.02.
* Romain LEFEBVRE
* Baptiste MORIN
