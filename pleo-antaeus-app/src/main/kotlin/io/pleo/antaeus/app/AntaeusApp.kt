/*
    Defines the main() entry point of the app.
    Configures the database and sets up the REST web service.
 */

@file:JvmName("AntaeusApp")

package io.pleo.antaeus.app

import getPaymentProvider
import io.pleo.antaeus.core.services.BillingService
import io.pleo.antaeus.core.services.CustomerService
import io.pleo.antaeus.core.services.InvoiceService
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.data.CustomerTable
import io.pleo.antaeus.data.InvoiceTable
import io.pleo.antaeus.rest.AntaeusRest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.*
import org.quartz.JobBuilder.*
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.impl.StdSchedulerFactory
import setupInitialData
import java.sql.Connection
import kotlin.math.log

fun main() {
    // The tables to create in the database.
    val tables = arrayOf(InvoiceTable, CustomerTable)

    // Connect to the database and create the needed tables. Drop any existing data.
    val db = Database
        .connect("jdbc:sqlite:/tmp/data.db", "org.sqlite.JDBC")
        .also {
            TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            transaction(it) {
                addLogger(StdOutSqlLogger)
                // Drop all existing tables to ensure a clean slate on each run
                SchemaUtils.drop(*tables)
                // Create all tables
                SchemaUtils.create(*tables)
            }
        }

    // Set up data access layer.
    val dal = AntaeusDal(db = db)

    // Insert example data in the database.
    setupInitialData(dal = dal)

    // Get third parties
    val paymentProvider = getPaymentProvider()

    // Create core services
    val invoiceService = InvoiceService(dal = dal)
    val customerService = CustomerService(dal = dal)

    // This is _your_ billing service to be included where you see fit
    val billingService = BillingService(paymentProvider = paymentProvider, dal = dal)

    // Create REST web service
    AntaeusRest(
        invoiceService = invoiceService,
        customerService = customerService
    ).run()

    // Implement scheduler to run billing service on first of every month
    val schedulerFactory = StdSchedulerFactory()

    val scheduler = schedulerFactory.scheduler

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
            }

        }
    }

    val jobDetail = newJob(BillingServiceJob::class.java)
            .withIdentity("billingService", "group1")
            .build()

    val trigger = TriggerBuilder.newTrigger()
            .withIdentity("billingTrigger", "group1")
            .startNow()
            .withSchedule(simpleSchedule()
                    .withIntervalInSeconds(40)
                    .repeatForever())
            .build()


    scheduler.context["billingService"] = billingService

    scheduler.scheduleJob(jobDetail, trigger)

    scheduler.start()


}


