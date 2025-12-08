Nombre total de logs dans l'app (hors dossier `functions`) : **70**

---

## CameraFragment.kt (`app/src/main/java/com/pastaga/geronimo/CameraFragment.kt`)

- **Ligne 395** — `Log.d("CameraFragment", "Analyse annulÃ©e par le bouton retour.")`
- **Ligne 419** — `Log.d("CameraFragment", "Mise à jour crédits temps réel: $credits")`
- **Ligne 517** — `Log.e("CameraFragment", "Photo capturÃ©e Ã©chouÃ©e: ${failure.reason}")`
- **Ligne 554** — `Log.e("CameraFragment", "Permission camÃ©ra non accordÃ©e, impossible d'ouvrir la camÃ©ra")`
- **Ligne 558** — `Log.d("CameraFragment", "Ouverture camÃ©ra déjà en cours ou déjà démarrée.")`
- **Ligne 580** — `Log.d("CameraFragment", "Taille de capture sÃ©lectionnÃ©e: ${captureSize.width}x${captureSize.height}")`
- **Ligne 586** — `Log.e("CameraFragment", "Erreur d'accÃ¨s Ã  la camÃ©ra", e)`
- **Ligne 591** — `Log.e("CameraFragment", "Cannot open camera: Fragment not attached to activity", e)`
- **Ligne 593** — `Log.e("CameraFragment", "Erreur inattendue lors de l'ouverture de la camÃ©ra", e)`
- **Ligne 630** — `Log.d("CameraFragment", "Preview 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 644** — `Log.d("CameraFragment", "Preview 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 656** — `Log.d("CameraFragment", "Preview non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 662** — `Log.d("CameraFragment", "Preview fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")`
- **Ligne 691** — `Log.d("CameraFragment", "Capture 16:9 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 704** — `Log.d("CameraFragment", "Capture 4:3 s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 716** — `Log.d("CameraFragment", "Capture non-carr\u00e9e s\u00e9lectionn\u00e9e: ${pick.width}x${pick.height}")`
- **Ligne 724** — `Log.d("CameraFragment", "Capture fallback s\u00e9lectionn\u00e9e: ${fallback.width}x${fallback.height}")`
- **Ligne 778** — `Log.d("CameraFragment", "configureTransform: view=${viewWidth}x${viewHeight}, buffer=${bufferWidth}x${bufferHeight}, correction=${correctionScaleX}x${correctionScaleY}")`
- **Ligne 805** — `Log.d("CameraFragment", "Session de preview configurÃ©e avec succÃ¨s")`
- **Ligne 828** — `Log.e("CameraFragment", "CameraDevice est null")`
- **Ligne 843** — `Log.d("CameraFragment", "Device orientation: ${currentOrientation}°, Sensor orientation: ${sensorOrientation}°, Final JPEG_ORIENTATION: ${rotation}°")`
- **Ligne 874** — `Log.d("CameraFragment", "Image brute temporaire enregistrÃ©e sous: $savedUri")`
- **Ligne 940** — `Log.d("CameraFragment", "Aucune localisation connue.")`
- **Ligne 946** — `Log.e("CameraFragment", "Erreur lors de la rÃ©cupÃ©ration de la localisation: ${e.message}")`
- **Ligne 966** — `Log.d("CameraFragment", "Localisation obtenue: Pays = $userCountry, RÃ©gion = $userRegion")`
- **Ligne 968** — `Log.d("CameraFragment", "Aucune adresse trouvÃ©e pour la localisation.")`
- **Ligne 973** — `Log.e("CameraFragment", "Erreur de gÃ©ocodage: ${e.message}")`
- **Ligne 981** — `Log.d("CameraFragment", "startCrop: Démarrage du recadrage pour URI: $sourceUri")`
- **Ligne 1002** — `Log.e("CameraFragment", "startCrop: Erreur lors de la création du fichier temporaire source pour le recadrage (copie).", ex)`
- **Ligne 1013** — `Log.e("CameraFragment", "startCrop: Erreur lors de la création du fichier image pour le recadrage.", ex)`
- **Ligne 1018** — `Log.d("CameraFragment", "startCrop: URI de destination pour le recadrage: $destinationUri")`
- **Ligne 1046** — `Log.e("CameraFragment", "startCrop: Impossible de créer un fichier pour l'URI de destination.")`
- **Ligne 1053** — `Log.e("CameraFragment", "startCrop: Impossible de déterminer l'URI source pour le recadrage.")`
- **Ligne 1063** — `Log.e("CameraFragment", "createImageFile: Le rÃ©pertoire de stockage externe est null.")`
- **Ligne 1082** — `Log.d("CameraFragment", "Result URI: $resultUri")`
- **Ligne 1110** — `Log.d("CameraFragment", "Image recadrÃ©e sauvegardÃ©e dans la galerie publique: $uriToSave")`
- **Ligne 1116** — `Log.e("CameraFragment", "Le rÃ©pertoire de stockage privÃ© externe est null.")`
- **Ligne 1129** — `Log.d("CameraFragment", "Image recadrÃ©e copiÃ©e dans le stockage privÃ©: $privateCopyUri")`
- **Ligne 1132** — `Log.e("CameraFragment", "Erreur lors de la sauvegarde/copie de l'image recadrÃ©e.", e)`
- **Ligne 1141** — `Log.d("CameraFragment", "Fichier temporaire UCrop supprimÃ©: ${uCropTempFile.absolutePath}")`
- **Ligne 1143** — `Log.e("CameraFragment", "Impossible de supprimer le fichier temporaire UCrop: ${uCropTempFile.absolutePath}")`
- **Ligne 1160** — `Log.e("CameraFragment", "UCrop: data est null pour RESULT_OK")`
- **Ligne 1169** — `Log.e("CameraFragment", "Erreur de recadrage: ${cropError?.message}")`
- **Ligne 1177** — `Log.d("CameraFragment", "Fichier temporaire source UCrop supprimé après échec recadrage: ${fileToDelete.absolutePath}")`
- **Ligne 1179** — `Log.e("CameraFragment", "Impossible de supprimer le fichier temporaire source UCrop: ${fileToDelete.absolutePath}")`
- **Ligne 1188** — `Log.d("CameraFragment", "Recadrage annulé.")`
- **Ligne 1196** — `Log.d("CameraFragment", "Fichier temporaire source UCrop supprimé après annulation recadrage: ${fileToDelete.absolutePath}")`
- **Ligne 1198** — `Log.e("CameraFragment", "Impossible de supprimer le fichier temporaire source UCrop: ${fileToDelete.absolutePath}")`
- **Ligne 1235** — `Log.e("CameraFragment", "Erreur: analyzeImageWithGemini appelé sans utilisateur connecté.")`
- **Ligne 1238** — `Log.d("CameraFragment", "Utilisateur connecté: ${currentUser.uid}")`
- **Ligne 1242** — `Log.d("GEMINI_LANG_DEBUG", "Langue du système: $systemLanguage")`
- **Ligne 1255** — `Log.d("CameraFragment", "Redirection vers PurchaseActivity: crédits épuisés (vérification client).")`
- **Ligne 1270** — `Log.d("CameraFragment", "Redirection vers PurchaseActivity: InsufficientCreditsException.")`
- **Ligne 1275** — `Log.d("CameraFragment", "Analyse annulÃ©e avant la mise Ã  jour UI.")`
- **Ligne 1282** — `Log.d("NaturePei_Debug", "[CameraFragment] Danger de la réponse de l'API: ${response.danger}")`
- **Ligne 1330** — `Log.d("CameraFragment", "Exception attrapÃ©e mais coroutine dÃ©jÃ  annulÃ©e.")`
- **Ligne 1426** — `Log.e("CameraFragment", "Erreur lors de la demande de l'API d'évaluation in-app: ${request.exception?.message}")`

---

## ImageAnalyzer.kt (`app/src/main/java/com/pastaga/geronimo/ImageAnalyzer.kt`)

- **Ligne 86** — `Log.d("ImageAnalyzer", "Redimensionnement: ${width}x${height} -> ${newWidth}x${newHeight}")`
- **Ligne 169** — `Log.d("NaturePei_Debug", "[ImageAnalyzer] Réponse API réussie: ${response?.danger}")`
- **Ligne 179** — `Log.e("NaturePei_Debug", "[ImageAnalyzer] Erreur HTTP lors de l'analyse: ", e)`
- **Ligne 182** — `Log.e("NaturePei_Debug", "[ImageAnalyzer] Erreur générale lors de l'analyse: ", e)`

---

## HistoryListFragment.kt (`app/src/main/java/com/pastaga/geronimo/HistoryListFragment.kt`)

- **Ligne 206** — `Log.e("HistoryListFragment", "Erreur: Ré-analyse appelée sans utilisateur connecté.")`
- **Ligne 209** — `Log.d("HistoryListFragment", "Utilisateur connecté: ${currentUser.uid}")`
- **Ligne 223** — `Log.d("HistoryListFragment", "Redirection vers PurchaseActivity: crédits épuisés (vérification client).")`
- **Ligne 245** — `Log.d("HistoryListFragment", "Redirection vers PurchaseActivity: InsufficientCreditsException.")`

---

## ResultActivity.kt (`app/src/main/java/com/pastaga/geronimo/ResultActivity.kt`)

- **Ligne 93** — `Log.d("NaturePei_Debug", "Confidence Score received: ${currentEntry?.confidenceScore}")`

---

## CreditsManager.kt (`app/src/main/java/com/pastaga/geronimo/CreditsManager.kt`)

- **Ligne 14** — `android.util.Log.d("CreditsManager", "Appel decrementCredits: début")`
- **Ligne 19** — `android.util.Log.d("CreditsManager", "Appel decrementCredits: succès")`
- **Ligne 21** — `android.util.Log.e("CreditsManager", "Appel decrementCredits: erreur", e)`

---

## LoadingDialogFragment.kt (`app/src/main/java/com/pastaga/geronimo/LoadingDialogFragment.kt`)

- **Ligne 33** — `android.util.Log.e("LoadingDialog", "Erreur lors du chargement de l'animation Lottie", e)`


