# Analyse performances — NaturePei / Geki (Android)

Ce document synthétise une analyse “statique” du dépôt (code + configuration) avec des **pistes concrètes** pour améliorer les **performances réelles** (temps de démarrage, fluidité UI, mémoire, réseau, batterie) et la **performance perçue** (ressenti utilisateur).

> Contrainte respectée : ce rapport ne change pas ton code. Il propose des actions et un plan de mesure.

---

## 1) Résumé exécutif (priorités)

### P0 — gains rapides et très probables
- **Supprimer le délai fixe du splash (2s)** : `SplashActivity` attend 2000ms avant de naviguer. Ça pénalise directement la performance perçue, même si l’app est déjà prête.
- **Rétrécissement R8/Proguard probablement neutralisé** : `proguard-rules.pro` contient des règles `-keep` très larges (dont `-keep public class com.pastaga.geronimo.** { *; }` et `-keep class com.google.android.gms.** { *; }`). Résultat probable : **APK plus gros**, **moins de code supprimé**, **startup plus lourd**.
- **Écritures/lectures de l’historique trop coûteuses et parfois sur le thread UI** : l’historique est stocké en **JSON dans SharedPreferences** et est (dé)sérialisé en liste complète à chaque modification ; certains appels utilisent `commit()` (synchrone) et/ou se produisent côté UI dans `CameraFragment`.

### P1 — gains importants (mais demandent un peu de refactor)
- **Persistance d’historique** : migrer de “liste JSON en SharedPreferences” vers **Room** (ou DataStore + modèle structuré) pour éviter la re-sérialisation complète et permettre index, pagination, limites.
- **Optimisation image** : éviter de **décoder l’image pleine résolution** pour l’analyse IA ; décoder directement en taille cible (sampling).
- **RecyclerView** : `HistoryAdapter.updateItems()` fait `notifyDataSetChanged()` (coûteux). Passer à `ListAdapter + DiffUtil` et corriger quelques micro-coûts (date formatter).

### P2 — gains avancés / industrialisation
- **Baseline Profile / Macrobenchmark** : améliorer le cold start et la navigation réelle via profils.
- **Instrumentation de mesure** : dashboards (startup, jank, ANR, OOM) via outils (métriques Play, Perfetto/Trace, etc.).

---

## 2) Périmètre et méthode

### Ce qui a été analysé
- Gradle / dépendances : `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`
- Points d’entrée : `AndroidManifest.xml`, `SplashActivity`, `MainActivity`
- Chemins chauds : `CameraFragment` (caméra + Firestore + analyse), `ImageAnalyzer` (réseau + encodage), `AnalysisHistoryManager` (persistance), UI liste/galerie (`HistoryListFragment`, `HistoryAdapter`, `GalleryFragment`, `GalleryAdapter`), affichage résultats (`ResultActivity`, `LastAnalysisFragment`, `FullScreenImageActivity`).

### Ce qui n’a pas été fait (volontairement)
- Aucun build/profiling exécuté ; pas de modifications de code.
- Les recommandations sont donc “fortement probables”, mais certaines doivent être **validées par mesures** (section 8).

---

## 3) Démarrage (startup) & performance perçue

### Constat
- `SplashActivity` applique un **délai fixe de 2 secondes** via `Handler.postDelayed(...)`.
  - Même si l’app a fini ses initialisations plus tôt, l’utilisateur attend quand même.
  - Sur appareils lents, tu cumules “vrai cold start” + “délai artificiel”.

### Recommandations
- **Retirer le délai fixe** : naviguer dès que la décision “où aller” est connue (auth/intro/main).
- Utiliser l’API **SplashScreen Android 12+** (lib `androidx.core:core-splashscreen`) :
  - tu gardes un splash visuel propre sans pénaliser inutilement.
  - possibilité de condition `setKeepOnScreenCondition { ... }` si tu as *vraiment* un chargement bloquant (à minimiser).
- Si tu veux une transition agréable : préférer un **skeleton**/placeholder dans l’écran suivant, plutôt qu’un délai global.

---

## 4) Taille APK / DEX / R8 (impact direct sur startup et mémoire)

### Constat
Le module `app` active déjà :
- `release { isMinifyEnabled = true; isShrinkResources = true }`

Mais tes règles R8/Proguard contiennent des `-keep` très larges, notamment :
- `-keep public class com.pastaga.geronimo.** { *; }`
- `-keep class com.google.android.gms.** { *; }`
- d’autres keep “généraux” sur `Application/Activity/Service/...`

### Risques
- **Shrinking quasi annulé** (surtout pour ton propre code).
- **APK / DEX plus gros** → **démarrage plus lent**, plus de pages mémoire, et potentiellement plus de GC.
- Moins d’optimisation R8 possible (inlining, dead code, etc.).

### Recommandations
- **Réduire drastiquement les `-keep`** :
  - Garde uniquement ce qui est **réellement requis par réflexion** (Gson, Firebase/Play Services, UCrop, etc.).
  - Pour tes propres classes, ne “keep” que :
    - les composants référencés par le Manifest (Activities/Providers, etc.)
    - les modèles réellement désérialisés par Gson si nécessaire (ou mieux : passer à Kotlin serialization / Moshi avec codegen).
- Approche sûre :
  - enlever `-keep public class com.pastaga.geronimo.** { *; }`
  - remplacer par des règles ciblées, ou des annotations `@Keep` sur les classes/membres nécessaires.
- Pour Play Services : éviter `-keep class com.google.android.gms.** { *; }` (trop large). Les libs Google fournissent généralement leurs consumer rules.

---

## 5) Réseau / API d’analyse image

### Constat (`ImageAnalyzer`)
- Retrofit + OkHttp, timeouts à **150s** (connect/read/write).
- `retryOnConnectionFailure(false)`.
- L’image est convertie en Base64 et envoyée.

### Risques / coûts
- Une requête “bloquée” peut monopoliser des ressources longtemps (threads, attente utilisateur).
- En cas de réseau instable, pas de retry : UX potentiellement moins bonne (pas forcément perf, mais ressenti).

### Recommandations
- **Timeouts** :
  - réduire les timeouts (ex: 20–40s) + stratégie UX (retry explicite, bouton annuler, etc.).
  - utiliser `callTimeout` (timeout global) et s’assurer que l’annulation coroutine annule la requête.
- **Cache / déduplication** (si pertinent) :
  - si l’utilisateur relance l’analyse d’une même photo, mettre en place un cache léger (hash image -> résultat) côté client ou côté backend.
- **Logging** :
  - `logging-interceptor` est déjà en `debugImplementation` : bien.

---

## 6) Traitement image & mémoire (risque OOM / latences)

### Constat
Dans `ImageAnalyzer.analyzeImage()` :
- l’image est d’abord **décodée en bitmap** depuis l’URI, puis redimensionnée ensuite.
- compression puis Base64.

### Risques
- Décoder une photo moderne (12–48MP) en bitmap plein format peut créer un gros pic mémoire et CPU, même si tu redimensionnes ensuite.

### Recommandations
- **Décoder directement en taille cible** :
  - API 28+ : `ImageDecoder` permet de fixer une target size via `OnHeaderDecodedListener`.
  - API < 28 : utiliser `BitmapFactory.Options` avec `inJustDecodeBounds` + `inSampleSize`.
- Objectif : arriver au maxDimension **sans jamais allouer** le bitmap pleine résolution.

---

## 7) Persistance historique (gros levier perf)

### Constat (`AnalysisHistoryManager`)
- Historique stocké dans SharedPreferences sous forme d’une **liste JSON complète** :
  - `saveAnalysisEntry()` fait `getAnalysisHistory()` (donc `gson.fromJson(...)`) puis `toJson(historyList)` puis `apply()`.
- `updateAnalysisEntry()` et `saveLastViewedCard()` utilisent `commit()` (synchrone).
- Dans `CameraFragment`, après une analyse, tu construis l’entrée puis tu appelles `saveAnalysisEntry()` et `saveLastViewedCard()` **dans un bloc `withContext(Dispatchers.Main)`**.

### Risques
- Plus l’historique grossit, plus chaque écriture devient chère (O(n)).
- `commit()` peut provoquer des **micro-freezes**.
- Écritures/JSON sur le thread UI = **risque de jank** sur devices moyens/bas.

### Recommandations “quick win” (sans tout migrer)
- Garantir que toute lecture/écriture d’historique est faite sur `Dispatchers.IO`.
- Remplacer `commit()` par `apply()` si la synchronisation stricte n’est pas indispensable (sinon : assurer `commit()` mais uniquement en IO).
- Limiter l’historique (ex: garder 200 entrées max) pour borner les coûts.

### Recommandation structurelle (fortement conseillée)
- Migrer vers **Room** :
  - insertion O(1), requêtes triées par timestamp, suppression, update, pagination.
  - permet aussi de stocker des champs sans (dé)sérialisation complète.

---

## 8) UI : listes, images, jank

### Galerie (`GalleryFragment` / `GalleryAdapter`)
Points positifs :
- Chargement MediaStore effectué en IO.
- Limite à 300 images.

Pistes d’amélioration :
- Dans Coil, préciser la **taille** cible des thumbnails et/ou une stratégie “thumbnail” pour éviter des décodages trop lourds.
- Ajouter placeholder / error pour lisser la perception et éviter des “blancs”.

### Historique (`HistoryAdapter`)
Constat :
- `updateItems()` fait `notifyDataSetChanged()` → rebind complet.
- `SimpleDateFormat` est créé dans `bind()` (par item).

Recommandations :
- Passer à `ListAdapter<AnalysisEntry, ...>` + `DiffUtil` (updates partiels).
- Utiliser un formatter réutilisable (ex: `lazy`/singleton) ou `java.time` si possible.

---

## 9) Firestore / crédits (coûts temps réel + fuites potentielles)

### Constat (`CameraFragment`)
- `setupCreditsListener()` installe un `addSnapshotListener` sur le doc utilisateur et met à jour l’UI.
- Le listener est remplacé si rappelé, mais **il n’est pas explicitement retiré en onStop/onDestroyView** (à vérifier côté lifecycle exact : le champ `creditsListener` est un `ListenerRegistration` persistant).

### Risques
- Listener qui reste actif trop longtemps → consommation réseau/batterie, callbacks, et risque de fuite si le Fragment est détruit.

### Recommandations
- Retirer le listener dans `onStop()` ou `onDestroyView()` : `creditsListener?.remove(); creditsListener = null`.
- Envisager une stratégie “pull + cache” si l’UI n’a pas besoin de temps réel.

---

## 10) “Compose activé” alors que l’UI est majoritairement View/XML

### Constat
Dans `app/build.gradle.kts` :
- `buildFeatures { compose = true; viewBinding = true }`
- dépendances Compose (BOM + ui/material3, etc.)
Alors que l’app analysée utilise surtout `AppCompatActivity`, `Fragment`, layouts XML, ViewPager2…

### Risques
- Augmentation du poids dépendances / méthode count / temps de build.
- Potentiel conflit de versions (BOM Compose défini en dur).

### Recommandations
- Si Compose n’est pas utilisé : **désactiver compose** et retirer les deps Compose.
- Si Compose est prévu : centraliser les versions via `libs.versions.toml` (tu as déjà un catalog) et éviter un BOM “en dur” différent.

---

## 11) Plan de mesure (avant/après)

### Mesures “simples” (sans code)
- **Temps de démarrage** : `adb shell am start -W <package>/<activity>`
  - mesurer cold start et warm start (plusieurs runs).
- **Jank** : Android Studio Profiler / “Jank stats” (ou Perfetto).
- **Mémoire** : pics lors d’analyse d’une photo haute résolution (observer allocations bitmap).

### Mesures “sérieuses” (recommandées)
- **Macrobenchmark** (startup + parcours clé) :
  - scénario : launch → caméra → capture → analyse → résultat → retour → historique.
- **Baseline Profile** :
  - générer et livrer un profil pour les chemins critiques.

### KPI à suivre
- **Cold start** (P50/P90)
- **Jank / frames > 16ms et > 32ms**
- **Peak RSS / heap** pendant analyse image
- **Taille APK / DEX** (et classes count)
- **Réseau** : temps de requête analyse, volume upload (Base64), taux d’échec

---

## 12) Checklist d’actions proposées (prête à exécuter)

### P0 (1–2h)
- Enlever le délai fixe de `SplashActivity` et passer à SplashScreen API.
- Nettoyer `proguard-rules.pro` : supprimer les `-keep` globaux et itérer jusqu’à obtenir un shrink réel sans casser la prod.
- Déplacer toute écriture d’historique (JSON/SharedPrefs) hors du Main thread, remplacer `commit()` côté UI.

### P1 (0.5–2 jours)
- Migrer l’historique vers Room (+ limite/pagination).
- Améliorer le pipeline image : décodage avec sampling (pas de bitmap pleine résolution).
- Adapter historique en `ListAdapter + DiffUtil` + micro-optimisations binder.

### P2 (1–3 jours)
- Macrobenchmark + Baseline Profile.
- Instrumentation (traces startup, jank, erreurs réseau) et suivi dans la durée.

---

## 13) Questions (pour affiner et prioriser)

Répondre à ces questions permet de proposer des recommandations encore plus précises :
1) Cible principale : **Android bas/milieu de gamme** ou plutôt haut de gamme ?
2) Volume d’historique typique : 20, 200, 2000 entrées ?
3) Les photos analysées sont plutôt 12MP+ ? (risque OOM)
4) Objectif principal : **startup**, **fluidité**, **batterie**, **réseau**, ou tout ?
5) Compose est-il réellement prévu ou c’est un reliquat ?


