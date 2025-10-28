/**
 * Cloud Function (2nd Gen) HTTPS avec Express pour reproduire la route
 * POST /analyze-image de l'API EC2, en proxy vers Gemini.
 */
const express = require("express");
const cors = require("cors");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const { GoogleGenerativeAI } = require("@google/generative-ai");

// Région (Paris)
const REGION = "europe-west9"; 

// Middleware Express
const app = express();
app.use(cors({ origin: true }));
app.use(express.json({ limit: "15mb" }));

// Déclaration du secret Firebase pour la clé Gemini
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");

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
    const { image, mimeType, prompt } = req.body;

    if (!image || !mimeType) {
      console.warn("Validation échouée: champs image, mimeType ou prompt manquants.");
      return res
        .status(400)
        .json({ message: "Les champs 'image', 'mimeType' et 'prompt' sont requis." });
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
    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-preview-05-20" });

    // Prompt Gemini
    console.log(
      `[${new Date().toISOString()}] Construction du prompt Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    const geminiPrompt = `Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de l'Île de la Réunion. Identifiez l'espèce et fournissez des informations structurées.

Analysez cette image et fournissez une réponse JSON stricte avec les 6 champs suivants:
1.  "localName": Nom commun en français (ex: "Tamarin des Hauts") ou "N/C" si inconnu.
2.  "scientificName": Nom scientifique latin (ex: "Acacia heterophylla") ou "N/C" si inconnu.
3.  "type": Type d'espèce et son statut combinés (ex: "Plante endémique", "Oiseau introduit", "Animal non endémique", "N/C") ou "N/C" si inconnu.
4.  "habitat": Habitat principal à La Réunion (ex: "Forêts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
5.  "characteristics": Description physique COURTE et synthétique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
6.  "reunionContext": Contexte réunionnais COURT et synthétique (usages, écologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.

Si l'espèce ne peut pas être identifiée ou si un champ est inconnu, utilisez "N/C".
Répondez UNIQUEMENT avec le JSON, sans texte supplémentaire.`;

    let result, response, text;
    console.log(
      `[${new Date().toISOString()}] Début de l'appel à l'API Gemini. Temps écoulé depuis le début: ${Date.now() - startTime} ms.`
    );
    const geminiCallStartTime = Date.now();
    try {
      result = await model.generateContent([geminiPrompt, imagePart]);
      response = await result.response;
      text = response.text();
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
        parsedResult.reunionContext
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
