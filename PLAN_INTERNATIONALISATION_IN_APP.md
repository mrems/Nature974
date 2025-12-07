# Plan d'Internationalisation de l'Application (Phase 2)

### Règles de traduction strictes (À respecter absolument)
*   **Toujours échapper les apostrophes (') avec `\'` dans les chaînes de caractères XML pour les ressources de type `string`.**
*   **Seuls les attributs `android:text` doivent être traduits.**
*   **NE JAMAIS traduire les `android:contentDescription`.**
*   **NE JAMAIS traduire les messages de log, les messages Toast ou les commentaires dans le code.**
*   Les fichiers `strings.xml` ne doivent contenir QUE les textes affichés par `android:text`.

### Vue d'overview

Cette phase vise à internationaliser toutes les chaînes de caractères codées en dur dans les fichiers de layout XML de l'application, en dehors de la section d'onboarding déjà traitée. Pour chaque chaîne identifiée, une ressource `@string` sera créée dans `app/src/main/res/values-fr/strings.xml` (français) et ensuite traduite dans `app/src/main/res/values/strings.xml` (anglais par défaut). Les fichiers XML de layout seront ensuite mis à jour pour utiliser ces références.

### Fichiers de ressources concernés

*   `app/src/main/res/values-fr/strings.xml` (français)
*   `app/src/main/res/values/strings.xml` (anglais par défaut)

### Étapes d'implémentation

#### 1. Mettre à jour `app/src/main/res/values-fr/strings.xml` (Français)

Ajoutez les chaînes de caractères suivantes au fichier `app/src/main/res/values-fr/strings.xml` :

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ... Onboarding strings (from Phase 1) ... -->

    <!-- Activity Purchase -->
    <string name="purchase_unlock_adventure_title">Débloquez Votre Aventure !</string>
    <string name="purchase_choose_plan_description">Choisissez votre plan et commencez à explorer la nature.</string>
    <string name="purchase_50_credits">50 crédits</string>
    <string name="purchase_100_credits">100 crédits</string>
    <string name="purchase_300_credits">300 crédits</string>
    <string name="purchase_per_month">/mois</string>
    <string name="purchase_continue_button">Continuer</string>
    <string name="purchase_payment_info">Annulez à tout moment depuis votre compte Google Play</string>

    <!-- Activity History -->
    <string name="history_title">Historique des Analyses</string>

    <!-- Dialog History Options -->
    <string name="history_option_reanalyze">Ré-analyser</string>
    <string name="history_option_delete">Supprimer</string>

    <!-- Dialog Loading -->
    <string name="loading_analysis_in_progress">Analyse en cours...</string>
    <string name="loading_identifying_photo">Identification de votre photo</string>

    <!-- Item Analysis History -->
    <string name="history_item_example_badge">EXEMPLE</string>

    <!-- Dialog Model Selection -->
    <string name="model_selection_title">Sélectionnez le niveau de précision souhaité pour l\'analyse :</string>
    <string name="model_selection_quick_option">Rapide (1 crédit)</string>
    <string name="model_selection_balanced_option">Équilibré (2 crédits)</string>
    <string name="model_selection_ultra_precise_option">Ultra-précis (5 crédits)</string>
    <string name="model_selection_analyze_button">Analyser</string>

    <!-- Fragment Confidence and Alternatives -->
    <string name="confidence_score_label">Indice de certitude</string>
    <string name="confidence_description">Niveau de confiance de l\'IA dans cette identification</string>
    <string name="alternatives_label">Identifications alternatives</string>
    <string name="alternatives_subtitle">L\'IA a identifié d\'autres possibilités</string>

    <!-- Fragment Last Analysis -->
    <string name="last_analysis_example_badge">EXEMPLE</string>

    <!-- Fragment Camera -->
    <string name="camera_hint_click_menu_buy_credits">Click here to open the Menu or to Buy Credits</string>
    <string name="camera_hint_slide_left">←  Swipe</string>
    <string name="camera_hint_slide_right">Swipe  →</string>
    <string name="camera_hint_slide_bottom">↓ Swipe ↓</string>

    <!-- Fragment Title Menu -->
    <string name="menu_preferences">Préférences</string>
    <string name="menu_subscription">Abonnement</string>
    <string name="menu_manual">Mode d\'emploi</string>
    <string name="menu_feedback">Feedback</string>

    <!-- Fragment History List -->
    <string name="history_list_title">Historique des analyses</string>
    <string name="history_list_empty_state_title">Pas encore d\'analyses</string>
    <string name="history_list_empty_state_description">Capturez votre première plante ou\nanimal pour commencer votre collection !</string>

    <!-- Item Info Card -->
    <!-- Titles et contenu pour ces cartes sont dynamiques et seront gérés dans les fichiers Kotlin si nécessaire -->

</resources>
```

#### 2. Mettre à jour `app/src/main/res/values/strings.xml` (Anglais par défaut)

Ajoutez les chaînes de caractères suivantes au fichier `app/src/main/res/values/strings.xml` :

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- ... Onboarding strings (from Phase 1) ... -->

    <!-- Activity Purchase -->
    <string name="purchase_unlock_adventure_title">Unlock Your Adventure!</string>
    <string name="purchase_choose_plan_description">Choose your plan and start exploring nature.</string>
    <string name="purchase_50_credits">50 credits</string>
    <string name="purchase_100_credits">100 credits</string>
    <string name="purchase_300_credits">300 credits</string>
    <string name="purchase_per_month">/month</string>
    <string name="purchase_continue_button">Continue</string>
    <string name="purchase_payment_info">Cancel anytime from your Google Play account</string>

    <!-- Activity History -->
    <string name="history_title">Analysis History</string>

    <!-- Dialog History Options -->
    <string name="history_option_reanalyze">Re-analyze</string>
    <string name="history_option_delete">Delete</string>

    <!-- Dialog Loading -->
    <string name="loading_analysis_in_progress">Analysis in progress...</string>
    <string name="loading_identifying_photo">Identifying your photo</string>

    <!-- Item Analysis History -->
    <string name="history_item_example_badge">EXAMPLE</string>

    <!-- Dialog Model Selection -->
    <string name="model_selection_title">Select the desired precision level for analysis:</string>
    <string name="model_selection_quick_option">Quick (1 credit)</string>
    <string name="model_selection_balanced_option">Balanced (2 credits)</string>
    <string name="model_selection_ultra_precise_option">Ultra-precise (5 credits)</string>
    <string name="model_selection_analyze_button">Analyze</string>

    <!-- Fragment Confidence and Alternatives -->
    <string name="confidence_score_label">Confidence Score</string>
    <string name="confidence_description">AI\'s confidence level in this identification</string>
    <string name="alternatives_label">Alternative Identifications</string>
    <string name="alternatives_subtitle">AI has identified other possibilities</string>

    <!-- Fragment Last Analysis -->
    <string name="last_analysis_example_badge">EXAMPLE</string>

    <!-- Fragment Camera -->
    <string name="camera_hint_click_menu_buy_credits">Click here to open the Menu or to Buy Credits</string>
    <string name="camera_hint_slide_left">←  Swipe</string>
    <string name="camera_hint_slide_right">Swipe  →</string>
    <string name="camera_hint_slide_bottom">↓ Swipe ↓</string>

    <!-- Fragment Title Menu -->
    <string name="menu_preferences">Preferences</string>
    <string name="menu_subscription">Subscription</string>
    <string name="menu_manual">User Manual</string>
    <string name="menu_feedback">Feedback</string>

    <!-- Fragment History List -->
    <string name="history_list_title">Analysis History</string>
    <string name="history_list_empty_state_title">No analyses yet</string>
    <string name="history_list_empty_state_description">Capture your first plant or\nanimal to start your collection!</string>

    <!-- Item Info Card -->
    <!-- Titles and content for these cards are dynamic and will be handled in Kotlin files if needed -->

</resources>
```

#### 3. Mettre à jour les fichiers de layout XML

Pour chaque fichier XML de layout listé ci-dessous, remplacez les textes en dur par leurs références `@string` correspondantes.

*   `app/src/main/res/layout/activity_purchase.xml`
*   `app/src/main/res/layout/activity_history.xml`
*   `app/src/main/res/layout/dialog_history_options.xml`
*   `app/src/main/res/layout/dialog_loading.xml`
*   `app/src/main/res/layout/item_analysis_history.xml`
*   `app/src/main/res/layout/dialog_model_selection.xml`
*   `app/src/main/res/layout/fragment_confidence_and_alternatives.xml`
*   `app/src/main/res/layout/fragment_last_analysis.xml`
*   `app/src/main/res/layout/fragment_camera.xml`
*   `app/src/main/res/layout/fragment_title_menu.xml`
*   `app/src/main/res/layout/fragment_history_list.xml`
*   `app/src/main/res/layout/item_info_card.xml`

**Exemples de modifications pour certains fichiers (vous devrez appliquer le même principe à tous les fichiers concernés) :**

**`app/src/main/res/layout/activity_purchase.xml`**

```xml
// ... existing code ...
                android:text="@string/purchase_unlock_adventure_title"
// ... existing code ...
                android:text="@string/purchase_choose_plan_description"
// ... existing code ...
                                android:text="@string/purchase_50_credits"
// ... existing code ...
                            android:text="@string/purchase_per_month"
// ... existing code ...
                                android:text="@string/purchase_100_credits"
// ... existing code ...
                            android:text="@string/purchase_per_month"
// ... existing code ...
                                android:text="@string/purchase_300_credits"
// ... existing code ...
                            android:text="@string/purchase_per_month"
// ... existing code ...
                android:text="@string/purchase_continue_button"
// ... existing code ...
                android:text="@string/purchase_payment_info"
// ... existing code ...
```

**`app/src/main/res/layout/activity_history.xml`**

```xml
// ... existing code ...
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/history_title"
// ... existing code ...
```

**`app/src/main/res/layout/dialog_history_options.xml`**

```xml
// ... existing code ...
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/history_option_reanalyze"
// ... existing code ...
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/history_option_delete"
// ... existing code ...
```

**`app/src/main/res/layout/dialog_loading.xml`**

```xml
// ... existing code ...
        <TextView
            android:id="@+id/loading_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/loading_analysis_in_progress"
// ... existing code ...
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/loading_identifying_photo"
// ... existing code ...
```

**`app/src/main/res/layout/item_analysis_history.xml`**

```xml
// ... existing code ...
                    android:text="@string/history_item_example_badge"
// ... existing code ...
```

**`app/src/main/res/layout/dialog_model_selection.xml`**

```xml
// ... existing code ...
    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/model_selection_title"
// ... existing code ...
        <RadioButton
            android:id="@+id/radio_flash_lite"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/model_selection_quick_option"
// ... existing code ...
        <RadioButton
            android:id="@+id/radio_flash"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/model_selection_balanced_option"
// ... existing code ...
        <RadioButton
            android:id="@+id/radio_gemini_3_pro"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="@string/model_selection_ultra_precise_option"
// ... existing code ...
    <Button
        android:id="@+id/btn_analyze"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/model_selection_analyze_button"
// ... existing code ...
```

**`app/src/main/res/layout/fragment_confidence_and_alternatives.xml`**

```xml
// ... existing code ...
                        android:text="@string/confidence_score_label"
// ... existing code ...
                    android:text="@string/confidence_description"
// ... existing code ...
                android:text="@string/alternatives_label"
// ... existing code ...
                android:text="@string/alternatives_subtitle"
// ... existing code ...
```

**`app/src/main/res/layout/fragment_last_analysis.xml`**

```xml
// ... existing code ...
            android:text="@string/last_analysis_example_badge"
// ... existing code ...
```

**`app/src/main/res/layout/fragment_camera.xml`**

```xml
// ... existing code ...
            android:text="@string/camera_hint_click_menu_buy_credits"
// ... existing code ...
                android:text="@string/camera_hint_slide_left"
// ... existing code ...
                android:text="@string/camera_hint_slide_right"
// ... existing code ...
                android:text="@string/camera_hint_slide_bottom"
// ... existing code ...
```

**`app/src/main/res/layout/fragment_title_menu.xml`**

```xml
// ... existing code ...
        android:id="@+id/menu_preferences"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/menu_preferences"
// ... existing code ...
        android:id="@+id/menu_subscription"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/menu_subscription"
// ... existing code ...
        android:id="@+id/menu_manual"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/menu_manual"
// ... existing code ...
        android:id="@+id/menu_feedback"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/menu_feedback"
// ... existing code ...
```

**`app/src/main/res/layout/fragment_history_list.xml`**

```xml
// ... existing code ...
        android:id="@+id/title_history"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/history_list_title"
// ... existing code ...
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/history_list_empty_state_title"
// ... existing code ...
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/history_list_empty_state_description"
// ... existing code ...
```

**`app/src/main/res/layout/item_info_card.xml`**

Il n'y a pas de texte en dur à traduire directement dans `item_info_card.xml`, car les titres et le contenu sont définis dynamiquement. Ces chaînes seront gérées dans les fichiers Kotlin si nécessaire.
