Total des logs trouvés : 185

# Liste des logs de l'application

Ce document répertorie tous les logs trouvés dans le code de l'application, organisés par fichier.

## `app/src/main/java/com/pastaga/geronimo/CameraFragment.kt`

```kotlin
395: Log.d("CameraFragment", "Analyse annulÃ©e par le bouton retour.")
419: Log.d("CameraFragment", "Mise à jour crédits temps réel: $credits")
467: e.printStackTrace()
517: Log.e("CameraFragment", "Photo capturÃ©e Ã©chouÃ©e: ${failure.reason}")
554: Log.e("CameraFragment", "Permission camÃ©ra non accordÃ©e, impossible d'ouvrir la camÃ©ra")
558: Log.d("CameraFragment", "Ouverture camÃ©ra déjà en cours ou déjà démarrée.")
580: Log.d("CameraFragment", "Taille de capture sÃ©lectionnÃ©e: ${captureSize.width}x${captureSize.height}")
586: Log.e("CameraFragment", "Erreur d'accÃ¨s Ã  la camÃ©ra", e)
591: Log.e("CameraFragment", "Cannot open camera: Fragment not attached to activity", e)
593: Log.e("CameraFragment", "Erreur inattendue lors de l'ouverture de la camÃ©ra", e)
630: Log.d("CameraFragment", "Preview 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
644: Log.d("CameraFragment", "Preview 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
656: Log.d("CameraFragment", "Preview non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
662: Log.d("CameraFragment", "Preview fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")
691: Log.d("CameraFragment", "Capture 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
704: Log.d("CameraFragment", "Capture 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
716: Log.d("CameraFragment", "Capture non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")
724: Log.d("CameraFragment", "Capture fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")
778: Log.d("CameraFragment", "configureTransform: view=${viewWidth}x${viewHeight}, buffer=${bufferWidth}x${bufferHeight}, correction=${correctionScaleX}x${correctionScaleY}")
805: Log.d("CameraFragment", "Session de preview configurÃ©e avec succÃ¨s")
807: e.printStackTrace()
822: e.printStackTrace()
828: Log.e("CameraFragment", "CameraDevice est null")
843: Log.d("CameraFragment", "Device orientation: ${currentOrientation}°, Sensor orientation: ${sensorOrientation}°, Final JPEG_ORIENTATION: ${rotation}°")
853: e.printStackTrace()
874: Log.d("CameraFragment", "Image brute temporaire enregistrÃ©e sous: $savedUri")
940: Log.d("CameraFragment", "Aucune localisation connue.")
946: Log.e("CameraFragment", "Erreur lors de la rÃ©cupÃ©ration de la localisation: ${e.message}")
966: Log.d("CameraFragment", "Localisation obtenue: Pays = $userCountry, RÃ©gion = $userRegion")
968: Log.d("CameraFragment", "Aucune adresse trouvÃ©e pour la localisation.")
973: Log.e("CameraFragment", "Erreur de gÃ©ocodage: ${e.message}")
981: Log.d("CameraFragment", "startCrop: Démarrage du recadrage pour URI: $sourceUri")
1002: Log.e("CameraFragment", "startCrop: Erreur lors de la création du fichier temporaire source pour le recadrage (copie).", ex)
1013: Log.e("CameraFragment", "startCrop: Erreur lors de la création du fichier image pour le recadrage.", ex)
1018: Log.d("CameraFragment", "startCrop: URI de destination pour le recadrage: $destinationUri")
1046: Log.e("CameraFragment", "startCrop: Impossible de créer un fichier pour l\'URI de destination.")
1053: Log.e("CameraFragment", "startCrop: Impossible de déterminer l\'URI source pour le recadrage.")
1060: Log.e("CameraFragment", "startCrop: UCrop n\'a pas pu démarrer.", it)
1076: Log.e("CameraFragment", "handleCropError: Erreur de recadrage", error)
1088: Log.e("CameraFragment", "handleCropError: Erreur lors de la copie du fichier recadré.", ex)
1104: Log.e("CameraFragment", "handleCropError: URI de fichier recadré invalide.", error)
1119: Log.d("CameraFragment", "handleCropResult: Image recadrée enregistrée sous: $resultUri")
1121: Log.e("CameraFragment", "handleCropResult: Échec de la résolution de l\'URI du résultat du recadrage en fichier réel.", e)
1148: Log.d("CameraFragment", "displayImageInAnalysis: Affichage de l\'image recadrée: $imageUri")
1186: Log.d("CameraFragment", "startAnalysis: Démarrage de l\'analyse de l\'image")
1223: Log.e("CameraFragment", "Erreur lors de l\'upload de l\'image sur Firebase Storage", e)
1233: Log.d("CameraFragment", "URL Firebase Storage de l\'image: $imageUrl")
1238: Log.d("CameraFragment", "Requête d\'analyse envoyée. ID de l\'utilisateur: ${user.uid}")
1242: Log.e("CameraFragment", "Erreur lors de l\'envoi de la requÃªte d\'analyse.", e)
1249: Log.d("CameraFragment", "Analyse terminÃ©e avec l\'ID de doc: ${analysisRef.id}")
1259: Log.e("CameraFragment", "Impossible de lire la rÃ©ponse d\'analyse", e)
1262: Log.d("CameraFragment", "Fin d\'analyse aprÃ¨s succÃ¨s.")
1264: Log.d("CameraFragment", "Analyse annulÃ©e par l\'utilisateur.")
1268: Log.e("CameraFragment", "Analyse terminÃ©e avec une erreur: ${it.exception?.message}")
1270: Log.d("CameraFragment", "Fin d\'analyse aprÃ¨s erreur.")
1277: Log.e("CameraFragment", "Analyse annulÃ©e par manque de crÃ©dits.")
1279: Log.d("CameraFragment", "Fin d\'analyse: CrÃ©dits insuffisants.")
1281: Log.e("CameraFragment", "Analyse annulÃ©e pour une raison inconnue.")
1283: Log.d("CameraFragment", "Fin d\'analyse: Raison inconnue.")
1287: Log.e("CameraFragment", "Utilisateur non connectÃ© pour l\'analyse.")
1289: Log.d("CameraFragment", "Fin d\'analyse: Utilisateur non connectÃ©.")
1293: Log.e("CameraFragment", "Erreur gÃ©nÃ©rale lors de l\'analyse.", e)
1295: Log.d("CameraFragment", "Fin d\'analyse: Erreur gÃ©nÃ©rale.")
1304: Log.e("CameraFragment", "Erreur lors de la dÃ©crÃ©mentation des crÃ©dits", e)
1306: Log.d("CameraFragment", "CrÃ©dits dÃ©crÃ©mentÃ©s aprÃ¨s analyse.")
1310: Log.e("CameraFragment", "Erreur lors de l\'envoi de la requÃªte de dÃ©crÃ©mentation des crÃ©dits", e)
1323: Log.e("CameraFragment", "Erreur inattendue lors de la vÃ©rification des crÃ©dits", e)
1344: Log.e("CameraFragment", "Erreur Firebase Functions: code=${he.code}, message=${he.details}")
1347: Log.e("CameraFragment", "Erreur lors de l\'appel de la fonction de dÃ©crÃ©mentation des crÃ©dits Firebase", e)
1375: Log.e("CameraFragment", "Erreur lors du traitement du bitmap redimensionnÃ©", e)
1379: Log.d("CameraFragment", "Bitmap redimensionnÃ© avec succÃ¨s.")
```

## `app/src/main/java/com/pastaga/geronimo/AnalysisActivity.kt`

```kotlin
133: Log.d(TAG, "Analysis ID received: $analysisId")
137: Log.e(TAG, "No analysis ID provided. Exiting.")
152: Log.d(TAG, "Fetching analysis data for ID: $analysisId")
158: Log.e(TAG, "Document non trouvÃ© pour l\'ID d\'analyse: $analysisId")
163: Log.e(TAG, "Erreur lors de la rÃ©cupÃ©ration des donnÃ©es d\'analyse", e)
168: Log.d(TAG, "Analysis data fetched successfully for ID: $analysisId")
171: Log.d(TAG, "Updating UI with analysis data.")
182: Log.e(TAG, "Error parsing analysis document", e)
192: Log.d(TAG, "Analysis data: ${analysis.analysisId}")
198: Log.d(TAG, "Species analysis: ${speciesAnalysis.name}, Score: ${speciesAnalysis.score}")
203: Log.d(TAG, "Habitat analysis: ${habitatAnalysis.name}, Score: ${habitatAnalysis.score}")
211: Log.d(TAG, "Threat analysis: ${threatAnalysis.name}, Score: ${threatAnalysis.score}")
219: Log.d(TAG, "Other names: ${analysis.otherNames}")
223: Log.d(TAG, "Common names: ${analysis.commonNames}")
227: Log.d(TAG, "Synonyms: ${analysis.synonyms}")
231: Log.d(TAG, "Family: ${analysis.family}")
235: Log.d(TAG, "Order: ${analysis.order}")
239: Log.d(TAG, "Class: ${analysis.class_}")
243: Log.d(TAG, "Phylum: ${analysis.phylum}")
247: Log.d(TAG, "Kingdom: ${analysis.kingdom}")
251: Log.d(TAG, "Description: ${analysis.description}")
255: Log.d(TAG, "Conservation status: ${analysis.conservationStatus}")
267: Log.d(TAG, "Habitat description: ${analysis.habitatDescription}")
271: Log.d(TAG, "Threats description: ${analysis.threatsDescription}")
275: Log.d(TAG, "Local context: ${analysis.localContext}")
279: Log.d(TAG, "Scientific names: ${analysis.scientificNames}")
283: Log.d(TAG, "Wiki URL: ${analysis.wikiUrl}")
287: Log.d(TAG, "Image URL: ${analysis.imageUrl}")
293: Log.e(TAG, "Error loading image: ${analysis.imageUrl}", e)
301: Log.d(TAG, "Opening wiki URL: ${analysis.wikiUrl}")
307: Log.e(TAG, "Error opening wiki URL: ${analysis.wikiUrl}", e)
320: Log.d(TAG, "Share button clicked. Analysis ID: $analysisId")
324: Log.e(TAG, "Error creating dynamic link", e)
334: Log.d(TAG, "Dynamic Link created: $shortDynamicLink")
339: Log.d(TAG, "User ID for sharing: $userId")
344: Log.e(TAG, "User not logged in. Cannot share analysis.", e)
349: Log.e(TAG, "Error getting current user", e)
357: Log.e(TAG, "Error creating URI for share link", e)
365: Log.d(TAG, "Local Context button clicked. Lat: $latitude, Lng: $longitude")
370: Log.e(TAG, "Error parsing latitude or longitude for local context", e)
375: Log.e(TAG, "Local context coordinates are missing.", e)
385: Log.d(TAG, "Feedback button clicked. Analysis ID: $analysisId")
389: Log.e(TAG, "Error creating feedback email intent", e)
401: Log.d(TAG, "Home button clicked.")
411: Log.d(TAG, "Back button pressed, navigating to home.")
```

## `app/src/main/java/com/pastaga/geronimo/AppOnboardingActivity.kt`

```kotlin
165: Log.d("AppOnboardingActivity", "Starting intro slides")
```

## `app/src/main/java/com/pastaga/geronimo/AuthActivity.kt`

```kotlin
173: Log.e("AuthActivity", "Firebase Auth init error", e)
202: Log.d("AuthActivity", "Google Sign-In client configured.")
236: Log.d("AuthActivity", "Google Sign-In flow started.")
249: Log.e("AuthActivity", "Google Sign-In: activity result failed", it.exception)
252: Log.e("AuthActivity", "Google Sign-In: No task from activity result", e)
262: Log.d("AuthActivity", "Google Sign-In successful. Authenticating with Firebase.")
267: Log.e("AuthActivity", "Google Sign-In: token retrieval failed", e)
278: Log.d("AuthActivity", "Firebase authentication successful with Google.")
280: Log.e("AuthActivity", "Firebase authentication failed with Google.", it.exception)
300: Log.d("AuthActivity", "Anonymous sign-in successful.")
302: Log.e("AuthActivity", "Anonymous sign-in failed.", it.exception)
308: Log.d("AuthActivity", "Navigating to MainActivity.")
```

## `app/src/main/java/com/pastaga/geronimo/AuthManager.kt`

```kotlin
10: Log.d("AuthManager", "User ID: ${FirebaseAuth.getInstance().currentUser?.uid ?: "No user"}")
```

## `app/src/main/java/com/pastaga/geronimo/HistoryActivity.kt`

```kotlin
113: Log.d(TAG, "Fetching history for user: ${currentUser.uid}")
118: Log.d(TAG, "Starting to fetch more items at position: $position")
123: Log.d(TAG, "Reached end of history, no more items to load.")
136: Log.e(TAG, "Error fetching history documents: ${e.message}")
153: Log.d(TAG, "Loaded ${newItems.size} new history items.")
158: Log.d(TAG, "User clicked on item with ID: ${historyItem.analysisId}")
```

## `app/src/main/java/com/pastaga/geronimo/MainActivity.kt`

```kotlin
103: Log.d("MainActivity", "Firebase App Check initialized successfully.")
105: Log.e("MainActivity", "Firebase App Check initialization failed.", it.exception)
113: Log.d("MainActivity", "User is logged in: ${currentUser != null}")
119: Log.d("MainActivity", "Navigation to AnalysisActivity with ID: $analysisId")
121: Log.d("MainActivity", "Navigating to CameraFragment for new analysis.")
125: Log.e("MainActivity", "Unhandled Deep Link type or missing analysisId: $uri", e)
140: Log.d("MainActivity", "User ID: ${currentUser.uid}")
145: Log.e("MainActivity", "User is not logged in.")
162: Log.d("MainActivity", "User profile fetched: $userProfile")
164: Log.e("MainActivity", "Error fetching user profile", e)
172: Log.d("MainActivity", "Credits updated: $credits")
174: Log.e("MainActivity", "Error updating credits TextView", e)
```

## `app/src/main/java/com/pastaga/geronimo/OfferActivity.kt`

```kotlin
81: Log.d("OfferActivity", "Received offer ID: $offerId")
85: Log.e("OfferActivity", "No offer ID provided. Exiting.")
101: Log.d("OfferActivity", "Fetching offer data for ID: $offerId")
107: Log.e("OfferActivity", "Offer document not found for ID: $offerId")
112: Log.e("OfferActivity", "Error fetching offer data", e)
117: Log.d("OfferActivity", "Offer data fetched successfully for ID: $offerId")
120: Log.d("OfferActivity", "Updating UI with offer data.")
129: Log.e("OfferActivity", "Error parsing offer document", e)
141: Log.d(TAG, "Offer Name: ${offer.name}")
153: Log.d(TAG, "Opening Play Store URL: $playStoreUrl")
158: Log.e(TAG, "Error opening Play Store URL: $playStoreUrl", e)
175: Log.d(TAG, "Restore purchases clicked.")
186: Log.e(TAG, "Error restoring purchases: ${it.exception?.message}")
203: Log.d(TAG, "Purchase flow started for SKU: ${offer.sku}")
207: Log.e(TAG, "Error launching billing flow: ${it.debugMessage}")
214: Log.d(TAG, "Purchase successful for SKU: ${offer.sku}")
216: Log.e(TAG, "Purchase failed for SKU: ${offer.sku}. Error: ${it.debugMessage}")
231: Log.e(TAG, "Error consuming purchase: ${it.debugMessage}")
235: Log.d(TAG, "Purchase consumed successfully. Granting credits.")
240: Log.e(TAG, "Error granting credits for SKU: ${offer.sku}", e)
246: Log.e(TAG, "Error calling Firebase Function to grant credits", e)
250: Log.d(TAG, "Credits granted successfully.")
253: Log.d(TAG, "Home button clicked.")
262: Log.d(TAG, "Back button pressed, navigating to home.")
```

## `app/src/main/java/com/pastaga/geronimo/PhotoManualActivity.kt`

```kotlin
165: Log.d(TAG, "Picture taken or selected. Starting analysis process.")
167: Log.e(TAG, "Selected image URI is null.")
175: Log.e(TAG, "Error getting image URI from intent", e)
```

## `app/src/main/java/com/pastaga/geronimo/fragments/GalleryBottomSheetFragment.kt`

```kotlin
123: Log.d(TAG, "Selected gallery image URI: $selectedImageUri")
129: Log.e(TAG, "Error processing image from gallery", e)
```

## `app/src/main/java/com/pastaga/geronimo/fragments/ManualFragment.kt`

```kotlin
152: Log.d(TAG, "Photo captured or selected. URI: $imageUri")
156: Log.e(TAG, "No image URI received.")
```

## `app/src/main/java/com/pastaga/geronimo/fragments/SettingsFragment.kt`

```kotlin
109: Log.d(TAG, "User logged out successfully.")
112: Log.e(TAG, "Error logging out user", e)
118: Log.e(TAG, "User not logged in, cannot log out.")
130: Log.d(TAG, "Delete user clicked.")
136: Log.e(TAG, "Error deleting user account", e)
139: Log.d(TAG, "User account deleted successfully.")
```

## `poubelle/fragments/GalleryBottomSheetFragment.kt`

```kotlin
123: Log.d(TAG, "Selected gallery image URI: $selectedImageUri")
129: Log.e(TAG, "Error processing image from gallery", e)
```

## `poubelle/fragments/ManualFragment.kt`

```kotlin
152: Log.d(TAG, "Photo captured or selected. URI: $imageUri")
156: Log.e(TAG, "No image URI received.")
```