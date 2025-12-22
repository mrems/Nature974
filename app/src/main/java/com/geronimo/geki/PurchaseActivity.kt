package com.geronimo.geki

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
import com.geronimo.geki.billing.BillingManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Color
import android.view.WindowManager
import androidx.core.view.WindowCompat
import android.widget.Button
import android.widget.RadioGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import android.widget.LinearLayout

class PurchaseActivity : AppCompatActivity() {

    private lateinit var billingManager: BillingManager
    private lateinit var card50Identifications: CardView
    private lateinit var card100Identifications: CardView
    private lateinit var card300Identifications: CardView
    private lateinit var continueButton: Button
    private lateinit var offerRadioGroup: RadioGroup // RÃ©fÃ©rence au RadioGroup
    private lateinit var radio50Identifications: RadioButton
    private lateinit var radio100Identifications: RadioButton
    private lateinit var radio300Identifications: RadioButton
    private lateinit var linearLayout50Identifications: LinearLayout
    private lateinit var linearLayout100Identifications: LinearLayout
    private lateinit var linearLayout300Identifications: LinearLayout

    private var selectedProductId: String? = null

    private val productIds = listOf("pack_requetes_25", "pack_requetes_100", "pack_500")
    private val idToDetails = mutableMapOf<String, ProductDetails>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(this, R.color.status_bar_translucent_green_0_4) // Utiliser la couleur dÃ©finie
        window.navigationBarColor = ContextCompat.getColor(this, R.color.navigation_bar_translucent_green_0_4) // Utiliser la couleur dÃ©finie

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true

        setContentView(R.layout.activity_purchase)

        card50Identifications = findViewById(R.id.card_50_identifications)
        card100Identifications = findViewById(R.id.card_100_identifications)
        card300Identifications = findViewById(R.id.card_300_identifications)
        continueButton = findViewById(R.id.continue_button)
        offerRadioGroup = findViewById(R.id.offer_radio_group) // Initialisation du RadioGroup
        radio50Identifications = findViewById(R.id.radio_50_identifications)
        radio100Identifications = findViewById(R.id.radio_100_identifications)
        radio300Identifications = findViewById(R.id.radio_300_identifications)

        linearLayout50Identifications = findViewById(R.id.linear_layout_50_identifications)
        linearLayout100Identifications = findViewById(R.id.linear_layout_100_identifications)
        linearLayout300Identifications = findViewById(R.id.linear_layout_300_identifications)

        // Gestion des clics sur les cartes pour cocher le RadioButton correspondant
        card50Identifications.setOnClickListener {
            radio50Identifications.isChecked = true
            // Manuellement dÃ©clencher le changement si le RadioGroup n'est pas informÃ©
            offerRadioGroup.check(R.id.radio_50_identifications)
        }
        card100Identifications.setOnClickListener {
            radio100Identifications.isChecked = true
            // Manuellement dÃ©clencher le changement si le RadioGroup n'est pas informÃ©
            offerRadioGroup.check(R.id.radio_100_identifications)
        }
        card300Identifications.setOnClickListener {
            radio300Identifications.isChecked = true
            // Manuellement dÃ©clencher le changement si le RadioGroup n'est pas informÃ©
            offerRadioGroup.check(R.id.radio_300_identifications)
        }

        // Gestion de la sÃ©lection des cartes via le RadioGroup
        offerRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            // DÃ©sÃ©lectionner manuellement tous les RadioButton
            radio50Identifications.isChecked = false
            radio100Identifications.isChecked = false
            radio300Identifications.isChecked = false

            val selectedCardId: Int = when (checkedId) {
                R.id.radio_50_identifications -> {
                    radio50Identifications.isChecked = true // Cochez le bouton radio correspondant
                    R.id.card_50_identifications
                }
                R.id.radio_100_identifications -> {
                    radio100Identifications.isChecked = true // Cochez le bouton radio correspondant
                    R.id.card_100_identifications
                }
                R.id.radio_300_identifications -> {
                    radio300Identifications.isChecked = true // Cochez le bouton radio correspondant
                    R.id.card_300_identifications
                }
                else -> -1 // Aucune sÃ©lection
            }
            val selectedCard = findViewById<CardView>(selectedCardId)
            
            selectedProductId = when (checkedId) {
                R.id.radio_50_identifications -> "pack_requetes_25"
                R.id.radio_100_identifications -> "pack_requetes_100"
                R.id.radio_300_identifications -> "pack_500"
                else -> null
            }
            updateOfferCardAppearance(selectedCard)
            setContinueButtonEnabled(selectedProductId != null)
        }

        // PrÃ©sÃ©lectionner l'option 100 crÃ©dits par dÃ©faut *aprÃ¨s* que le listener soit configurÃ©
        offerRadioGroup.check(R.id.radio_100_identifications)
        // Appliquer les styles initiaux aprÃ¨s la prÃ©sÃ©lection
        updateOfferCardAppearance(findViewById<CardView>(R.id.card_100_identifications))

        // Listener pour le bouton Continuer
        continueButton.setOnClickListener {
            selectedProductId?.let { productId ->
                launchPurchase(productId)
            } ?: run {
                // GÃ©rer le cas oÃ¹ aucun produit n'est sÃ©lectionnÃ© (bien que le bouton devrait Ãªtre dÃ©sactivÃ©)
            }
        }

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
                setContinueButtonEnabled(true)
            }
        )
        billingManager.start()
    }

    private fun setContinueButtonEnabled(enabled: Boolean) {
        continueButton.isEnabled = enabled
        continueButton.alpha = if (enabled) 1f else 0.6f
    }

    private fun updateOfferCardAppearance(selectedCard: CardView?) {
        listOf(card50Identifications, card100Identifications, card300Identifications).forEach { card ->
            card.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        }

        listOf(linearLayout50Identifications, linearLayout100Identifications, linearLayout300Identifications).forEach { linearLayout ->
            linearLayout.background = ContextCompat.getDrawable(this, R.drawable.card_border_transparent)
        }
        
        // Mettre Ã  jour l'apparence de la carte sÃ©lectionnÃ©e
        selectedCard?.let { card ->
            card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.offer_pro_light))
            val selectedLinearLayout = when (card.id) {
                R.id.card_50_identifications -> linearLayout50Identifications
                R.id.card_100_identifications -> linearLayout100Identifications
                R.id.card_300_identifications -> linearLayout300Identifications
                else -> null
            }
            selectedLinearLayout?.background = ContextCompat.getDrawable(this, R.drawable.card_border_green)
        }
    }

    private suspend fun loadProducts() {
        val details = withContext(Dispatchers.IO) {
            billingManager.queryProducts(productIds)
        }

        // VÃ©rifier les abonnements existants
        val existingPurchases = withContext(Dispatchers.IO) {
            billingManager.queryExistingPurchases(BillingClient.ProductType.SUBS)
        }

        existingPurchases.forEach { purchase ->
            if (purchase.products.any { it in productIds }) {
                setContinueButtonEnabled(false)
                // Vous pourriez vouloir afficher un message Ã  l'utilisateur ici
                return@forEach
            }
        }

        idToDetails.clear()
        details.forEach { pd ->
            val id = pd.productId
            idToDetails[id] = pd
        }
        updateOfferCardPrices()
        // La sÃ©lection initiale sera gÃ©rÃ©e par le RadioGroup si un produit est prÃ©-sÃ©lectionnÃ©, ou par dÃ©faut.
    }

    private fun updateOfferCardPrices() {
        // Pour l'instant, les prix sont en dur dans le XML. 
        // Si nous voulons des prix dynamiques, nous aurions besoin d'IDs spÃ©cifiques pour les TextViews de prix dans chaque carte. 
        // Par exemple:
        // findViewById<TextView>(R.id.price_50_identifications).text = idToDetails["pack_requetes_25"]?.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.get(0)?.formattedPrice
        // Et ainsi de suite pour les autres cartes.
    }

    private fun launchPurchase(id: String) {
        val pd = idToDetails[id]
        if (pd == null) {
            setContinueButtonEnabled(false)
            return
        }
        billingManager.launchPurchase(this, pd)
    }

    private suspend fun verifyAndGrant(productId: String, token: String) {
        setContinueButtonEnabled(false)
        try {
            val result = withContext(Dispatchers.IO) {
                Tasks.await(
                    FirebaseFunctions.getInstance()
                        .getHttpsCallable("verifyAndGrantSubscription")
                        .call(mapOf("productId" to productId, "purchaseToken" to token))
                )
            }
            withContext(Dispatchers.IO) {
                billingManager.acknowledge(
                    token,
                    onAck = {},
                    onFail = { /* journaliser mais ne pas bloquer l'UI */ }
                )
            }
            setContinueButtonEnabled(true)
            // TODO: RafraÃ®chir l'Ã©tat de l'utilisateur/UI aprÃ¨s un achat d'abonnement rÃ©ussi
        } catch (e: Exception) {
            setContinueButtonEnabled(true)
            // Erreur de vÃ©rification de l'abonnement
        }
    }
}



