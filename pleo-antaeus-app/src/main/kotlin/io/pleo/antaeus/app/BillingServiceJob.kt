package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.BillingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.SchedulerContext
import org.quartz.SchedulerException
import kotlin.coroutines.CoroutineContext

class BillingServiceJob : Job, CoroutineScope {

    private val job: kotlinx.coroutines.Job = kotlinx.coroutines.Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun execute(context: JobExecutionContext?) {
        launch (Dispatchers.Default) {
            var schedulerContext: SchedulerContext? = null

            try {
                schedulerContext = context?.scheduler?.context
            } catch (e: SchedulerException) {
                e.printStackTrace()
            }
            val billingService1: BillingService = schedulerContext!!["billingService"] as BillingService
            billingService1.charge()
        }

    }
}