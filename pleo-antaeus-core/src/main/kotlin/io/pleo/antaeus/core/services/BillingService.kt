package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import kotlinx.coroutines.*

@ObsoleteCoroutinesApi
class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {

    val handler = CoroutineExceptionHandler{coroutineContext, throwable ->

    }

    suspend fun charge() = coroutineScope{
        val invoices = dal.fetchPendingInvoices()


        // Since payment provider is an external service
        // run it in a coroutine to avoid blocking the main thread.
        // This code also runs asynchronously hence faster.
        val deferredResults = invoices.map { invoice ->
            async (handler) {
                val result = paymentProvider.charge(invoice)
                Pair(invoice, result)
            }
        }

        // get invoices that were successfully charged
        val results = deferredResults.awaitAll().filter { it.second }.map { it.first }


    }


}