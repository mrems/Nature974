/**
 * Cloud Function (2nd Gen) HTTPS avec Express pour reproduire la route
 * POST /analyze-image de l'API EC2, en proxy vers Gemini.
 */
const express = require("express");
const cors = require("cors");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret, defineString } = require("firebase-functions/params");
const functions = require("firebase-functions");
const functionsV1 = require("firebase-functions/v1");
const { GoogleGenerativeAI } = require("@google/generative-ai");
const { google } = require("googleapis");
const { Buffer } = require('buffer'); // <-- AJOUTER CETTE LIGNE

const admin = require('firebase-admin');
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
initializeApp();
const db = getFirestore();

// Charger les variables d'environnement depuis .env en développement local
if (process.env.NODE_ENV !== 'production') {
  require('dotenv').config();
}

// Région (Paris)
const REGION = "europe-west9"; 

// Middleware Express
const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: "15mb" }));

// Déclaration du secret Firebase pour la clé Gemini
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const ANDROID_PACKAGE_NAME = defineString("ANDROID_PACKAGE_NAME");

// Mapping modèle -> coût en crédits
const MODEL_CREDITS = {
  'gemini-2.5-flash-lite-preview-09-2025': 1,
  'gemini-2.5-flash': 2,
  'gemini-2.5-pro': 5,
};

// Utilitaire: transformer base64 en part compatible Gemini
function base64ToGenerativePart(base64EncodedImage, mimeType) {
  return {
    inlineData: {
      data: base64EncodedImage,
      mimeType,
    },
  };
}

// Contrôleur: logique identique à l'EC2
async function analyzeImage(req, res) {
  const startTime = Date.now();
  console.log(`[${new Date().toISOString()}] Début de la requête analyzeImage.`);
  try {
    console.log("En-têtes de la requête:", req.headers);
    // console.log('Corps de la requête:', req.body); // potentiellement volumineux

    console.log(
      `[${new Date().toISOString()}] Validation des champs. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    const { image, mimeType, prompt, country, region, modelId, uid, language } = req.body;

    // Log de la langue reçue
    console.log(`[${new Date().toISOString()}] Langue reçue du frontend: ${language ?? 'Non spécifiée'}`);

    if (!image || !mimeType) {
      console.warn("Validation échouée: champs image ou mimeType manquants.");
      return res
        .status(400)
        .json({ message: "Les champs 'image' et 'mimeType' sont requis." });
    }

    if (!uid) {
      console.warn("Validation échouée: champ uid manquant.");
      return res
        .status(400)
        .json({ message: "Le champ 'uid' est requis pour l'authentification." });
    }

    // Validation du modelId
    const validModelIds = Object.keys(MODEL_CREDITS);
    if (!modelId || !validModelIds.includes(modelId)) {
      console.warn("Validation échouée: modelId invalide ou manquant.");
      return res
        .status(400)
        .json({ message: `Le champ 'modelId' est requis et doit être l'un des suivants: ${validModelIds.join(', ')}. `});
    }

    // L'ancienne logique de déduction des crédits en début de requête a été supprimée
    // pour éviter le double débit. La décrémentation n'a lieu qu'en cas de succès
    // de l'analyse et d'envoi d'une réponse valide au client (voir lignes 241-242).
    // const creditsRequired = MODEL_CREDITS[modelId];
    // console.log(`[${new Date().toISOString()}] Crédits requis pour ${modelId}: ${creditsRequired}`);
    // try {
    //   await decrementCreditsWithAmount(uid, creditsRequired);
    // } catch (creditError) {
    //   console.error("Erreur lors de la déduction des crédits:", creditError);
    //   return res
    //     .status(400)
    //     .json({ message: creditError.message });
    // }

    // Conversion de l'image
    console.log(
      `[${new Date().toISOString()}] Début de la conversion de l'image Base64. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    const imageConversionStartTime = Date.now();
    let imagePart;
    try {
      imagePart = base64ToGenerativePart(image, mimeType);
      console.log(
        `[${new Date().toISOString()}] Fin de la conversion de l'image Base64: ${Date.now() - imageConversionStartTime} ms. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
      );
    } catch (conversionError) {
      console.error("Erreur lors de la conversion de l'image base64:", conversionError);
      return res
        .status(500)
        .json({ message: "Erreur lors du traitement de l'image.", error: conversionError.message });
    }

    const genAI = new GoogleGenerativeAI(GEMINI_API_KEY.value());
    // L'ancien log de débogage pour GEMINI_API_KEY a été supprimé car il n'apparaissait pas dans les logs.
    // console.log(`[DEBUG] GEMINI_API_KEY est chargée. Longueur: ${GEMINI_API_KEY.value()?.length || 0}. Début: ${GEMINI_API_KEY.value()?.substring(0, 5) || 'N/A'}`);

    const model = genAI.getGenerativeModel({ model: modelId });

    // Prompt Gemini
    console.log(
      `[${new Date().toISOString()}] Construction du prompt Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    let locationContext = "";
    if (country) {
      locationContext = `The user is located in ${country}.`;
      if (region) {
        locationContext += ` The region provided is: ${region}. Only use the region if you have factual, specific, and non-generic information relevant to this region (habitat, status, uses). If you are unsure or if the region resembles a city/neighborhood, ignore it and do not mention it.`;
      }
      locationContext += "\n\n"; // Ajouter une ligne vide pour une meilleure séparation
    }
    // Construire dynamiquement la spécialisation selon la localisation fournie
    let expertiseLine = "You are an expert in biodiversity (fauna and flora), stones, and minerals.";
    if (country) {
      expertiseLine = `You are an expert in biodiversity (fauna and flora), stones, and minerals, specialized in the species and geological formations of ${country}.`;
    }

    const geminiPrompt = `Respond in ${language ?? 'en'}. Please respond ONLY with the following JSON. The JSON FIELD NAMES must remain in English. All textual values MUST IMPERATIVELY be provided in the following language: ${language ?? 'en'}.

${locationContext}${expertiseLine}

If you are sent a photo of a human or a manufactured object and it is not the main and obvious subject of the image, ignore it. Focus on identifying and analyzing fauna, flora, fungi, stones, and minerals. If the human is the main subject, you must fill in all fields humorously and sarcastically, always with new content, expressing your imagination and humor.

Rule for region usage: The 'country' variable is always used. The 'region' variable is optional and MUST ONLY be used if you have factual, specific, and non-generic information relevant to this region (habitat, status, uses). If you are unsure or if the region resembles a city/neighborhood, ignore it and do not mention it.

Analyze this image and provide a strict JSON response with the following 12 fields:
1.  "localName": Common name in the requested language (e.g., "Blackbird" for English) or "N/C" if unknown.
2.  "scientificName": Latin scientific name (e.g., "Turdus merula") or "N/C" if unknown.
3.  "type": Species type and status (if relevant). Be concise. Use terms like "Endemic Plant", "Bird", "Tropical Fish", "Climbing Vine", "Feline", "Insect", "Carnivorous Plant", "Marine Shellfish", "Crustacean", "Ornamental Plant". Avoid redundancies like "cultivated" or "introduced" if the type is already clear. Use "N/C" if unknown.
4.  "habitat": Main habitat (e.g., "Humid forests >1200m", "Coastline", "Urban areas") or "N/C" if unknown.
5.  "characteristics": SHORT and synthetic physical description (size, color, shape, max 2-3 sentences) or "N/C" if unknown.
6.  "localContext": Local context based on country="${country ?? 'N/C'}".${region ? ` Only include region="${region}" IF you have concrete, specific, and non-generic information for that region; otherwise, do not mention the region.` : ''} (uses, ecology, cultural anecdote, max 2-3 sentences) or "N/C" if unknown.
7.  "Peculiarities": Peculiarities (medicinal virtues, lithotherapy properties, behavior, character, diet, reproduction). **EXCLUSIVELY** if a SERIOUS AND REAL DANGER exists: include its specific dangerousness/toxicity for humans (including children), domestic dogs, and cats (e.g., "kidney toxic plant", "venomous bite", "venomous animal, painful bite"). **IN ALL OTHER CASES (NO SERIOUS DANGER, OR IRRELEVANT INFORMATION ABOUT LACK OF DANGER), NEVER MENTION THE ABSENCE OF DANGER OR TOXICITY**. Be concise.
8.  "representativeColorHex": "A hexadecimal color code (e.g., \"#FF5733\") that best represents the identified species in the image. If the species cannot be identified, use \"#CCCCCC\". **THE VALUE MUST IMPERATIVELY BE ENCLOSED IN DOUBLE QUOTES.**"
9.  "danger": true if the analyzed species presents a MORTAL DANGER to humans (adults and children) or pets (dogs and cats) (e.g., toxic plants/mushrooms, dangerous predators, venomous animals, etc.). Otherwise, false.
10. "confidenceScore": An integer representing the AI's confidence percentage in its identification (0-100). Only use "N/C" if the primary identification is "N/C".
11. "alternativeIdentifications": A JSON array if the AI hesitates between multiple identifications. Each object must have:
    - "scientificName": Scientific Latin name of the alternative.
    - "localName": Common name of the alternative in the requested language, or "N/C" if unknown.
    - "difference": SHORT and precise description (max 2-3 sentences) of the characteristics distinguishing it from the primary identification (e.g., "Distinguished by larger leaf size and absence of stem hairs."). If no notable difference, use "N/C".
    If no alternatives are relevant, return an empty array [].
12. "justificationText": A concise explanation (around 20 words) of the confidence score and the absence of alternatives, ONLY if "alternativeIdentifications" is an empty array. Otherwise, "N/A".

If the species cannot be identified or if a field is unknown, use "N/C".
`;

    let result, response, text;
    console.log(
      `[${new Date().toISOString()}] Début de l'appel à l'API Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    const geminiCallStartTime = Date.now();
    try {
      // Récupérer le nombre de tokens du prompt (image + texte)
      const { totalTokens: promptTokens } = await model.countTokens([geminiPrompt, imagePart]);
      console.log(`[${new Date().toISOString()}] Jetons du prompt (entrée): ${promptTokens}`);

      result = await model.generateContent([geminiPrompt, imagePart]);
      response = await result.response;
      text = response.text();
      const usageMetadata = response.usageMetadata;
      if (usageMetadata) {
        console.log(`[${new Date().toISOString()}] Détail des jetons consommés:`);
        console.log(`[${new Date().toISOString()}]   - Prompt total (entrée): ${usageMetadata.promptTokenCount}`);
        if (usageMetadata.promptTokensDetails) {
          const textTokens = usageMetadata.promptTokensDetails.find(d => d.modality === 'TEXT')?.tokenCount || 0;
          const imageTokens = usageMetadata.promptTokensDetails.find(d => d.modality === 'IMAGE')?.tokenCount || 0;
          console.log(`[${new Date().toISOString()}]     - Texte du prompt: ${textTokens}`);
          console.log(`[${new Date().toISOString()}]     - Image du prompt: ${imageTokens}`);
        }
        console.log(`[${new Date().toISOString()}]   - Réponse JSON (sortie): ${usageMetadata.candidatesTokenCount}`);
        console.log(`[${new Date().toISOString()}]   - Jetons de pensée (internes): ${usageMetadata.thoughtsTokenCount || 0}`);
        console.log(`[${new Date().toISOString()}]   - TOTAL FACTURABLE: ${usageMetadata.totalTokenCount}`);
      }
      console.log(`[${new Date().toISOString()}] Réponse brute de Gemini (objet complet):`, JSON.stringify(response, null, 2));
      console.log(`[${new Date().toISOString()}] Texte extrait de Gemini (brut):`, text);
      console.log(
        `[${new Date().toISOString()}] Fin de l'appel à l'API Gemini: ${Date.now() - geminiCallStartTime} ms. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
      );
    } catch (geminiError) {
      console.error("Erreur lors de l'appel à l'API Gemini:", geminiError);
      return res
        .status(500)
        .json({ message: "Erreur lors de l'appel à l'IA pour l'analyse d'image.", error: geminiError.message });
    }

    console.log("Réponse brute de Gemini:", JSON.stringify(response, null, 2));
    console.log("Texte extrait de Gemini:", text);

    // Nettoyage blocs de code
    if (text.startsWith("```json") && text.endsWith("```")) {
      text = text.substring(7, text.length - 3).trim();
    }
    // L'ancien log de débogage `[DEBUG_CLEANED_TEXT]` a été supprimé.

    // Correction du formatage de representativeColorHex si Gemini l'oublie
    text = text.replace(/("representativeColorHex":\s*)(#([0-9A-Fa-f]{6}))/g, '$1"$2"');

    // Log du texte nettoyé avant parsing JSON
    // console.log(`[${new Date().toISOString()}] Texte Gemini nettoyé (avant parsing):`, text);

    // Parsing JSON
    console.log(
      `[${new Date().toISOString()}] Début du nettoyage et du parsing de la réponse Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    try {
      const parsedResult = JSON.parse(text);
      // L'ancien log de débogage `[DEBUG_PARSED_RESULT]` a été supprimé.
      console.log(
        `[${new Date().toISOString()}] Fin du parsing de la réponse Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
      );
      if (
        parsedResult.localName &&
        parsedResult.scientificName &&
        parsedResult.type &&
        parsedResult.habitat &&
        parsedResult.characteristics &&
        parsedResult.localContext &&
        parsedResult.Peculiarities && // CHANGEMENT ICI
        parsedResult.danger !== undefined && // Nouveau champ à valider: vérifier l'existence, pas la valeur booléenne
        parsedResult.confidenceScore !== undefined && // Nouveau champ à valider
        parsedResult.alternativeIdentifications && // Nouveau champ à valider
        parsedResult.justificationText !== undefined // Nouveau champ à valider
      ) {
        console.log(
          `[${new Date().toISOString()}] Réponse finale envoyée à l'application mobile: ${JSON.stringify(parsedResult)}`
        );
        console.log(`[${new Date().toISOString()}] Valeur de danger avant envoi: ${parsedResult.danger}`);
        console.log(
          `[${new Date().toISOString()}] Réponse envoyée: Succès. Temps total: ${Date.now() - startTime} ms.`
        );
        // Déduire les crédits après un traitement réussi et juste avant d'envoyer la réponse.
        await decrementCreditsWithAmount(uid, MODEL_CREDITS[modelId]);
        return res.status(200).json(parsedResult);
      } else {
        // L'ancien log de débogage `[DEBUG_VALIDATION_FAILED]` a été supprimé.
        console.error(
          "La réponse JSON de Gemini est valide mais ne contient pas tous les champs attendus:",
          parsedResult // Afficher le résultat complet pour le débogage si l'erreur persiste
        );
        return res
          .status(500)
          .json({ message: "Erreur: Réponse de l'IA dans un format inattendu ou champs manquants." });
      }
    } catch (parseError) {
      console.error("Erreur lors du parsing JSON ou réponse inattendue de Gemini:", parseError);
      return res
        .status(500)
        .json({ message: "Erreur: Impossible d'interpréter la réponse de l'IA ou format JSON invalide." });
    }
  } catch (error) {
    console.error("Erreur générale lors de l'analyse de l'image:", error);
    res
      .status(500)
      .json({ message: "Erreur interne du serveur lors de l'analyse de l'image.", error: error.message });
  } finally {
    console.log(
      `[${new Date().toISOString()}] Fin de la requête analyzeImage. Durée totale: ${Date.now() - startTime} ms`
    );
  }
}

// Route Express identique
app.post("/analyze-image", analyzeImage);

// Export de la fonction HTTPS Firebase
exports.api = onRequest({ region: REGION, secrets: [GEMINI_API_KEY], timeoutSeconds: 120 }, app);

// Déclencheur v1 (compat) pour la création d'utilisateur Firebase Auth
exports.onUserCreate = functionsV1.auth.user().onCreate(async (user) => {
  const { uid, email } = user;
  console.log(`Nouvel utilisateur créé: ${uid} (${email})`);
  try {
    await db.collection('users').doc(uid).set({
      email: email || null,
      credits: 10,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    console.log(`Document utilisateur créé pour ${uid}`);
    return null;
  } catch (error) {
    console.error(`Erreur lors de la création du document utilisateur pour ${uid}:`, error);
    return null;
  }
});

// Fonction utilitaire pour décrémenter un nombre spécifique de crédits
async function decrementCreditsWithAmount(uid, amount) {
  console.log(`[decrementCreditsWithAmount] uid=${uid} - début, montant=${amount}`);
  try {
    await db.runTransaction(async (tx) => {
      const userRef = db.collection('users').doc(uid);
      const snap = await tx.get(userRef);
      if (!snap.exists) {
        throw new Error('Document utilisateur introuvable.');
      }
      const credits = snap.get('credits') || 0;
      console.log(`[decrementCreditsWithAmount] uid=${uid} - crédits actuels=${credits}`);
      if (credits < amount) {
        throw new Error('Crédits insuffisants.');
      }
      tx.update(userRef, {
        credits: FieldValue.increment(-amount),
        updatedAt: FieldValue.serverTimestamp(),
      });
    });
    console.log(`[decrementCreditsWithAmount] uid=${uid} - succès décrémentation de ${amount} crédits`);
  } catch (error) {
    console.error(`[decrementCreditsWithAmount] uid=${uid} - erreur`, error);
    throw error;
  }
}

// Callable: décrémente 1 crédit de l'utilisateur authentifié, via transaction Firestore
exports.decrementCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError('unauthenticated', 'Authentification requise.');
  }
  const uid = context.auth.uid;
  try {
    await decrementCreditsWithAmount(uid, 1);
    return { success: true };
  } catch (error) {
    if (error.message === 'Document utilisateur introuvable.') {
      throw new functionsV1.https.HttpsError('not-found', error.message);
    } else if (error.message === 'Crédits insuffisants.') {
      throw new functionsV1.https.HttpsError('failed-precondition', error.message);
    }
    throw new functionsV1.https.HttpsError('internal', error.message || 'Erreur serveur');
  }
});

// Callable: retourne le solde de crédits courant de l'utilisateur authentifié
exports.getCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError('unauthenticated', 'Authentification requise.');
  }
  const uid = context.auth.uid;
  console.log(`[getCredits] uid=${uid} - début`);
  const snap = await db.collection('users').doc(uid).get();
  if (!snap.exists) {
    throw new functionsV1.https.HttpsError('not-found', 'Document utilisateur introuvable.');
  }
  const dataDoc = snap.data() || {};
  const credits = dataDoc.credits || 0;
  console.log(`[getCredits] uid=${uid} - crédits=${credits}`);
  return { credits };
});

// Mapping produit -> crédits (à aligner avec Play Console)
const PRODUCT_CREDITS = {
  credits_25: 25,
  credits_100: 100,
  credits_500: 500,
};

// Nom du package Android, fourni via variable d'environnement Functions
// Récupéré via paramètre Firebase (.env.*), pas via legacy config

// Callable: vérifie l'achat via AndroidPublisher et crédite l'utilisateur
exports.verifyAndGrantCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError('unauthenticated', 'Authentification requise.');
  }
  const uid = context.auth.uid;
  const productId = data?.productId;
  const purchaseToken = data?.purchaseToken;

  if (!productId || !purchaseToken) {
    throw new functionsV1.https.HttpsError('invalid-argument', 'productId et purchaseToken requis.');
  }
  const creditsToGrant = PRODUCT_CREDITS[productId] || 0;
  if (creditsToGrant <= 0) {
    throw new functionsV1.https.HttpsError('failed-precondition', 'Produit inconnu.');
  }
  if (!ANDROID_PACKAGE_NAME.value()) {
    console.error('ANDROID_PACKAGE_NAME non défini dans les variables d’environnement Functions.');
    throw new functionsV1.https.HttpsError('failed-precondition', 'Configuration serveur manquante.');
  }

  const tokenDocRef = db.collection('purchaseTokens').doc(purchaseToken);
  const tokenSnap = await tokenDocRef.get();
  if (tokenSnap.exists) {
    console.log(`[verifyAndGrantCredits] token déjà traité: ${purchaseToken}`);
    return { alreadyProcessed: true };
  }

  try {
    const auth = new google.auth.GoogleAuth({
      scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    const androidpublisher = google.androidpublisher({ version: 'v3', auth });

    const { data: purchase } = await androidpublisher.purchases.products.get({
      packageName: ANDROID_PACKAGE_NAME.value(),
      productId,
      token: purchaseToken,
    });

    // purchase.purchaseState === 0 => acheté
    if (purchase?.purchaseState !== 0) {
      console.warn(`[verifyAndGrantCredits] Achat non valide pour token=${purchaseToken}:`, purchase);
      throw new functionsV1.https.HttpsError('failed-precondition', 'Achat non valide ou non finalisé.');
    }

    await db.runTransaction(async (tx) => {
      const userRef = db.collection('users').doc(uid);
      const userSnap = await tx.get(userRef);
      if (!userSnap.exists) {
        throw new functionsV1.https.HttpsError('not-found', 'Utilisateur introuvable.');
      }
      tx.update(userRef, {
        credits: FieldValue.increment(creditsToGrant),
        updatedAt: FieldValue.serverTimestamp(),
      });
      tx.set(tokenDocRef, {
        uid,
        productId,
        creditsGranted: creditsToGrant,
        createdAt: FieldValue.serverTimestamp(),
      });
    });

    console.log(`[verifyAndGrantCredits] uid=${uid} - +${creditsToGrant} crédits accordés pour ${productId}`);
    return { success: true, creditsGranted: creditsToGrant };
  } catch (err) {
    if (err instanceof functionsV1.https.HttpsError) throw err;
    console.error('[verifyAndGrantCredits] Erreur serveur:', err);
    throw new functionsV1.https.HttpsError('internal', err.message || 'Erreur de vérification d’achat');
  }
});

// Importez votre configuration d'authentification Google Play Developer ici
// Pour une implémentation complète, vous devrez gérer l'authentification avec une clé de service
// et utiliser une bibliothèque cliente pour l'API Google Play Developer.
// Ceci est une SIMPLIFICATION.

// ----------------------------------------------------------------------
// Configuration de l'authentification Google Play Developer API
// ----------------------------------------------------------------------

const packageName = 'com.pastaga.geronimo'; // <<< REMPLACEZ PAR LE PACKAGE DE VOTRE APP

// ----------------------------------------------------------------------
// Fonction Firebase pour la vérification des abonnements
// ----------------------------------------------------------------------

exports.verifyAndGrantSubscription = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'The function must be called while authenticated.');
    }

    const { productId, purchaseToken } = data;

    if (!productId || !purchaseToken) {
        throw new functions.https.HttpsError('invalid-argument', 'The function must be called with productId and purchaseToken.');
    }

    try {
        // Préparer le client AndroidPublisher à la demande (lazy), pour éviter les erreurs d'init au chargement
        const encoded = process.env.PLAYSTORE_SERVICE_ACCOUNT_KEY;
        if (!encoded) {
            console.error('PLAYSTORE_SERVICE_ACCOUNT_KEY manquant dans les variables d’environnement.');
            throw new functions.https.HttpsError('failed-precondition', 'Configuration serveur manquante.');
        }
        let credentials;
        try {
            credentials = JSON.parse(Buffer.from(encoded, 'base64').toString('utf8'));
        } catch (e) {
            console.error('PLAYSTORE_SERVICE_ACCOUNT_KEY invalide (Base64 ou JSON).');
            throw new functions.https.HttpsError('failed-precondition', 'Clé de service invalide.');
        }
        const auth = new google.auth.GoogleAuth({
            credentials,
            scopes: ['https://www.googleapis.com/auth/androidpublisher']
        });
        const androidpublisher = google.androidpublisher({
            version: 'v3',
            auth
        });

        // --- Étape 2: Vérifier l'achat auprès de Google Play ---
        const subscriptionResponse = await androidpublisher.purchases.subscriptions.get({
            packageName: packageName,
            subscriptionId: productId,
            token: purchaseToken
        });

        const subscription = subscriptionResponse.data;

        // Vérifiez le statut de l'abonnement
        // Un abonnement est actif si purchaseState est PURCHASES_PURCHASED (0)
        // et qu'il n'est pas expiré (expiryTimeMillis > Date.now())
        const isSubscriptionValid = subscription.purchaseState === 0 &&
                                    subscription.expiryTimeMillis > Date.now();

        if (isSubscriptionValid) {
            // --- Étape 3: Mettre à jour le statut de l'utilisateur dans votre base de données ---
            const userId = context.auth.uid;
            const now = admin.firestore.FieldValue.serverTimestamp();

            await admin.firestore().collection('users').doc(userId).set({
                isPremium: true,
                subscriptionId: productId,
                purchaseToken: purchaseToken,
                subscriptionStartDate: new Date(parseInt(subscription.startTimeMillis)),
                subscriptionExpiryDate: new Date(parseInt(subscription.expiryTimeMillis)),
                autoRenewing: subscription.autoRenewing,
                // Ajoutez ici la logique pour les requêtes si elles se réinitialisent mensuellement
                // Par exemple : requestsRemaining: getRequestsForProduct(productId),
                //               requestsLastReset: now,
            }, { merge: true });

            return { status: 'success', message: 'Subscription verified and granted.' };
        } else {
            throw new functions.https.HttpsError('permission-denied', 'Subscription is not active or valid.');
        }

    } catch (error) {
        console.error('Error verifying subscription:', error);
        // Distinguer les erreurs pour un meilleur débogage côté client si nécessaire
        if (error.code === 404) { // Not found - token invalid or expired
             throw new functions.https.HttpsError('not-found', 'Subscription not found or invalid token.');
        }
        throw new functions.https.HttpsError('internal', 'Unable to verify subscription.', error.message);
    }
});

// Ajoutez ici une fonction utilitaire si vous avez besoin de mapper les IDs de produits aux quotas de requêtes
/*
function getRequestsForProduct(productId) {
    switch (productId) {
        case 'pack_requetes_25': return 25;
        case 'pack_requetes_100': return 100;
        case 'pack_500': return 500;
        default: return 0;
    }
}
*/

