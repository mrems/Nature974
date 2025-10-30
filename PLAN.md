# Plan de Développement de l'Application Geronimo

## Plan Général en Phases

1.  **Phase 1 : Authentification de l'utilisateur et initialisation des crédits**
    *   Objectif : Mettre en place la connexion/inscription via Firebase Auth et assurer que chaque nouvel utilisateur ait un document Firestore avec 5 crédits.

2.  **Phase 2 : Système de compteur de requêtes**
    *   Objectif : Implémenter la logique de décrémentation des crédits via une Cloud Function, afficher le solde dans l'application et gérer le "gating" des requêtes.

3.  **Phase 3 : Intégration des achats In-App (Google Play Billing)**
    *   Objectif : Permettre aux utilisateurs d'acheter des packs de crédits via Google Play, avec vérification serveur et ajout sécurisé des crédits.

4.  **Phase 4 : Tests et Déploiement**
    *   Objectif : Tester l'ensemble du système, configurer les pistes de test Google Play et préparer la publication.

---

## Plan Détaillé pour la Phase 1 : Authentification de l'utilisateur et initialisation des crédits

### Objectif de la Phase 1
Mettre en place la connexion/inscription via Firebase Auth (Google et Email/Mot de passe), persister les sessions utilisateur, et garantir que chaque nouvel utilisateur dispose d'un document Firestore (`users/{uid}`) initialisé avec 5 crédits gratuits.

### 1. Configuration de Firebase Authentification (Console Firebase)
*   **Tâche**: Activer les fournisseurs d'authentification nécessaires.
*   **Étapes**:
    1.  Accéder à la [Console Firebase](https://console.firebase.google.com/).
    2.  Naviguer vers "Authentication" > "Sign-in method".
    3.  Activer "Google" et "Email/Password".
    4.  Pour Google Sign-In, s'assurer que l'empreinte SHA-1 de votre clé de signature d'application est configurée. (Normalement, `google-services.json` le fait, mais vérifiez.)

### 2. Mise à jour des Dépendances Android (Gradle)
*   **Tâche**: Ajouter les bibliothèques Firebase Auth et Firestore à votre projet Android.
*   **Fichiers à modifier**: `app/build.gradle.kts` et `gradle/libs.versions.toml`
*   **Détails**:
    *   Dans `gradle/libs.versions.toml`, ajouter les versions pour `firebase-auth-ktx` et `firebase-firestore-ktx`.
    *   Dans `app/build.gradle.kts`, ajouter les implémentations de ces bibliothèques.

    ```kotlin
    // build.gradle.kts (app) - ajouter sous dependencies
    // ... existing code ...
    implementation(platform("com.google.firebase:firebase-bom:32.7.0")) // Vérifiez la dernière version
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    // ... existing code ...
    ```

### 3. Implémentation de l'Interface Utilisateur pour l'Authentification (Android)
*   **Tâche**: Développer les écrans et la logique pour permettre aux utilisateurs de se connecter ou de s'inscrire.
*   **Fichiers à créer/modifier**: `app/src/main/java/com/example/naturepei/auth/AuthActivity.kt` (ou un Fragment dédié), `app/src/main/res/layout/activity_auth.xml`
*   **Détails**:
    *   **`AuthActivity.kt`**:
        *   Gérer le flux de connexion/inscription.
        *   Utiliser `FirebaseAuth.getInstance()` pour les opérations.
        *   **Google Sign-In**: Intégrer la bibliothèque `com.google.android.gms:play-services-auth` et gérer le `ActivityResultLauncher` pour le résultat du flux Google Sign-In.
        *   **Email/Password**: Implémenter des champs de saisie pour l'email et le mot de passe, et des boutons pour "S'inscrire" et "Se connecter". Utiliser `createUserWithEmailAndPassword` et `signInWithEmailAndPassword`.
        *   **Gestion de l'état de l'utilisateur**: Rediriger l'utilisateur vers l'écran principal de l'application après une connexion réussie et le déconnecter si nécessaire.
        *   **Persistance de session**: Firebase Auth gère la persistance de session automatiquement, mais assurez-vous de vérifier `FirebaseAuth.getInstance().currentUser` au démarrage de l'application.

### 4. Développement d'une Cloud Function `onUserCreate` (Firebase Functions)
*   **Tâche**: Initialiser un document Firestore pour chaque nouvel utilisateur créé via Firebase Auth.
*   **Fichiers à modifier**: `functions/index.js`, `functions/package.json`
*   **Détails**:
    *   **`functions/package.json`**:
        *   Ajouter `firebase-admin` et `firebase-functions` comme dépendances.
    *   **`functions/index.js`**:
        *   Initialiser `firebase-admin`.
        *   Créer une fonction `onCreate` qui se déclenche à chaque création d'utilisateur Firebase Auth.
        *   Dans cette fonction, créer un nouveau document dans la collection Firestore `users` avec l'UID de l'utilisateur comme ID de document.
        *   Le document doit contenir le champ `credits` initialisé à 5, ainsi que `createdAt` et `updatedAt` avec des timestamps serveur.

    ```javascript
    // functions/index.js
    const functions = require('firebase-functions');
    const admin = require('firebase-admin');

    admin.initializeApp();
    const db = admin.firestore();

    exports.onUserCreate = functions.auth.user().onCreate(async (user) => {
      const { uid, email } = user;
      console.log(`Nouvel utilisateur créé: ${uid} (${email})`);
      try {
        await db.collection('users').doc(uid).set({
          email: email,
          credits: 5, // Les 5 crédits gratuits initiaux
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        });
        console.log(`Document utilisateur créé pour ${uid}`);
        return null;
      } catch (error) {
        console.error(`Erreur lors de la création du document utilisateur pour ${uid}:`, error);
        return null; // Retourner null pour éviter de relancer la fonction inutilement
      }
    });
    ```
    *   **Déploiement**: Une fois la fonction écrite, elle devra être déployée sur Firebase.

### 5. Mise à jour des Règles Firestore
*   **Tâche**: Sécuriser l'accès aux documents utilisateurs dans Firestore.
*   **Fichier à modifier**: `firestore.rules`
*   **Détails**:
    *   Définir des règles qui permettent à un utilisateur de lire son propre document dans la collection `users`.
    *   Interdire toute écriture directe par le client sur le champ `credits`. Ce champ doit être uniquement modifiable par les Cloud Functions (via le contexte `request.auth.token.admin == true` ou des validations spécifiques dans les fonctions).

    ```firestore
    // firestore.rules
    rules_version = '2';
    service cloud.firestore {
      match /databases/{database}/documents {
        match /users/{userId} {
          allow read: if request.auth != null && request.auth.uid == userId;
          allow write: if false; // Aucune écriture directe par le client
          // Les Cloud Functions avec admin SDK peuvent écrire sans être affectées par ces règles 'allow write'.
          // Si vous utilisez des fonctions HTTPS Callable qui modifient les crédits, la validation devra être faite DANS la fonction.
        }
      }
    }
    ```
    *   **Déploiement**: Ces règles devront être déployées sur Firebase.

### Résultat attendu à la fin de la Phase 1
*   Les utilisateurs peuvent s'inscrire/se connecter via Google ou Email/Password.
*   Après connexion, `FirebaseAuth.getInstance().currentUser` est disponible.
*   Chaque nouvel utilisateur dispose d'un document `users/{uid}` dans Firestore avec 5 crédits initiaux.
*   Les règles Firestore empêchent les modifications non autorisées des crédits.

## Liste des tâches pour la Phase 1

*   **Terminée**: Activer Google et Email/Password dans la console Firebase (Authentication > Sign-in method).
*   **Terminée**: Ajouter les dépendances Firebase Auth et Firestore aux fichiers `app/build.gradle.kts` et `gradle/libs.versions.toml`.
*   **Terminée**: Implémenter l'interface utilisateur pour l'authentification (connexion/inscription via Google Sign-In, Email/Password) dans l'application Android.
    *   `activity_auth.xml` créé et corrigé.
    *   `googleg_standard_color_18.xml` créé.
    *   `AuthActivity.kt` créé avec logique Google Sign-In et navigation.
*   **Terminée**: Développer et déployer la Cloud Function `onUserCreate` (`functions/index.js`) pour initialiser les crédits Firestore des nouveaux utilisateurs.
*   **Terminée**: Mettre à jour et déployer les règles Firestore (`firestore.rules`) pour sécuriser la collection `users`.

---

## Plan Détaillé pour la Phase 2 : Système de compteur de requêtes

### Objectif de la Phase 2
Mettre en place un système de crédits consommés à chaque action clef ("requête"), avec décrémentation sécurisée côté serveur (Cloud Functions), affichage du solde dans l'app Geronimo, et blocage des actions lorsque le solde est nul.

### 1. Définir ce qui consomme un crédit
*   **Tâche**: Lister les actions précises qui consomment 1 crédit (ex. analyse d'image/identification).
*   **Détails**: Documenter les écrans/points d'entrée concernés afin d'unifier le comportement côté app et serveur.

### 2. Architecture et choix d’API
*   **Option A (recommandée maintenant)**: Créer une **HTTPS Callable** `decrementCredits` appelée avant l’action (simples à intégrer côté Android). Ajouter aussi `getCredits` pour lire le solde.
*   **Option B (renforcement ultérieur)**: Intégrer la décrémentation directement dans l’endpoint existant (`/analyze-image`) pour enforcement serveur automatique (anti contournement). Peut être fait après validation côté client.

### 3. Cloud Functions : décrémentation atomique des crédits
*   **Fichiers**: `functions/index.js`, `functions/package.json`
*   **Exigences**:
    * Authentification requise (`context.auth.uid`).
    * Transaction Firestore pour garantir: si `credits > 0` alors `credits = credits - 1`, sinon erreur.
    * Mise à jour `updatedAt` avec `FieldValue.serverTimestamp()`.

```javascript
// Exemple (à ajouter dans functions/index.js)
const functionsV1 = require("firebase-functions/v1");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const db = getFirestore();

exports.decrementCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError(
      "unauthenticated",
      "Authentification requise."
    );
  }
  const uid = context.auth.uid;

  try {
    await db.runTransaction(async (tx) => {
      const userRef = db.collection("users").doc(uid);
      const snap = await tx.get(userRef);
      if (!snap.exists) {
        throw new functionsV1.https.HttpsError("not-found", "Document utilisateur introuvable.");
      }
      const credits = snap.get("credits") || 0;
      if (credits <= 0) {
        throw new functionsV1.https.HttpsError(
          "failed-precondition",
          "Crédits insuffisants."
        );
      }
      tx.update(userRef, {
        credits: FieldValue.increment(-1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    });
    return { success: true };
  } catch (error) {
    if (error instanceof functionsV1.https.HttpsError) throw error;
    throw new functionsV1.https.HttpsError("internal", error.message || "Erreur serveur");
  }
});

exports.getCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError(
      "unauthenticated",
      "Authentification requise."
    );
  }
  const uid = context.auth.uid;
  const snap = await db.collection("users").doc(uid).get();
  if (!snap.exists) {
    throw new functionsV1.https.HttpsError("not-found", "Document utilisateur introuvable.");
  }
  const { credits = 0 } = snap.data() || {};
  return { credits };
});
```

### 4. Règles Firestore
*   **Statut**: Les règles actuelles empêchent déjà toute écriture directe par le client sur `users/{uid}`; l’Admin SDK des Functions n’est pas bloqué.
*   **Action**: Conserver cette politique. Si ajout de endpoints HTTP publics, valider l’identité côté serveur.

### 5. Intégration Android (affichage et gating)
*   **Affichage du solde**: Écouter `users/{uid}` via `addSnapshotListener` et afficher le nombre de crédits.
*   **Gating**: Avant l’action payante, vérifier le solde local; si `> 0`, appeler `decrementCredits` puis poursuivre; sinon, bloquer et afficher un message (CTA vers achats en Phase 3).
*   **UX**: Désactiver les boutons si solde nul, surface d’erreur claire en cas d’échec de la callable.

### 6. Tests
*   **Unitaires Functions**: Chemins `credits > 0`, `credits == 0`, doc manquant.
*   **Intégration App**: Rafraîchissement temps réel, gestion erreurs callable, cohérence UI.

### Résultat attendu à la fin de la Phase 2
*   Décrémentation sécurisée par transaction Firestore.
*   Solde affiché et mis à jour en temps réel dans l’app Geronimo.
*   Actions bloquées lorsque le solde est nul (préparation Phase 3 achats).

## Liste des tâches pour la Phase 2

*   **Terminée**: Définir la liste exacte des actions consommant 1 crédit (actuellement: analyse et ré‑analyse).
*   **Terminée**: Ajouter les Cloud Functions `decrementCredits` et `getCredits` et intégrer côté app.
*   **Terminée**: Intégrer l’affichage du solde dans l’UI (listener Firestore + `TextView` permanente).
*   **Terminée**: Implémenter le gating côté app (callable `decrementCredits` avant analyse/ré‑analyse).
*   **Terminée**: Déploiement des fonctions Firebase et validation en environnement de test.
*   **À faire**: Tests (unitaires Functions, intégration app) et journalisation minimale.
