package io.pleo.antaeus.app

import io.pleo.antaeus.core.services.BillingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.quartz.SchedulerContext
import org.quartz.SchedulerException

class BillingServiceJob: Job {

    override fun execute(context: JobExecutionContext?) {
        GlobalScope.launch (Dispatchers.Default) {
            var schedulerContext: SchedulerContext? = null

            try {
                schedulerContext = context?.scheduler?.context
            } catch (e: SchedulerException) {
                e.printStackTrace()
            }
            val billingService1: BillingService = schedulerContext!!["billingService"] as BillingService
            billingService1.charge()
            print("run job")
        }

    }
}