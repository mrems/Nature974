# performance.md — Plan global d’optimisation des performances

## Objectifs et métriques
- Objectifs: réduire le temps de démarrage, accélérer l’analyse d’images, diminuer la taille de l’APK, lisser l’UI.
- Métriques à suivre:
  - Taille APK release (avant/après)
  - Temps « icône → premier écran interactif »
  - Temps « capture → résultat affiché »
  - Fluidité listes (chute d’images), stabilité mémoire (pas d’OOM)

---

## Phase 1 (aujourd’hui) — 3 actions essentielles
Concentrée sur impact immédiat: poids APK, démarrage, latence d’analyse.

### 1) Build: activer R8 et shrink + logging réseau seulement en debug
- Fichiers concernés: `app/build.gradle.kts`, `proguard-rules.pro`
- Opérations:
  - Activer minification (R8) et shrink des ressources en build `release`.
  - Déplacer l’interceptor OkHttp en `debugImplementation` (pas de logs HTTP en prod).
  - Vérifier/garder les règles ProGuard pour Firebase et Retrofit/Gson si nécessaire.
- Todo
  - [ ] Activer R8 (`isMinifyEnabled=true`) et shrink (`isShrinkResources=true`) en release
  - [ ] Déplacer l’interceptor HTTP en `debugImplementation`
  - [ ] Vérifier `proguard-rules.pro` (Firebase, réflexion, GSON)
  - [ ] Construire un APK release et mesurer la taille (baseline → après)

### 2) Splash: migrer vers l’API SplashScreen (sans délai artificiel) en conservant le branding
- Fichiers concernés: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/example/naturepei/MainActivity.kt`, thèmes `res/values/*`
- Opérations:
  - Déplacer l’intent MAIN/LAUNCHER sur `MainActivity`, retirer `SplashActivity` du manifeste.
  - Initialiser le splash natif via `installSplashScreen()` dans `MainActivity`.
  - Définir le fond/logo du splash via le thème pour garder l’identité visuelle.
  - Tester transitions API 31+ et versions antérieures.
- Todo
  - [ ] Mettre MAIN/LAUNCHER sur `MainActivity`, retirer `SplashActivity` du manifeste
  - [ ] Ajouter `installSplashScreen()` à `MainActivity`
  - [ ] Configurer thème splash (fond, logo/branding)
  - [ ] Tester démarrage sur 2–3 appareils (API 29–34)

### 3) Pipeline image: capture modérée + redimension avant encodage
- Fichiers concernés: `app/src/main/java/com/example/naturepei/CameraFragment.kt`, `app/src/main/java/com/example/naturepei/ImageAnalyzer.kt`
- Opérations:
  - Choisir une taille JPEG raisonnable (bord long ≈ 2048 px) pour l’`ImageReader`.
  - Redimensionner les bitmaps à ~1024–1280 px avant encodage base64 (bande passante/mémoire).
  - Conserver la compression ~60 % (déjà en place).
  - Mesurer le temps « capture → résultat » à la fin de la phase 1.
- Todo
  - [ ] Sélectionner une taille JPEG modérée dans `CameraFragment`
  - [ ] Redimensionner lors du décodage dans `ImageAnalyzer`
  - [ ] Valider qu’orientation/EXIF restent correctes
  - [ ] Mesurer latence « capture → résultat » (avant/après)

---

## Phase 2 (priorité élevée, courte) — UI images, listeners, build perf
Cible: fluidité UI, réduction I/O et consommations en arrière-plan.

### A) Uniformiser le chargement d’images avec Coil
- Fichiers: `HistoryAdapter.kt`, `ResultActivity.kt`, `LastAnalysisFragment.kt`
- Opérations: remplacer `setImageURI` par Coil (cache, redimension) et fixer des tailles adaptées en liste/détail.
- Todo
  - [ ] Remplacer `setImageURI` par Coil dans `HistoryAdapter`
  - [ ] Remplacer `setImageURI` par Coil dans `ResultActivity`
  - [ ] Remplacer `setImageURI` par Coil dans `LastAnalysisFragment`

### B) Optimiser les ressources images
- Fichiers: `res/drawable/*`, `res/mipmap/*`
- Opérations: convertir PNG lourds (icônes, titres) en WebP ou vecteurs; supprimer ressources inutilisées.
- Todo
  - [ ] Identifier PNG lourds et convertir en WebP (lossless si nécessaire)
  - [ ] Remplacer par vecteurs quand c’est possible (icônes)
  - [ ] Nettoyer ressources non référencées

### C) Firestore: détacher le listener crédits hors écran
- Fichier: `CameraFragment.kt`
- Opération: retirer `creditsListener` en `onPause` pour éviter consommation CPU/RAM inutile hors écran.
- Todo
  - [ ] Détacher `creditsListener` en `onPause` et remettre à `null`

### D) Build perf & runtime modern
- Fichiers: `gradle.properties`, `app/build.gradle.kts`
- Opérations: activer configuration-cache, parallélisme, incrémental Kotlin; passer Java/Kotlin à 17 pour perfs runtime et API récentes.
- Todo
  - [ ] Activer `org.gradle.configuration-cache=true`
  - [ ] Activer `org.gradle.parallel=true` et `kotlin.incremental=true`
  - [ ] Passer `sourceCompatibility/targetCompatibility` et `kotlinOptions.jvmTarget` à 17

---

## Phase 3 (structure & dépendances)
Cible: scalabilité de l’historique et fiabilité libs.

### A) Listes performantes
- Fichiers: `HistoryAdapter.kt`
- Opérations: migrer vers `ListAdapter` + `DiffUtil` pour des mises à jour fines sans `notifyDataSetChanged()`, ce qui réduit le jank.
- Todo
  - [ ] Créer un `DiffUtil.ItemCallback<AnalysisEntry>`
  - [ ] Étendre `ListAdapter` et remplacer la méthode d’update par `submitList`

### B) Historique: Room + Paging (optionnel si volume important)
- Fichiers: module data (DAO/Entity/Database), `HistoryListFragment.kt`
- Opérations: stocker l’historique en base de données SQLite (via Room), paginer l’affichage pour les très longues listes.
- Todo
  - [ ] Définir Entity/DAO/Database Room
  - [ ] Remplacer SharedPreferences JSON par Room pour l’historique
  - [ ] Ajouter Paging 3 si l’historique est très volumineux

### C) Mise à jour dépendances clés
- Opérations: Mettre à jour Coroutines, Retrofit/OkHttp, Activity/Fragment KTX, Firebase BoM, Play Services vers les versions récentes stables (nécessite tests QA).
- Todo
  - [ ] Monter versions Coroutines/Retrofit/OkHttp/KTX
  - [ ] Mettre à jour Firebase BoM et Play Services
  - [ ] Effectuer QA de régression (réseau, authentification, Firestore)

### D) Locales/ressources et Compose
- Opérations: Limiter les locales packagées si pertinent (ex: `fr`) pour réduire la taille de l'APK; si Compose n'est pas utilisé, retirer le plugin et les dépendances; sinon, aligner le BOM et les versions.
- Todo
  - [ ] Limiter locales packagées si acceptable pour le produit (`resConfigs`)
  - [ ] Retirer plugin/dépendances Compose si non utilisé OU aligner BOM/versions si conservé

---

## Phase 4 (optimisations avancées)
Cible: réglages fins et robustesse.

### A) Caméra
- Opérations: Affiner la taille de preview pour une expérience fluide; envisager de limiter le FPS pour réduire la charge CPU; évaluer une migration vers CameraX pour la gestion unifiée.
- Todo
  - [ ] Tester différentes tailles de preview selon les appareils
  - [ ] Évaluer l’impact d’une limitation de FPS si bénéfique
  - [ ] Étudier CameraX (bénéfices/impacts)

### B) Géolocalisation
- Opérations: Implémenter un timeout ou un abandon rapide pour la requête `Geocoder` afin de ne pas bloquer l’UX en cas de problème réseau ou de géocodage lent.
- Todo
  - [ ] Ajouter un timeout logique au géocoder

### C) Réseau
- Opérations: Optimiser le backoff exponentiel pour les retries (calibrer jitter/délais) et éviter les retries sur les erreurs non récupérables.
- Todo
  - [ ] Affiner le backoff/jitter et exclure les statuts d’erreur non récupérables des retries

### D) Monitoring léger
- Opérations: Ajouter des logs/traces simples aux points clés (démarrage, capture→résultat, erreurs réseau) pour établir une baseline et diagnostiquer rapidement les problèmes.
- Todo
  - [ ] Intégrer des métriques et logs simples aux points clés de performance

---

## Phase 5 (outillage & finitions)
Cible: vitesse de build et hygiène du projet sur le long terme.

### A) Vitesse de build
- Opérations: Valider le bon fonctionnement des caches Gradle/Daemon; utiliser les build scans pour identifier les goulots d’étranglement; intégrer à la CI si existante.
- Todo
  - [ ] Vérifier l’efficacité du cache local et du Gradle Daemon
  - [ ] Exécuter un build scan et analyser les hotspots de performance
  - [ ] Optimiser les builds en CI (si applicable)

### B) Ressources
- Opérations: Utiliser les outils lint pour identifier les ressources non utilisées ou dupliquées; s’assurer que les images WebP sont bien lossless là où la qualité est critique.
- Todo
  - [ ] Exécuter les lints de ressources et nettoyer
  - [ ] Vérifier l’usage de WebP lossless/lossy selon les besoins

### C) Règles ProGuard/R8
- Opérations: Revue approfondie des règles ProGuard/R8 pour éviter les `-keep` trop larges et s’assurer que seul le code strictement nécessaire est conservé.
- Todo
  - [ ] Revue des règles et rationalisation des `-keep`

---

## Validation et rollback
- Tests: Effectuer des tests complets sur le démarrage, la capture/recadrage/analyse, la galerie, l’historique, l’authentification et Firestore.
- Appareils: Tester sur 2 à 3 appareils représentatifs (par exemple, Android 10 à 14).
- Rollback: Maintenir des branches Git ou des commits clairs pour chaque phase afin de permettre un retour rapide en arrière en cas de régression.

