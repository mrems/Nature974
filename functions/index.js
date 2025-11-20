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
    const { image, mimeType, prompt, country, region } = req.body;

    if (!image || !mimeType) {
      console.warn("Validation échouée: champs image ou mimeType manquants.");
      return res
        .status(400)
        .json({ message: "Les champs 'image' et 'mimeType' sont requis." });
    }

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
    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-preview-09-2025" });

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
    let expertiseLine = "Vous êtes un expert en biodiversité (faune et flore).";
    if (country) {
      expertiseLine = `Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de ${country}.`;
    }

    const geminiPrompt = `${locationContext}${expertiseLine}

Règle d'utilisation de la région: La variable 'country' est toujours utilisée. La variable 'region' est optionnelle et NE DOIT ÊTRE utilisée que si vous avez des informations spécifiques et vérifiables pour cette région. Sinon, n'incluez aucune référence à la région dans la réponse.

Analysez cette image et fournissez une réponse JSON stricte avec les 6 champs suivants:
1.  "localName": Nom commun en français (ex: "Merle noir") ou "N/C" si inconnu.
2.  "scientificName": Nom scientifique latin (ex: "Turdus merula") ou "N/C" si inconnu.
3.  "type": Type d'espèce et son statut combinés (ex: "Plante endémique", "Oiseau introduit", "Animal non endémique", "N/C") ou "N/C" si inconnu.
4.  "habitat": Habitat principal (ex: "Forêts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
5.  "characteristics": Description physique COURTE et synthétique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
6.  "localContext": Contexte local basé sur country="${country ?? 'N/C'}".${region ? ` N'incluez region="${region}" QUE si vous disposez d'informations concrètes, spécifiques et non génériques à cette région; sinon, n'évoquez pas la région.` : ''} (usages, écologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.

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

    // Parsing JSON
    console.log(
      `[${new Date().toISOString()}] Début du nettoyage et du parsing de la réponse Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    try {
      const parsedResult = JSON.parse(text);
      console.log(
        `[${new Date().toISOString()}] Fin du parsing de la réponse Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
      );
      if (
        parsedResult.localName &&
        parsedResult.scientificName &&
        parsedResult.type &&
        parsedResult.habitat &&
        parsedResult.characteristics &&
        parsedResult.localContext
      ) {
        console.log(
          `[${new Date().toISOString()}] Réponse finale envoyée à l'application mobile: ${JSON.stringify(parsedResult)}`
        );
        console.log(
          `[${new Date().toISOString()}] Réponse envoyée: Succès. Temps total: ${Date.now() - startTime} ms.`
        );
        return res.status(200).json(parsedResult);
      } else {
        console.error(
          "La réponse JSON de Gemini est valide mais ne contient pas tous les champs attendus:",
          parsedResult
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
exports.api = onRequest({ region: REGION, secrets: [GEMINI_API_KEY] }, app);

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

// Callable: décrémente 1 crédit de l'utilisateur authentifié, via transaction Firestore
exports.decrementCredits = functionsV1.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functionsV1.https.HttpsError('unauthenticated', 'Authentification requise.');
  }
  const uid = context.auth.uid;
  console.log(`[decrementCredits] uid=${uid} - début`);
  try {
    await db.runTransaction(async (tx) => {
      const userRef = db.collection('users').doc(uid);
      const snap = await tx.get(userRef);
      if (!snap.exists) {
        throw new functionsV1.https.HttpsError('not-found', 'Document utilisateur introuvable.');
      }
      const credits = snap.get('credits') || 0;
      console.log(`[decrementCredits] uid=${uid} - crédits actuels=${credits}`);
      if (credits <= 0) {
        throw new functionsV1.https.HttpsError('failed-precondition', 'Crédits insuffisants.');
      }
      tx.update(userRef, {
        credits: FieldValue.increment(-1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    });
    console.log(`[decrementCredits] uid=${uid} - succès décrémentation`);
    return { success: true };
  } catch (error) {
    if (error instanceof functionsV1.https.HttpsError) throw error;
    console.error(`[decrementCredits] uid=${uid} - erreur`, error);
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

