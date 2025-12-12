# Plan détaillé — renommage complet **Geronimo → Geki** (nouvelle app Firebase + nouvelle fiche Play Store)

> **Important** : ce document est un **plan**.  
> **Aucune action n’est exécutée automatiquement** tant que tu ne me donnes pas explicitement le feu vert.

## Objectif

Faire un renommage **total** du projet :

- **Nom de l’app** (affiché sur le launcher) : **Geki**
- **Package / namespace / applicationId** : passer de `com.pastaga.geronimo` vers **`com.geronimo.geki`** (validé)
- **Thèmes** Android : `Theme.Geronimo.*` → `Theme.Geki.*`
- **Ressources & textes** : supprimer/renommer les occurrences “Geronimo” (contentDescription, ids, commentaires UI)
- **Backend/Firebase** : recréer une **nouvelle app Firebase** + nouvelles configurations (Google Sign-In, Auth, Firestore, Functions)
- **Play Store** : créer une **nouvelle fiche** (nouvelle application) liée au nouveau package

## Pré-requis (à valider avant de toucher au code)

### A. Choix définitifs (tu valides)
- **Nouveau package** : **`com.geronimo.geki`** ✅
- **Nom affiché** : `Geki` (majuscule/minuscule OK ?)
- **Nom projet Gradle** : **`rootProject.name = "Geki"`** ✅
- **Nom Firebase project** (ex) : `geki-xxxxx` (tu choisis l’id)
- **Nom de domaine Functions** : sera différent (nouveau projet). On mettra à jour l’URL côté Android.

### B. Sauvegarde / sécurité (recommandé)
- Faire un commit Git avant le renommage (si le repo est versionné).
- Conserver les anciens fichiers (ex: `google-services.json` actuel) dans un dossier d’archive si besoin.

## Cartographie — où “Geronimo” existe actuellement (dans TON repo)

### 1) Android — applicationId / namespace
- `app/build.gradle.kts`
  - `namespace = "com.pastaga.geronimo"`
  - `applicationId = "com.pastaga.geronimo"`
  - ➜ objectif : `namespace = "com.geronimo.geki"` et `applicationId = "com.geronimo.geki"`

### 2) Android — code Kotlin (packages + dossiers)
- Dossier principal :
  - `app/src/main/java/com/pastaga/geronimo/**`
- Tous les fichiers `.kt` déclarent `package com.pastaga.geronimo`
- Certains imports internes : `import com.pastaga.geronimo.R`
  - ➜ objectif : dossier `app/src/main/java/com/geronimo/geki/**`, `package com.geronimo.geki`, imports `com.geronimo.geki.R`

### 3) Android — Manifest (label + thèmes)
- `app/src/main/AndroidManifest.xml`
  - `android:label="Geki"` (label en dur)
  - `android:theme="@style/Theme.Geronimo"` (+ Splash/Auth)

### 4) Android — thèmes & styles
- `app/src/main/res/values/themes.xml`
- `app/src/main/res/values-night/themes.xml`
- `app/src/main/res/values-v31/themes.xml`
  - `Theme.Geronimo.*`
  - `TextAppearance.Geronimo.*`

### 5) Android — Proguard/R8
- `app/proguard-rules.pro`
  - règles `-keep ... com.pastaga.geronimo...`

### 6) Android — ressources/layouts (UI + accessibilité)
Occurences “Geronimo” :
- `app/src/main/res/layout/activity_purchase.xml`
  - ids `@+id/geronimo_*`
  - contentDescription “Titre Geronimo”, “Image du personnage Geronimo”
- `app/src/main/res/layout/fragment_intro_page1.xml`
  - contentDescription “Geronimo Logo”
- `app/src/main/res/layout/fragment_intro_page4.xml`
  - id `@+id/geronimo_splash_image_view`
  - contentDescription “Geronimo Splash”
- `app/src/main/res/layout/fragment_intro_page5.xml`
  - contentDescription “Logo Geronimo”

### 7) Firebase config (Android)
- `app/google-services.json`
  - `project_id: "geronimo-7224a"`
  - `package_name: "com.pastaga.geronimo"`
  - ➜ objectif : nouveau `google-services.json` lié à `com.geronimo.geki`

### 8) Backend/Functions — package attendu
- `functions/index.js`
  - `const packageName = 'com.pastaga.geronimo';`
  - ➜ objectif : `const packageName = 'com.geronimo.geki';`

### 9) Backend URL codée en dur côté Android
- `app/src/main/java/com/pastaga/geronimo/ImageAnalyzer.kt`
  - `https://europe-west9-geronimo-7224a.cloudfunctions.net/api/`

## Stratégie recommandée (ordre qui évite les pièges)

### Étape 0 — “Freeze” (préparation)
1. Confirmer les choix (package, nom affiché, nom Firebase).
2. (Recommandé) Commit de sauvegarde.
3. Noter les infos à reporter :
   - SHA-1 / SHA-256 du keystore debug et release (pour Google Sign-In).
   - VersionCode / VersionName (tu peux repartir à 1 si c’est une nouvelle app Play).

### Étape 1 — Créer le **nouveau** projet Firebase (hors code)
À faire sur la console Firebase :
1. Créer un **nouveau projet** Firebase (ex: `geki-xxxxx`).
2. Ajouter une **app Android** dans ce projet :
   - Package = **`com.geronimo.geki`**
3. Configurer :
   - Authentication (Google)
   - Firestore (et règles)
   - Cloud Functions (si utilisées)
4. Télécharger le **nouveau** `google-services.json`.
5. Mettre à jour les paramètres Google Sign-In :
   - Ajouter SHA-1/SHA-256 debug + release

**Checkpoint** : tu dois avoir un `google-services.json` cohérent avec le **nouveau package**.

### Étape 2 — Renommage Android “structurel” (package + Gradle)
Dans le code (quand tu me donnes le feu vert) :
1. Modifier `app/build.gradle.kts`
   - `namespace` → `com.geronimo.geki`
   - `applicationId` → `com.geronimo.geki`
2. Déplacer les fichiers Kotlin :
   - `app/src/main/java/com/pastaga/geronimo/**` → `app/src/main/java/com/geronimo/geki/**`
3. Mettre à jour toutes les déclarations :
   - `package com.pastaga.geronimo` → `package com.geronimo.geki`
4. Mettre à jour imports internes :
   - `import com.pastaga.geronimo.R` → `import com.geronimo.geki.R`
5. Mettre à jour toute référence textuelle `com.pastaga.geronimo` restante.

**Checkpoint** : le projet compile localement (Debug) avec le nouveau package.

### Étape 3 — Thèmes : `Geronimo` → `Geki`
1. Renommer les styles dans :
   - `values/themes.xml`
   - `values-night/themes.xml`
   - `values-v31/themes.xml`
2. Mettre à jour les références :
   - `AndroidManifest.xml` : `@style/Theme.Geronimo...` → `@style/Theme.Geki...`
   - Code Kotlin : `R.style.Theme_Geronimo_Dialog` → `R.style.Theme_Geki_Dialog`

**Checkpoint** : pas de crash au lancement (thème introuvable = crash immédiat).

### Étape 4 — Nom affiché de l’app (propre)
1. Éviter `android:label="Geki"` en dur :
   - Ajouter `@string/app_name` dans toutes les langues (au minimum `values/` + `values-fr/`)
2. Mettre dans le manifest :
   - `android:label="@string/app_name"`

**Checkpoint** : le nom affiché sous l’icône est correct.

### Étape 5 — Nettoyage UI : ids / contentDescription / textes “Geronimo”
1. Remplacer les contentDescription “Geronimo” → “Geki” (accessibilité).
2. (Optionnel mais “renommage total”) Renommer les ids `@+id/geronimo_*` → `@+id/geki_*`
   - Attention : cela implique de modifier le code qui les référence (ViewBinding / findViewById).

**Checkpoint** : aucune référence XML/Kotlin cassée.

### Étape 6 — Proguard/R8
1. Mettre à jour `app/proguard-rules.pro`
   - `com.pastaga.geronimo` → `com.geronimo.geki`

### Étape 7 — Brancher le nouveau Firebase dans l’app
1. Remplacer `app/google-services.json` par celui du nouveau projet Firebase.
2. Vérifier que le plugin Google Services est OK (déjà présent dans `app/build.gradle.kts`).
3. Vérifier Google Sign-In (certificats + client id).

**Checkpoint** : Auth Google fonctionne.

### Étape 8 — Backend / Cloud Functions
Deux possibilités :

#### Option A — Tu recrées aussi Functions dans le nouveau projet (recommandé)
1. Déployer Functions sur le nouveau projet Firebase.
2. Obtenir la nouvelle URL :
   - `https://<region>-<new-project>.cloudfunctions.net/api/`
3. Mettre à jour côté Android :
   - `ImageAnalyzer.kt` : nouvelle `BACKEND_API_URL`
4. Mettre à jour côté Functions :
   - `functions/index.js` : `packageName = 'com.geronimo.geki'`

#### Option B — Tu gardes l’ancien backend (possible mais incohérent avec “nouvelle app”)
1. Conserver l’URL actuelle côté Android.
2. Conserver la validation package côté Functions (ou l’élargir).

**Checkpoint** : un appel API complet fonctionne (analyse OK).

### Étape 9 — Play Console (nouvelle app)
1. Créer une nouvelle application dans Google Play Console.
2. Renseigner le nouveau package `com.pastaga.geki`.
   - ➜ objectif : **`com.geronimo.geki`**
3. Upload AAB/APK signé (release).
4. Configurer in-app billing / abonnements si utilisés (nouveaux produits si nécessaire).

## Checklist de validation finale (avant release)
- L’app s’installe (debug & release).
- Lancement OK (pas d’erreur de thème).
- Google Sign-In OK.
- Firestore OK (lecture/écriture selon règles).
- Cloud Functions OK (URL + auth/validation package).
- Billing OK (si activé).
- Pas de “Geronimo” résiduel dans UI/strings (sauf si voulu).

## Ce dont j’ai besoin de toi pour commencer (quand tu voudras)
1. ✅ Package validé : `com.geronimo.geki`
2. ✅ `rootProject.name` à renommer en `Geki`
3. Il reste à trancher : Option A (nouveau backend Functions) ou conservation temporaire de l’ancien.

---
Dernière note : quand tu me donnes l’accord, je peux faire le renommage en “petits lots” avec checkpoints (build/launch entre chaque étape) pour éviter les gros breakages.


