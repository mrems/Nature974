package com.pastaga.geronimo

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object CreditsManager {

    /**
     * Décrémente un crédit côté serveur via la callable `decrementCredits`.
     * Lève une exception si non authentifié, crédits insuffisants ou erreur serveur.
     */
    suspend fun decrementOneCredit() {
        try {
            android.util.Log.d("CreditsManager", "Appel decrementCredits: début")
            FirebaseFunctions.getInstance()
                .getHttpsCallable("decrementCredits")
                .call()
                .await()
            android.util.Log.d("CreditsManager", "Appel decrementCredits: succès")
        } catch (e: Exception) {
            android.util.Log.e("CreditsManager", "Appel decrementCredits: erreur", e)
            throw e
        }
    }
}



