# Plan de Gestion des Images : Enregistrement Unique de la Photo Recadrée de Haute Qualité dans la Galerie et l'App

## Objectif
Implémenter un flux de gestion des images où seule la version recadrée de haute qualité d'une photo est enregistrée dans la galerie publique de l'utilisateur, tout en conservant une copie indépendante et de haute qualité dans le stockage privé de l'application pour l'affichage, et en n'envoyant qu'une version optimisée (600x600, 40% de compression) à l'API.

## Fichiers affectés
*   `app/src/main/java/com/pastaga/geronimo/CameraFragment.kt`
*   `app/src/main/java/com/pastaga/geronimo/ImageAnalyzer.kt` (vérification, pas de changement direct lié à ce plan)

## Étapes détaillées

### 1. Préparation (Déjà fait)
*   S'assurer que `maxLongEdge` dans `chooseOptimalCaptureSize` de `CameraFragment.kt` est réglé à `Int.MAX_VALUE` pour capturer l'image à la résolution maximale. (Ceci a été fait lors de la dernière interaction).

### 2. Modification de `onImageAvailableListener` dans `CameraFragment.kt`
*   Modifier la logique de sauvegarde initiale de l'image capturée. Au lieu de l'enregistrer dans `getExternalFilesDir(Environment.DIRECTORY_PICTURES)` de manière permanente :
    *   L'image brute sera sauvegardée dans un fichier temporaire accessible par UCrop. Ce fichier sera supprimé une fois le recadrage terminé (qu'il soit réussi ou annulé).

### 3. Modification de `uCropActivityResultLauncher` dans `CameraFragment.kt`
*   Après un recadrage réussi (`result.resultCode == Activity.RESULT_OK`):
    *   Obtenir l'URI de l'image recadrée haute qualité (`resultUri = UCrop.getOutput(data)`).
    *   **Enregistrer cette `resultUri` dans la galerie publique via `MediaStore`**:
        *   Lire les octets du fichier temporaire `resultUri`.
        *   Utiliser `ContentValues` et `resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)` pour créer une nouvelle entrée dans la galerie.
        *   Écrire les octets de l'image recadrée dans l'`OutputStream` obtenu de `MediaStore`.
        *   Mettre à jour le statut `IS_PENDING` pour rendre l'image visible dans la galerie (pour Android Q+).
    *   **Copier cette même image recadrée de haute qualité vers le stockage privé de l'application**:
        *   Lire les octets du fichier temporaire `resultUri` (ou directement du `OutputStream` de l'étape précédente).
        *   Sauvegarder ces octets dans un nouveau fichier (`File`) dans le répertoire privé de l'application (ex: `getExternalFilesDir(Environment.DIRECTORY_PICTURES)`).
        *   L'URI de cette copie privée sera utilisée pour `croppedImageUri` et transmise à `showModelSelectionDialog(privateCopyUri)`.
    *   **Nettoyage**: Supprimer le fichier temporaire `resultUri` créé par UCrop.
*   En cas d'annulation ou d'erreur de recadrage (`Activity.RESULT_CANCELED` ou `UCrop.RESULT_ERROR`):
    *   S'assurer que tous les fichiers temporaires créés par UCrop sont supprimés.

### 4. Vérification de `ImageAnalyzer.kt`
*   Confirmer que la fonction `analyzeImage` reçoit l'URI de la **copie privée de l'image recadrée** (celle stockée dans le dossier privé de l'application).
*   S'assurer que le redimensionnement à 600x600 et la compression à 40% se produisent toujours à cet endroit, juste avant l'envoi à l'API. (Ceci est déjà le cas et ne devrait pas changer).

## Permissions nécessaires (vérification)
*   Si l'application cible Android 10 (API 29) ou supérieur, les permissions `WRITE_EXTERNAL_STORAGE` ne sont pas nécessaires pour écrire des fichiers média dans des collections spécifiques comme `Pictures` via `MediaStore`.
*   Si l'application cible Android 9 (API 28) ou inférieur, la permission `WRITE_EXTERNAL_STORAGE` serait nécessaire pour écrire dans la galerie publique. (Votre `minSdk` est 24, donc `MediaStore` gérera la compatibilité, mais il est bon de le noter).

## Avantages de cette approche
*   **Simplicité pour l'utilisateur**: Une seule image pertinente (recadrée de haute qualité) apparaît dans la galerie du téléphone.
*   **Robustesse de l'application**: L'historique de l'application est indépendant de la galerie publique. Si l'utilisateur supprime l'image de sa galerie, elle reste accessible dans l'application.
*   **Qualité d'affichage**: L'affichage dans l'application utilise des images recadrées de haute qualité.
*   **Performance API**: L'API reçoit des images optimisées pour la performance (600x600, 40% de compression).
