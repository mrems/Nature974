package com.geronimo.geki

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

object CreditsManager {

    /**
     * DÃ©crÃ©mente un crÃ©dit cÃ´tÃ© serveur via la callable `decrementCredits`.
     * LÃ¨ve une exception si non authentifiÃ©, crÃ©dits insuffisants ou erreur serveur.
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



