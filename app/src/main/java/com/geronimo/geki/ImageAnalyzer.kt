package com.geronimo.geki

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
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
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.lang.reflect.Type

class InsufficientCreditsException(message: String) : Exception(message)

class ImageAnalyzer(private val context: Context) {

    private val BACKEND_API_URL = "https://europe-west9-geronimo-7224a.cloudfunctions.net/api/"

    /**
     * DÃ©sÃ©rialiseur tolÃ©rant pour Ã©viter que l'app considÃ¨re "aucune rÃ©ponse"
     * lorsque le backend renvoie des champs avec un type un peu diffÃ©rent
     * (ex: confidenceScore = "N/C" au lieu d'un entier).
     */
    private class AnalyzeImageResponseDeserializer : JsonDeserializer<AnalyzeImageResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): AnalyzeImageResponse {
            val obj = json.asJsonObjectOrNull()
                ?: throw JsonParseException("AnalyzeImageResponse: JSON non objet")

            fun str(key: String, default: String = "N/C"): String {
                val el = obj.get(key)
                return if (el == null || el.isJsonNull) default else el.asString
            }

            fun bool(key: String, default: Boolean = false): Boolean {
                val el = obj.get(key) ?: return default
                if (el.isJsonNull) return default
                return try {
                    when {
                        el.isJsonPrimitive && el.asJsonPrimitive.isBoolean -> el.asBoolean
                        el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.equals("true", ignoreCase = true)
                        else -> default
                    }
                } catch (_: Exception) {
                    default
                }
            }

            fun intOrNullFlexible(key: String): Int? {
                val el = obj.get(key) ?: return null
                if (el.isJsonNull) return null
                return try {
                    when {
                        el.isJsonPrimitive && el.asJsonPrimitive.isNumber -> el.asInt
                        el.isJsonPrimitive && el.asJsonPrimitive.isString -> el.asString.toIntOrNull()
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
            }

            fun stringOrNull(key: String): String? {
                val el = obj.get(key) ?: return null
                if (el.isJsonNull) return null
                return try { el.asString } catch (_: Exception) { null }
            }

            fun <T> deserializeOrNull(key: String, type: Type): T? {
                val el = obj.get(key) ?: return null
                if (el.isJsonNull) return null
                return try { context.deserialize<T>(el, type) } catch (_: Exception) { null }
            }

            // Champs principaux: fallback "N/C" si absent
            val localName = str("localName")
            val scientificName = str("scientificName")
            val type = str("type")
            val habitat = str("habitat")
            val characteristics = str("characteristics")
            val localContext = str("localContext")

            // Champs optionnels / tolÃ©rants
            val peculiarities = stringOrNull("Peculiarities") // backend utilise volontairement P majuscule
            val representativeColorHex = stringOrNull("representativeColorHex")
            val danger = bool("danger", default = false)
            val confidenceScore = intOrNullFlexible("confidenceScore")

            val altType: Type = com.google.gson.reflect.TypeToken.getParameterized(
                List::class.java,
                AlternativeIdentification::class.java
            ).type
            val alternativeIdentifications: List<AlternativeIdentification>? =
                deserializeOrNull<List<AlternativeIdentification>>("alternativeIdentifications", altType)

            val justificationText = stringOrNull("justificationText")

            return AnalyzeImageResponse(
                localName = localName,
                scientificName = scientificName,
                type = type,
                habitat = habitat,
                characteristics = characteristics,
                localContext = localContext,
                Peculiarities = peculiarities,
                representativeColorHex = representativeColorHex,
                danger = danger,
                confidenceScore = confidenceScore,
                alternativeIdentifications = alternativeIdentifications,
                justificationText = justificationText
            )
        }

        private fun JsonElement.asJsonObjectOrNull(): JsonObject? =
            try { if (this.isJsonObject) this.asJsonObject else null } catch (_: Exception) { null }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(150, TimeUnit.SECONDS)
        .readTimeout(150, TimeUnit.SECONDS)
        .writeTimeout(150, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // DÃ©sactiver les tentatives de relance automatiques
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(AnalyzeImageResponse::class.java, AnalyzeImageResponseDeserializer())
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BACKEND_API_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val naturePeiService = retrofit.create(NaturePeiApiService::class.java)

    private fun getCombinedPrompt(): String {
        val systemPrompt = "Vous Ãªtes un expert en biodiversitÃ© (faune et flore) spÃ©cialisÃ© dans les espÃ¨ces de l\'ÃŽle de la RÃ©union. Identifiez l\'espÃ¨ce et fournissez des informations structurÃ©es."
        val userQuery = """
            Analysez cette image et fournissez une rÃ©ponse JSON stricte avec les 6 champs suivants:
            1.  "localName": Nom commun en franÃ§ais (ex: "Tamarin des Hauts") ou "N/C" si inconnu.
            2.  "scientificName": Nom scientifique latin (ex: "Acacia heterophylla") ou "N/C" si inconnu.
            3.  "type": Type d'espÃ¨ce et son statut combinÃ©s (ex: "Plante endÃ©mique", "Oiseau introduit", "Animal non endÃ©mique", "N/C") ou "N/C" si inconnu.
            4.  "habitat": Habitat principal Ã  La RÃ©union (ex: "ForÃªts humides >1200m", "Littoral", "Milieux urbains") ou "N/C" si inconnu.
            5.  "characteristics": Description physique COURTE et synthÃ©tique (taille, couleur, forme, max 2-3 phrases) ou "N/C" si inconnu.
            6.  "reunionContext": Contexte rÃ©unionnais COURT et synthÃ©tique (usages, Ã©cologie, anecdote culturelle, max 2-3 phrases) ou "N/C" si inconnu.
            
            Si l'espÃ¨ce ne peut pas Ãªtre identifiÃ©e ou si un champ est inconnu, utilisez "N/C".
            RÃ©pondez UNIQUEMENT avec le JSON, sans texte supplÃ©mentaire.
        """.trimIndent()
        return "```json\n${systemPrompt}\n\n${userQuery}\n```"
    }

    /**
     * Redimensionne un bitmap pour que son bord long ne dÃ©passe pas maxDimension
     */
    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Si l'image est dÃ©jÃ  assez petite, pas besoin de la redimensionner
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
        // Redimensionner le bitmap avant encodage pour rÃ©duire la taille
        val resizedBitmap = resizeBitmap(bitmap, 600) // DÃ©finit la taille maximale pour l'analyse Ã  600x600 pixels
        
        val byteArrayOutputStream = ByteArrayOutputStream()
        val format = when (mimeType) {
            "image/jpeg" -> Bitmap.CompressFormat.JPEG
            "image/png" -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG // Default to JPEG
        }
        // Optimisation : RÃ©duire Ã  60% pour l'analyse IA (suffisant pour la reconnaissance)
        resizedBitmap.compress(format, 40, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        
        // LibÃ©rer la mÃ©moire du bitmap redimensionnÃ© si diffÃ©rent de l'original
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
        val localName: String = "N/C",
        val scientificName: String = "N/C",
        val type: String = "N/C",
        val habitat: String = "N/C",
        val characteristics: String = "N/C",
        val localContext: String = "N/C",
        val Peculiarities: String? = null, // Nouveau champ pour les particularitÃ©s
        val representativeColorHex: String? = null,
        val danger: Boolean = false, // Nouveau champ danger
        val confidenceScore: Int? = null, // Score de confiance de l'IA (0-100)
        val alternativeIdentifications: List<AlternativeIdentification>? = null, // Autres possibilitÃ©s identifiÃ©es
        val justificationText: String? = null // Texte de justification quand aucune alternative n'est proposÃ©e
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
                // Le prompt n'est plus construit ici, il est gÃ©rÃ© par le backend.
                // val fullPrompt = getCombinedPrompt()

                val request = AnalyzeImageRequest(
                    image = base64Image,
                    mimeType = mimeType,
                    country = country,
                    region = region,
                    modelId = modelId,
                    uid = uid,
                    language = language // Inclure le paramÃ¨tre language
                )

                // Suppression de la boucle de relance explicite (maxRetries, attempt, success)
                // La requÃªte est effectuÃ©e une seule fois. Les dÃ©lais d'attente OkHttpClient sont dÃ©sormais Ã  150s.
                val response = naturePeiService.analyzeImage(request)
                response
            } catch (e: HttpException) {
                // GÃ©rer spÃ©cifiquement les erreurs HTTP (code 400 pour crÃ©dits insuffisants)
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e("ImageAnalyzer", "Erreur HTTP analyzeImage: code=${e.code()} body=${errorBody ?: "N/A"}", e)
                if (e.code() == 400) {
                    if (errorBody?.contains("CrÃ©dits insuffisants") == true || errorBody?.contains("insuffisants") == true) {
                        throw InsufficientCreditsException("CrÃ©dits insuffisants pour effectuer cette analyse.")
                    }
                }
                null
            } catch (e: Exception) {
                Log.e("ImageAnalyzer", "Erreur analyzeImage (parsing / rÃ©seau / autre): ${e.message}", e)
                null
            }
        }
    }
}



