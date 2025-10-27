/**
 * Cloud Function (2nd Gen) pour agir comme un proxy vers l'API Gemini.
 * Cette fonction est nommée 'testGeminiKey' et est configurée pour autoriser
 * les requêtes CORS depuis n'importe quelle origine.
 */
const { onCall } = require("firebase-functions/v2/https");
const { GoogleGenAI } = require("@google/genai");

// Définir la région (Europe West 9 - Paris)
const REGION = "europe-west9"; 
// Taille maximale autorisée pour l'image Base64 (environ 10 Mo)
const MAX_IMAGE_SIZE_MB = 10;
const MAX_BASE64_LENGTH = MAX_IMAGE_SIZE_MB * 1024 * 1024 * 1.33; // ~33% de overhead pour base64

// Configuration de la fonction testGeminiKey
exports.testGeminiKey = onCall({ region: REGION, cors: true }, async (request) => {
    
    const { apiKey, prompt, base64Image, mimeType } = request.data;

    // 1. Validation des arguments
    if (!apiKey || !prompt || !base64Image || !mimeType) {
        throw new Error("Missing required fields: apiKey, prompt, base64Image, or mimeType.");
    }
    
    // 2. Validation de la taille de l'image (pour éviter les Timeouts ou les 400)
    if (base64Image.length > MAX_BASE64_LENGTH) {
        throw new Error(`Image is too large. Max size supported is about ${MAX_IMAGE_SIZE_MB}MB.`);
    }

    try {
        // Initialisation de l'API Gemini avec la clé fournie par le client
        const ai = new GoogleGenAI({ apiKey });

        // Construction du contenu multi-modal (texte + image)
        const contents = [
            {
                role: "user",
                parts: [
                    { text: prompt },
                    {
                        inlineData: {
                            mimeType: mimeType,
                            data: base64Image
                        }
                    }
                ]
            }
        ];

        // Appel à l'API Gemini Vision
        const response = await ai.models.generateContent({
            model: "gemini-2.5-flash-preview-09-2025",
            contents: contents
        });
        
        // 3. Traitement de la réponse
        const text = response.candidates?.[0]?.content?.parts?.[0]?.text;
        
        if (!text) {
             throw new Error("Gemini returned no text content or response structure was invalid.");
        }

        return { response: text };

    } catch (error) {
        console.error("Gemini API Error:", error.message);

        // Analyse détaillée de l'erreur pour le client
        const errorMessage = error.message;
        const errorCodeMatch = errorMessage.match(/\[(\d+)\]/);
        const errorCode = errorCodeMatch ? errorCodeMatch[1] : 'Inconnu';
        
        // Si l'erreur est liée à la clé/IP (peu probable maintenant, mais géré)
        if (errorMessage.includes('403') || errorMessage.includes('PERMISSION_DENIED')) {
            throw new Error(`Erreur ${errorCode}: L'IP de La Réunion pourrait être bloquée. Contactez le support.`);
        }

        // Si l'erreur est un Bad Request, il faut que le client essaie une autre image
        if (errorMessage.includes('400') || errorMessage.includes('INVALID_ARGUMENT')) {
            throw new Error(`Erreur ${errorCode}: La requête (image/format) est invalide. Veuillez essayer avec une image plus petite ou un autre format.`);
        }
        
        // Erreur générique
        throw new Error(`Erreur Gemini: ${errorMessage}`);
    }
});
