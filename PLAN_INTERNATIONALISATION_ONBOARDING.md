# Plan d'Internationalisation de l'Onboarding (Français et Anglais)

### Règles de traduction strictes (À respecter absolument)
*   **Toujours échapper les apostrophes (') avec `\'` dans les chaînes de caractères XML pour les ressources de type `string`.**
*   **Seuls les attributs `android:text` doivent être traduits.**
*   **NE JAMAIS traduire les `android:contentDescription`.**
*   **NE JAMAIS traduire les messages de log, les messages Toast ou les commentaires dans le code.**
*   Les fichiers `strings.xml` ne doivent contenir QUE les textes affichés par `android:text`.

### Vue d'overview

Ce plan vise à rendre la section d'onboarding de l'application multilingue. L'anglais sera la langue par défaut de l'application, et le français sera disponible si la langue du téléphone est configurée en français. Ce plan inclut l'extraction de toutes les chaînes de caractères de l'onboarding, leur organisation dans les fichiers de ressources Android appropriés, et la mise à jour des layouts et du code Kotlin pour utiliser ces ressources localisées.

### Phase 1: Internationalisation de l'Onboarding

#### Fichiers concernés par l'onboarding

*   `app/src/main/res/values/strings.xml` (sera le fichier par défaut en anglais)
*   `app/src/main/res/values-fr/strings.xml` (sera le fichier pour le français)
*   `app/src/main/java/com/pastaga/geronimo/IntroPage1Fragment.kt`
*   `app/src/main/res/layout/fragment_intro_page1.xml`
*   `app/src/main/java/com/pastaga/geronimo/IntroPage2Fragment.kt`
*   `app/src/main/res/layout/fragment_intro_page2.xml`
*   `app/src/main/java/com/pastaga/geronimo/IntroPage3Fragment.kt`
*   `app/src/main/res/layout/fragment_intro_page3.xml`
*   `app/src/main/java/com/pastaga/geronimo/IntroPage4Fragment.kt`
*   `app/src/main/res/layout/fragment_intro_page4.xml`
*   `app/src/main/java/com/pastaga/geronimo/IntroPage5Fragment.kt`
*   `app/src/main/res/layout/fragment_intro_page5.xml`
*   `app/src/main/java/com/pastaga/geronimo/GoogleSignInActivity.kt`
*   `app/src/main/java/com/pastaga/geronimo/OnboardingFragment.kt`

### Étapes d'implémentation

#### 1. Préparer le fichier `strings.xml` pour le français

Le fichier `app/src/main/res/values/strings.xml` est actuellement vide. Nous allons ajouter toutes les nouvelles chaînes d'onboarding au fichier `app/src/main/res/values/strings.xml`, qui sera ensuite déplacé dans `values-fr`.

*   **Action :** Ajoutez les chaînes d'onboarding suivantes au fichier `app/src/main/res/values/strings.xml`.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Intro Page 1 -->
    <string name="intro_page1_welcome_title">Bienvenue sur</string>
    <string name="intro_page1_subtitle">Avec Geki vous allez pouvoir identifier n\'importe quoi sur votre chemin :</string>
    <string name="intro_page1_list_items">• Un oiseau qui passe par la,\n• Une nouvelle plante mystérieuse,\n• Un insecte posé sur votre terrasse,\n• Une fleur qu\'on vous a offerte,\n• Un champignon dans la forêt,\n• Un fruit sur un marché exotique,\n• Une empreinte animale par terre,\n• Un coquillage ramassé sur la plage,\n• Une pierre sur un collier,\n• etc…</string>
    <string name="intro_page1_bold_text">littéralement n\'importe quoi !</string>
    <string name="intro_page1_button_start">Commencer</string>

    <!-- Intro Page 2 -->
    <string name="intro_page2_title">Rapide et intuitif</string>
    <string name="intro_page2_description">Il suffit de prendre une photo pour avoir une réponse en quelques secondes</string>
    <string name="intro_page2_button_next">Suivant</string>

    <!-- Intro Page 3 -->
    <string name="intro_page3_title">Nouvelle génération de modèles IA</string>
    <string name="intro_page3_subtitle">Nous vous offrons les meilleurs modèles au monde. Sélectionnez parmi nos trois niveaux d\'identification, chacun offrant une profondeur d\'analyse croissante.</string>
    <string name="intro_page3_quick_title">Rapide</string>
    <string name="intro_page3_quick_description">Identification rapide et efficace.</string>
    <string name="intro_page3_standard_title">Standard</string>
    <string name="intro_page3_standard_description">Équilibre entre précision et rapidité.</string>
    <string name="intro_page3_precise_title">Extrêmement précis</string>
    <string name="intro_page3_precise_description">Analyse approfondie pour les cas difficiles.</string>
    <string name="intro_page3_button_next">Suivant</string>

    <!-- Intro Page 4 -->
    <string name="intro_page4_title">Partout avec vous</string>
    <string name="intro_page4_description">Que ce soit en balade ou en voyage, vous allez bientôt connaître et reconnaître toute la nature autour de vous</string>
    <string name="intro_page4_button_next">Suivant</string>

    <!-- Intro Page 5 -->
    <string name="intro_page5_title">Dernière étape</string>
    <string name="intro_page5_description">Pour utiliser l\'application, vous devez maintenant vous connecter avec Google et accepter les permissions necessaires.</string>
    <string name="intro_page5_button_login">Se connecter</string>

    <!-- Onboarding Fragment (Permissions) -->
    <string name="permission_camera_title">Permission Caméra</string>
    <string name="permission_camera_description">Cette application a besoin d'accéder à votre caméra pour capturer et analyser des photos de plantes et d'animaux.</string>
    <string name="permission_location_title">Permission Localisation</string>
    <string name="permission_location_description">L'accès à votre localisation permet d'améliorer la précision de l'identification des espèces en fonction de votre région.</string>
    <string name="permission_gallery_title">Permission Galerie</string>
    <string name="permission_gallery_description">L'accès à vos photos vous permet d'analyser des images existantes depuis votre galerie.</string>
    <string name="permission_button_accept">Accepter</string>
</resources>
```

#### 2. Déplacer le fichier `strings.xml` français

*   **Action :** Créez le dossier `app/src/main/res/values-fr/`.
*   **Action :** Déplacez le fichier `app/src/main/res/values/strings.xml` (contenant le français) vers `app/src/main/res/values-fr/strings.xml`.

#### 3. Créer le fichier `strings.xml` pour l'anglais (par défaut)

*   **Action :** Créez un nouveau fichier `app/src/main/res/values/strings.xml`.
*   **Action :** Ajoutez le contenu suivant (traductions anglaises) à ce nouveau fichier. L'anglais est la langue par défaut.

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Intro Page 1 -->
    <string name="intro_page1_welcome_title">Welcome to</string>
    <string name="intro_page1_subtitle">With Geki you will be able to identify anything on your path:</string>
    <string name="intro_page1_list_items">• A bird flying by,\n• A mysterious new plant,\n• An insect on your terrace,\n• A flower you were given,\n• A mushroom in the forest,\n• A fruit at an exotic market,\n• An animal footprint on the ground,\n• A seashell picked up on the beach,\n• A stone on a necklace,\n• etc…</string>
    <string name="intro_page1_bold_text">literally anything!</string>
    <string name="intro_page1_button_start">Get Started</string>

    <!-- Intro Page 2 -->
    <string name="intro_page2_title">Fast and Intuitive</string>
    <string name="intro_page2_description">Just take a photo to get an answer in seconds</string>
    <string name="intro_page2_button_next">Next</string>

    <!-- Intro Page 3 -->
    <string name="intro_page3_title">New Generation of AI Models</string>
    <string name="intro_page3_subtitle">We offer you the best models in the world. Select from our three levels of identification, each offering increasing analytical depth.</string>
    <string name="intro_page3_quick_title">Quick</string>
    <string name="intro_page3_quick_description">Fast and efficient identification.</string>
    <string name="intro_page3_standard_title">Standard</string>
    <string name="intro_page3_standard_description">Balance between precision and speed.</string>
    <string name="intro_page3_precise_title">Extremely Precise</string>
    <string name="intro_page3_precise_description">In-depth analysis for difficult cases.</string>
    <string name="intro_page3_button_next">Next</string>

    <!-- Intro Page 4 -->
    <string name="intro_page4_title">Everywhere with you</string>
    <string name="intro_page4_description">Whether on a stroll or traveling, you will soon know and recognize all the nature around you</string>
    <string name="intro_page4_button_next">Next</string>

    <!-- Intro Page 5 -->
    <string name="intro_page5_title">Last Step</string>
    <string name="intro_page5_description">To use the application, you must now connect with Google and accept the necessary permissions.</string>
    <string name="intro_page5_button_login">Log In</string>

    <!-- Onboarding Fragment (Permissions) -->
    <string name="permission_camera_title">Camera Permission</string>
    <string name="permission_camera_description">This application needs access to your camera to capture and analyze photos of plants and animals.</string>
    <string name="permission_location_title">Location Permission</string>
    <string name="permission_location_description">Access to your location improves the accuracy of species identification based on your region.</string>
    <string name="permission_gallery_title">Gallery Permission</string>
    <string name="permission_gallery_description">Access to your photos allows you to analyze existing images from your gallery.</string>
    <string name="permission_button_accept">Accept</string>
</resources>
```

#### 4. Mettre à jour les fichiers de layout XML

Pour chaque fichier XML de layout listé dans la section "Fichiers concernés par l'onboarding", remplacez les textes en dur par leurs références `@string` correspondantes.

*   `app/src/main/res/layout/fragment_intro_page1.xml`
*   `app/src/main/res/layout/fragment_intro_page2.xml`
*   `app/src/main/res/layout/fragment_intro_page3.xml`
*   `app/src/main/res/layout/fragment_intro_page4.xml`
*   `app/src/main/res/layout/fragment_intro_page5.xml`
*   `app/src/main/res/layout/fragment_onboarding.xml` (pour les permissions)

**Exemple de modification pour `app/src/main/res/layout/fragment_intro_page1.xml` :**

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#F5F5F0"
    android:fillViewport="true">

    <LinearLayout
        android:id="@+id/content_container_with_button"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:gravity="center_horizontal|center_vertical"
        android:padding="24dp">

        <!-- Titre Welcome to -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/intro_page1_welcome_title"
            android:textSize="24sp"
            android:textStyle="bold"
            android:fontFamily="@font/montserrat_bold"
            android:gravity="center_horizontal"
            android:layout_marginBottom="-16dp" />

        <!-- Titre Geronimo! remplacé par l'image -->
        <ImageView
            android:layout_width="wrap_content"
            android:layout_height="85dp"
            android:src="@drawable/geki_title"
            android:contentDescription="Geronimo Logo"
            android:adjustViewBounds="true"
            android:layout_marginBottom="24dp" />

        <!-- Sous-titre -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/intro_page1_subtitle"
            android:textSize="18sp"
            android:gravity="start"
            android:lineSpacingExtra="4dp"
            android:layout_marginBottom="24dp"
            android:paddingHorizontal="16dp" />

        <!-- Liste d'éléments -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/intro_page1_list_items"
            android:textSize="17sp"
            android:lineSpacingExtra="6dp"
            android:layout_marginBottom="24dp" />

        <!-- Phrase en gras -->
        <TextView
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/intro_page1_bold_text"
            android:textSize="20sp"
            android:textStyle="bold"
            android:gravity="start"
            android:paddingHorizontal="16dp" />

        <!-- Bouton Get Started -->
        <androidx.appcompat.widget.AppCompatButton
            android:id="@+id/page1_button"
            android:layout_width="match_parent"
            android:layout_height="56dp"
            android:text="@string/intro_page1_button_start"
            android:textColor="#FFFFFF"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textAllCaps="false"
            android:background="@drawable/button_primary_rounded"
            android:layout_marginTop="32dp" />

    </LinearLayout>

</ScrollView>
```
*(Vous devrez répéter ce processus pour tous les layouts mentionnés ci-dessus en vous basant sur les extractions et traductions fournies précédemment.)*

#### 5. Mettre à jour les fichiers Kotlin

Pour chaque fichier Kotlin listé dans la section "Fichiers concernés par l'onboarding", remplacez les chaînes de caractères en dur par leurs références `getString(R.string.)` correspondantes. Si une chaîne ne correspond pas à un `android:text`, elle ne doit pas être traduite et doit rester en dur si nécessaire.

*   `app/src/main/java/com/pastaga/geronimo/GoogleSignInActivity.kt`
*   `app/src/main/java/com/pastaga/geronimo/OnboardingFragment.kt`

**Exemple de modification pour `app/src/main/java/com/pastaga/geronimo/OnboardingFragment.kt` (pour les permissions, si des titres ou descriptions sont définis dynamiquement) :**

```kotlin
// Exemple hypothétique dans OnboardingFragment.kt
// ... existing code ...
        when (position) {
            0 -> {
                binding.onboardingTitle.text = getString(R.string.permission_camera_title)
                binding.onboardingDescription.text = getString(R.string.permission_camera_description)
                binding.onboardingIcon.setImageResource(R.drawable.camera)
            }
            1 -> {
                binding.onboardingTitle.text = getString(R.string.permission_location_title)
                binding.onboardingDescription.text = getString(R.string.permission_location_description)
                binding.onboardingIcon.setImageResource(R.drawable.location)
            }
            // ... autres pages de permission ...
        }
        binding.requestPermissionButton.text = getString(R.string.permission_button_accept)
// ... existing code ...
```

### Phase 2: Internationalisation du contenu in-app (à définir ultérieurement)

Cette phase couvrira l'internationalisation de toutes les autres chaînes de caractères et éléments d'interface utilisateur à l'intérieur de l'application, au-delà de la section d'onboarding. Les détails de cette phase seront définis une fois la Phase 1 terminée.