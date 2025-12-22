package com.geronimo.geki.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class BillingManager(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onPurchaseSuccess: (productId: String, purchaseToken: String) -> Unit,
    private val onError: (Throwable) -> Unit
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun start() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                } else {
                    onError(IllegalStateException("Billing init failed: ${result.debugMessage}"))
                }
            }

            override fun onBillingServiceDisconnected() {
                // Caller may retry by calling start() again
            }
        })
    }

    suspend fun queryProducts(productIds: List<String>): List<ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val productList = productIds.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.SUBS) // CHANGER ICI pour SUBSCRIPTIONS
                    .build()
            }
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(productDetailsList ?: emptyList())
                } else {
                    cont.resume(emptyList())
                }
            }
        }

    fun launchPurchase(activity: Activity, product: ProductDetails) {
        // Pour les abonnements, un offerToken est requis
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: run {
                onError(IllegalStateException("Aucune offre d'abonnement disponible pour ${product.productId}"))
                return
            }
        val pdParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offerToken)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(pdParams))
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK || purchases.isNullOrEmpty()) return
        purchases.filter { it.products.isNotEmpty() && it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                val productId = purchase.products.first()
                val token = purchase.purchaseToken
                onPurchaseSuccess(productId, token)
            }
    }

    fun consume(token: String, onConsumed: () -> Unit, onFail: (Throwable) -> Unit) {
        val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        billingClient.consumeAsync(params) { billingResult, _ ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onConsumed()
            } else {
                onFail(IllegalStateException("Consume failed: ${billingResult.debugMessage}"))
            }
        }
    }

    fun acknowledge(token: String, onAck: () -> Unit, onFail: (Throwable) -> Unit) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(token)
            .build()
        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                onAck()
            } else {
                onFail(IllegalStateException("Acknowledge failed: ${billingResult.debugMessage}"))
            }
        }
    }

    // Pour les abonnements, nous devons Ã©galement interroger les achats existants.
    suspend fun queryExistingPurchases(productType: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(productType).build()
            ) { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases ?: emptyList())
                } else {
                    cont.resume(emptyList())
                }
            }
        }
}


