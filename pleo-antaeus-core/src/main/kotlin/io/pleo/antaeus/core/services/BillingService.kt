package io.pleo.antaeus.core.services

import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.*
import mu.KotlinLogging

val logger = KotlinLogging.logger {  }

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {

    private val handler = CoroutineExceptionHandler{ _, throwable ->
        if (throwable is CustomerNotFoundException) {
            logger.error(throwable) {"Customer does not exist"}
            return@CoroutineExceptionHandler
        }

        if (throwable is CurrencyMismatchException) {
            logger.error(throwable) {"Currency mismatch"}
            return@CoroutineExceptionHandler
        }

        if (throwable is NetworkException) {
            logger.error(throwable) {"Network error"}
            return@CoroutineExceptionHandler
        }

        logger.error(throwable) {"Error occurred"}
        return@CoroutineExceptionHandler

    }


    suspend fun charge() = coroutineScope{
        val invoices = dal.fetchPendingInvoices()


        // Since payment provider is an external service,
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

        // Does payment provider update invoice? If not you could run the code below.
        // I am going to assume it doesn't and add logic to updated invoice
        results.forEach {
            dal.updateStatus(invoice = it, status = InvoiceStatus.PAID)
        }

    }


}