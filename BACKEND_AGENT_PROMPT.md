# Instructions pour l'Agent Cursor : Adaptation du Backend EC2

## Objectif Principal
Adapter l'endpoint d'analyse d'image du backend EC2 (`https://super-abu.com/api/nature-pei/analyze-image`) pour qu'il construise lui-même le prompt pour l'API Gemini, gère la réponse structurée et la renvoie à l'application mobile.

## Contexte Actuel
L'application Android a été mise à jour. Elle envoie désormais uniquement l'image et son type (`mimeType`). Le backend doit maintenant construire le prompt complet pour l'API Gemini, en demandant une réponse au format JSON strict avec 6 champs spécifiques.

## Détails du Fonctionnement Actuel de l'Endpoint Backend (avant modifications)
*   **Requête entrante (depuis l'App Android) :**
    *   Méthode : `POST`
    *   Endpoint : `/api/nature-pei/analyze-image`
    *   Body : JSON avec `{"image": "base64_image_data", "mimeType": "image/jpeg", "prompt": "le_prompt_pour_gemini"}`
*   **Interaction avec l'API Gemini :**
    *   Le backend extrait le `prompt` de la requête de l'application et l'envoie à l'API Gemini.
    *   **Le backend s'attend à une réponse TEXTE de Gemini et la parse de manière non structurée.**
*   **Réponse sortante (vers l'App Android) :**
    *   Le backend renvoie un JSON avec seulement 3 champs : `{"localName": "...", "scientificName": "...", "description": "..."}`.

## Nouvelle Logique Requise pour l'Endpoint Backend

Votre agent Cursor doit modifier le code du backend EC2 pour implémenter la logique suivante :

1.  **Réception de la requête de l'Application Android :**
    *   **Le backend recevra désormais une requête POST avec uniquement l'image et le `mimeType`. Le champ `prompt` ne sera plus envoyé par l'application.**
    *   Exemple de Body de requête entrante : `{"image": "base64_image_data", "mimeType": "image/jpeg"}`

2.  **Construction du Prompt pour l'API Gemini (par le backend) :**
    *   Le backend doit construire lui-même le prompt complet (`systemPrompt` et `userQuery`) qui sera envoyé à l'API Gemini.
    *   **Le prompt doit être le suivant :**
        ```text
        Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de l'Île de la Réunion. Identifiez l'espèce et fournissez des informations structurées.

        Analysez cette image et fournissez une réponse JSON stricte avec les 6 champs suivants:
        1.  "localName": Nom commun en français (ex: "Tamarin des Hauts") ou "N/C" si inconnu.
        2.  "scientificName": Nom scientifique latin (ex: "Acacia heterophylla") ou "N/C" si inconnu.
        3.  "type": Type d'espèce et son statut combinés (ex: "Plante endémique", "Oiseau introduit", "Animal non endémique", "N/C") ou "N/C" si inconnu.
        4.  "habitat": Habitat principal à La Réunion (ex: "Forêts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
        5.  "characteristics": Description physique COURTE et synthétique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
        6.  "reunionContext": Contexte réunionnais COURT et synthétique (usages, écologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.

        Si l'espèce ne peut pas être identifiée ou si un champ est inconnu, utilisez "N/C".
        Répondez UNIQUEMENT avec le JSON, sans texte supplémentaire.
        ```
    *   Le backend combinera ce prompt avec l'image reçue et enverra le tout à l'API Gemini.

3.  **Parsing de la Réponse de l'API Gemini (par le backend) :**
    *   Le backend doit s'attendre et parser une réponse STRICTEMENT au format JSON de la part de l'API Gemini, comme décrit dans l'exemple ci-dessous.
    *   **Structure JSON Attendue de l'API Gemini :**
        ```json
        {
          "localName": "Tamarin des Hauts",
          "scientificName": "Acacia heterophylla",
          "type": "Plante endémique",
          "habitat": "Forêts humides >1200m",
          "characteristics": "Grand arbre des forêts humides de La Réunion, feuilles persistantes, fleurs jaunes en grappes.",
          "reunionContext": "Espèce endémique essentielle aux écosystèmes montagnards. Bois apprécié des artisans."
        }
        ```
    *   **Gestion des valeurs "N/C" :** Le backend doit gérer les valeurs `"N/C"` renvoyées par Gemini sans générer d'erreurs.

4.  **Construction et Envoi de la Réponse à l'Application Android :**
    *   Le backend doit construire une réponse JSON contenant **exactement les 6 champs** et leurs valeurs obtenues de la réponse de Gemini.
    *   Cette réponse structurée doit être renvoyée à l'application Android.

## Points d'Attention pour l'Agent Backend

*   **Modification de l'entrée de l'endpoint :** L'endpoint `/api/nature-pei/analyze-image` ne doit plus s'attendre au champ `prompt` dans la requête entrante.
*   **Construction du prompt :** Intégrer la logique de construction du prompt directement dans le code du backend avant d'appeler Gemini.
*   **Robustesse du parsing JSON :** Utiliser un parseur JSON natif et gérer les exceptions lors de la lecture de la réponse de Gemini.
*   **Concision des champs :** Puisque le backend construit le prompt, il a le contrôle total sur la demande de concision pour `characteristics` et `reunionContext`.
*   **Compatibilité :** Assurer la compatibilité avec le reste de l'infrastructure si d'autres endpoints existent.
*   **Dépendances :** S'assurer que les bibliothèques de parsing JSON nécessaires sont installées et correctement configurées dans l'environnement EC2.

---

Ce fichier `BACKEND_AGENT_PROMPT.md` sert de guide clair pour votre agent Cursor qui travaillera sur le backend.
rel