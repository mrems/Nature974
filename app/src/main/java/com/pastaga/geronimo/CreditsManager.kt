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
            FirebaseFunctions.getInstance()
                .getHttpsCallable("decrementCredits")
                .call()
                .await()
        } catch (e: Exception) {
            throw e
        }
    }
}



