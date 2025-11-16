package com.pastaga.geronimo

import android.os.Bundle
import android.view.View
import androidx.cardview.widget.CardView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.google.android.gms.tasks.Tasks
import com.google.firebase.functions.FirebaseFunctions
import com.pastaga.geronimo.billing.BillingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.view.WindowManager
import androidx.core.view.WindowCompat

class PurchaseActivity : AppCompatActivity() {

    private lateinit var billingManager: BillingManager
    private lateinit var btn10: View
    private lateinit var btn50: View
    private lateinit var btn100: View

    private val productIds = listOf("pack_requetes_25", "pack_requetes_100", "pack_500")
    private val idToDetails = mutableMapOf<String, ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Définir la couleur de la barre d'état et de la barre de navigation
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.parseColor("#DD000000") // Noir avec opacité 87%
        window.navigationBarColor = Color.parseColor("#DD000000") // Noir avec opacité 87%

        // Définir les éléments de la barre d'état et de navigation en blanc
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = false

        setContentView(R.layout.activity_purchase)

        btn10 = findViewById(R.id.btn10)
        btn50 = findViewById(R.id.btn50)
        btn100 = findViewById(R.id.btn100)

        setButtonsEnabled(false)

        billingManager = BillingManager(
            context = this,
            onReady = {
                lifecycleScope.launch {
                    loadProducts()
                }
            },
            onPurchaseSuccess = { productId, token ->
                lifecycleScope.launch {
                    verifyAndGrant(productId, token)
                }
            },
            onError = { e -> 
                // Erreur Billing
                setButtonsEnabled(true)
            }
        )
        billingManager.start()

        btn10.setOnClickListener { launchPurchase("pack_requetes_25") }
        btn50.setOnClickListener { launchPurchase("pack_requetes_100") }
        btn100.setOnClickListener { launchPurchase("pack_500") }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        btn10.isEnabled = enabled
        btn50.isEnabled = enabled
        btn100.isEnabled = enabled
        val alpha = if (enabled) 1f else 0.6f
        btn10.alpha = alpha
        btn50.alpha = alpha
        btn100.alpha = alpha
    }

    private suspend fun loadProducts() {
        val details = withContext(Dispatchers.IO) {
            billingManager.queryProducts(productIds)
        }

        // Vérifier les abonnements existants
        val existingPurchases = withContext(Dispatchers.IO) {
            billingManager.queryExistingPurchases(BillingClient.ProductType.SUBS)
        }

        // TODO: Traiter existingPurchases pour mettre à jour l'UI et l'état de l'utilisateur si un abonnement est actif
        existingPurchases.forEach { purchase ->
            // Exemple: Si l'abonnement est actif, vous pouvez désactiver les boutons d'achat
            if (purchase.products.any { it in productIds }) {
                setButtonsEnabled(false)
                // Vous pourriez vouloir faire une vérification backend ici aussi pour être sûr de l'état de l'abonnement
            }
        }

        idToDetails.clear()
        details.forEach { pd ->
            val id = pd.productId
            idToDetails[id] = pd
        }
        updateButtonLabels()
        setButtonsEnabled(true)
    }

    private fun updateButtonLabels() {
        // Le layout utilise des CardView avec du texte statique.
        // Si besoin d'afficher les prix dynamiques Billing, ajouter des IDs aux TextView de prix et les mettre à jour ici.
    }

    private fun launchPurchase(id: String) {
        val pd = idToDetails[id]
        if (pd == null) {
            // Produit indisponible
            return
        }
        billingManager.launchPurchase(this, pd)
    }

    private suspend fun verifyAndGrant(productId: String, token: String) {
        setButtonsEnabled(false)
        try {
            // Pour les abonnements, nous n'appelons PAS consume() côté client.
            // La vérification et l'octroi de l'accès se font entièrement côté serveur.
            val result = withContext(Dispatchers.IO) {
                Tasks.await(
                    FirebaseFunctions.getInstance()
                        .getHttpsCallable("verifyAndGrantSubscription") // Nouvelle fonction backend pour les abonnements
                        .call(mapOf("productId" to productId, "purchaseToken" to token))
                )
            }
            // Si le backend confirme, accuser réception (acknowledge) de l'achat côté client
            withContext(Dispatchers.IO) {
                billingManager.acknowledge(
                    token,
                    onAck = {},
                    onFail = { /* journaliser mais ne pas bloquer l'UI */ }
                )
            }
            setButtonsEnabled(true)
            // Abonnement activé avec succès
            // TODO: Rafraîchir l'état de l'utilisateur/UI après un achat d'abonnement réussi
        } catch (e: Exception) {
            setButtonsEnabled(true)
            // Erreur de vérification de l'abonnement
        }
    }
}



