package com.example.naturepei

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
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import android.graphics.ImageDecoder
import android.os.Build

class ImageAnalyzer(private val context: Context) {

    private val BACKEND_API_URL = "https://europe-west9-geronimo-7224a.cloudfunctions.net/api/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
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

    private fun bitmapToBase64(bitmap: Bitmap, mimeType: String): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val format = when (mimeType) {
            "image/jpeg" -> Bitmap.CompressFormat.JPEG
            "image/png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG // Default to JPEG
        }
        // Optimisation : Réduire à 60% pour l'analyse IA (suffisant pour la reconnaissance)
        bitmap.compress(format, 60, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    interface NaturePeiApiService {
        @POST("analyze-image")
        suspend fun analyzeImage(@Body request: AnalyzeImageRequest): AnalyzeImageResponse
    }

    data class AnalyzeImageRequest(
        val image: String,
        val mimeType: String
        // Le prompt n'est plus envoyé par l'application, il est construit par le backend.
        // val prompt: String
    )

    data class AnalyzeImageResponse(
        val localName: String,
        val scientificName: String,
        val type: String,
        val habitat: String,
        val characteristics: String,
        val reunionContext: String
    )

    suspend fun analyzeImage(imageUri: Uri): AnalyzeImageResponse? {
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

                val request = AnalyzeImageRequest(base64Image, mimeType)

                var response: AnalyzeImageResponse? = null
                val maxRetries = 5
                var attempt = 0
                var success = false

                while (attempt < maxRetries && !success) {
                    try {
                        response = naturePeiService.analyzeImage(request)
                        success = true
                    } catch (e: Exception) {
                        attempt++
                        if (attempt < maxRetries) {
                            val delayTime = (Math.pow(2.0, attempt.toDouble()).toLong() * 1000) + (0..1000).random()
                            Log.e("ImageAnalyzer", "Tentative $attempt échouée. Nouvelle tentative dans ${delayTime / 1000}s...", e)
                            kotlinx.coroutines.delay(delayTime)
                        } else {
                            throw e // Re-throw after max retries
                        }
                    }
                }
                response
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Erreur lors de l\'analyse: ", e)
                null
            }
        }
    }
}


