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
  'gemini-3-pro-preview': 5,
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
    const { image, mimeType, prompt, country, region, modelId, uid } = req.body;

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
      locationContext = `L'utilisateur se trouve en ${country}.`;
      if (region) {
        locationContext += ` Région indiquée: ${region}. N'utilisez la région que si vous disposez d'informations factuelles, spécifiques et non génériques propres à cette région (habitat, statut, usages). Si vous n'êtes pas certain ou si la région ressemble à une ville/quartier, ignorez-la et ne la mentionnez pas.`;
      }
      locationContext += "\n\n"; // Ajouter une ligne vide pour une meilleure séparation
    }
    // Construire dynamiquement la spécialisation selon la localisation fournie
    let expertiseLine = "Vous êtes un expert en biodiversité (faune et flore), pierres et minéraux.";
    if (country) {
      expertiseLine = `Vous êtes un expert en biodiversité (faune et flore), pierres et minéraux, spécialisé dans les espèces et formations géologiques de ${country}.`;
    }

    const geminiPrompt = `${locationContext}${expertiseLine}

Si on t'envoie la photo d'un humain ou d'un objet manufacturé et qu'il n'est pas le sujet principal et évident de l'image, ignore-le. Concentre-toi sur l'identification et l'analyse de la faune, la flore, les champignons, les pierres et minéraux. Si l'humain est le sujet principal, alors tu dois remplir tous les champs de manière humoristique et sarcastique, toujours nouvelle, qui exprime ton imagination et ton humour.

Règle d'utilisation de la région: La variable 'country' est toujours utilisée. La variable 'region' est optionnelle et NE DOIT ÊTRE utilisée que si vous avez des informations spécifiques et vérifiables pour cette région. Sinon, n'incluez aucune référence à la région dans la réponse.

Analysez cette image et fournissez une réponse JSON stricte avec les 12 champs suivants:
1.  "localName": Nom commun en français (ex: "Merle noir") ou "N/C" si inconnu.
2.  "scientificName": Nom scientifique latin (ex: "Turdus merula") ou "N/C" si inconnu.
3.  "type": Type d'espèce et statut (si pertinent). Soyez concis. Utilisez des termes comme "Plante endémique", "Oiseau", "Poisson tropical", "Liane grimpante", "Felin", "Insecte", "Plante carnivore", "Coquillage marin", "Crustacé", "Plante ornementale". Évitez les redondances comme "cultivée" ou "introduite" si le type est déjà clair. Utilisez "N/C" si inconnu.
4.  "habitat": Habitat principal (ex: "Forêts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
5.  "characteristics": Description physique COURTE et synthétique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
6.  "localContext": Contexte local basé sur country="${country ?? 'N/C'}".${region ? ` N'incluez region="${region}" QUE si vous disposez d'informations concrètes, spécifiques et non génériques à cette région; sinon, n'évoquez pas la région.` : ''} (usages, écologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.
7.  "Peculiarities": Particularités (vertus médicinales, propriétés en lithothérapie, comportement, caractère, alimentation, reproduction). **EXCLUSIVEMENT** si un DANGER GRAVE ET RÉEL existe : inclure sa dangerosité/toxicité spécifique pour les humains (y compris les enfants), les chiens et les chats domestiques (ex: "plante toxique pour les reins", "morsure venimeuse", "animal venimeux, morsure douloureuse"). **DANS TOUS LES AUTRES CAS (AUCUN DANGER GRAVE, OU INFORMATION NON PERTINENTE SUR L'ABSENCE DE DANGER), NE MENTIONNEZ JAMAIS L'ABSENCE DE DANGER OU DE TOXICITÉ**. Soyez concis.
8.  "representativeColorHex": "Un code couleur hexadécimal (ex: \"#FF5733\") qui représente le mieux l'espèce identifiée dans l'image. Si l'espèce ne peut être identifiée, utilisez \"#CCCCCC\". **LA VALEUR DOIT IMPÉRATIVEMENT ÊTRE ENTRE GUILLEMETS DOUBLES.**"
9.  "danger": true si l'espèce analysée présente un DANGER MORTEL pour les humains (adultes et enfants) ou les animaux de compagnie (chiens et chats) (par exemple, plantes/champignons toxiques, prédateurs dangereux, animaux venimeux, etc.). Sinon, false.
10. "confidenceScore": Un entier représentant le pourcentage de confiance de l'IA dans son identification (0-100). N'utilisez "N/C" que si l'identification principale est "N/C".
11. "alternativeIdentifications": Un tableau d'objets JSON si l'IA hésite entre plusieurs identifications. Chaque objet doit avoir:
    - "scientificName": Nom scientifique latin de l'alternative.
    - "localName": Nom commun français de l'alternative, ou "N/C" si inconnu.
    - "difference": Description COURTE et précise (max 2-3 phrases) des caractéristiques permettant de la distinguer de l'identification principale (ex: "Se différencie par la taille des feuilles plus grandes et l'absence de poils sur la tige."). Si aucune différence notable, utilisez "N/C".
    Si aucune alternative n'est pertinente, renvoyez un tableau vide [].
12. "justificationText": Une explication concise (environ 20 mots) du score de confiance et de l'absence d'alternatives, UNIQUEMENT si "alternativeIdentifications" est un tableau vide. Sinon, "N/A".

Si l'espèce ne peut pas être identifiée ou si un champ est inconnu, utilisez "N/C".
Répondez UNIQUEMENT avec le JSON, sans texte supplémentaire.`;

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

