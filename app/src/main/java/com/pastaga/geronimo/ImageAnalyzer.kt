package com.pastaga.geronimo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import android.graphics.ImageDecoder
import android.os.Build

class InsufficientCreditsException(message: String) : Exception(message)

class ImageAnalyzer(private val context: Context) {

    private val BACKEND_API_URL = "https://europe-west9-geronimo-7224a.cloudfunctions.net/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(150, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // Désactiver les tentatives de relance automatiques
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_API_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val naturePeiService = retrofit.create(NaturePeiApiService::class.java)

    private fun getCombinedPrompt(): String {
        val systemPrompt = "Vous êtes un expert en biodiversité (faune et flore) spécialisé dans les espèces de l\'Île de la Réunion. Identifiez l\'espèce et fournissez des informations structurées."
        val userQuery = """
            Analysez cette image et fournissez une réponse JSON stricte avec les 6 champs suivants:
            1.  "localName": Nom commun en français (ex: "Tamarin des Hauts") ou "N/C" si inconnu.
            2.  "scientificName": Nom scientifique latin (ex: "Acacia heterophylla") ou "N/C" si inconnu.
            3.  "type": Type d'espèce et son statut combinés (ex: "Plante endémique", "Oiseau introduit", "Animal non endémique", "N/C") ou "N/C" si inconnu.
            4.  "habitat": Habitat principal à La Réunion (ex: "Forêts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
            5.  "characteristics": Description physique COURTE et synthétique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
            6.  "reunionContext": Contexte réunionnais COURT et synthétique (usages, écologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.
            
            Si l'espèce ne peut pas être identifiée ou si un champ est inconnu, utilisez "N/C".
            Répondez UNIQUEMENT avec le JSON, sans texte supplémentaire.
        """.trimIndent()
        return "```json\n${systemPrompt}\n\n${userQuery}\n```"
    }

    /**
     * Redimensionne un bitmap pour que son bord long ne dépasse pas maxDimension
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Si l'image est déjà assez petite, pas besoin de la redimensionner
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        // Calculer le ratio de redimensionnement
        val ratio = if (width > height) {
            maxDimension.toFloat() / width.toFloat()
        } else {
            maxDimension.toFloat() / height.toFloat()
        }
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
    
    private fun bitmapToBase64(bitmap: Bitmap, mimeType: String): String {
        // Redimensionner le bitmap avant encodage pour réduire la taille
        val resizedBitmap = resizeBitmap(bitmap, 600) // Définit la taille maximale pour l'analyse à 600x600 pixels
        
        val byteArrayOutputStream = ByteArrayOutputStream()
        val format = when (mimeType) {
            "image/jpeg" -> Bitmap.CompressFormat.JPEG
            "image/png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG // Default to JPEG
        }
        // Optimisation : Réduire à 60% pour l'analyse IA (suffisant pour la reconnaissance)
        resizedBitmap.compress(format, 40, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        
        // Libérer la mémoire du bitmap redimensionné si différent de l'original
        if (resizedBitmap != bitmap) {
            resizedBitmap.recycle()
        }
        
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    interface NaturePeiApiService {
        @POST("analyze-image")
        suspend fun analyzeImage(@Body request: AnalyzeImageRequest): AnalyzeImageResponse
    }

    data class AnalyzeImageRequest(
        val image: String,
        val mimeType: String,
        val country: String? = null,
        val region: String? = null,
        val modelId: String,
        val uid: String,
        val language: String? = null // Nouveau champ pour la langue
    )

    data class AnalyzeImageResponse(
        val localName: String,
        val scientificName: String,
        val type: String,
        val habitat: String,
        val characteristics: String,
        val localContext: String,
        val Peculiarities: String? = null, // Nouveau champ pour les particularités
        val representativeColorHex: String?,
        val danger: Boolean = false, // Nouveau champ danger
        val confidenceScore: Int? = null, // Score de confiance de l'IA (0-100)
        val alternativeIdentifications: List<AlternativeIdentification>? = null, // Autres possibilités identifiées
        val justificationText: String? = null // Texte de justification quand aucune alternative n'est proposée
    )


    suspend fun analyzeImage(imageUri: Uri, modelId: String, uid: String, country: String? = null, region: String? = null, language: String? = null): AnalyzeImageResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri))
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                val base64Image = bitmapToBase64(bitmap, mimeType)
                // Le prompt n'est plus construit ici, il est géré par le backend.
                // val fullPrompt = getCombinedPrompt()

                val request = AnalyzeImageRequest(
                    image = base64Image,
                    mimeType = mimeType,
                    country = country,
                    region = region,
                    modelId = modelId,
                    uid = uid,
                    language = language // Inclure le paramètre language
                )

                // Suppression de la boucle de relance explicite (maxRetries, attempt, success)
                // La requête est effectuée une seule fois. Les délais d'attente OkHttpClient sont désormais à 150s.
                val response = naturePeiService.analyzeImage(request)
                response
            } catch (e: HttpException) {
                // Gérer spécifiquement les erreurs HTTP (code 400 pour crédits insuffisants)
                if (e.code() == 400) {
                    val errorBody = e.response()?.errorBody()?.string()
                    if (errorBody?.contains("Crédits insuffisants") == true || errorBody?.contains("insuffisants") == true) {
                        throw InsufficientCreditsException("Crédits insuffisants pour effectuer cette analyse.")
                    }
                }
                null
            } catch (e: Exception) {
                null
            }
        }
    }
}



