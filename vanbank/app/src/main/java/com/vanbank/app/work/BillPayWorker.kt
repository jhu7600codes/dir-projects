package com.vanbank.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vanbank.app.VanBankApplication
import com.vanbank.app.data.repository.BillPayOutcome

/**
 * Sweeps every user's due bills on a schedule and auto-deducts them --
 * "recurring fake bills that auto-deduct on a schedule (WorkManager for
 * scheduling)". WorkManager's floor for periodic work is 15 minutes, which
 * is plenty for a bill-pay simulator: due dates are days/weeks/months apart,
 * so a request is never more than 15 minutes late.
 */
class BillPayWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as VanBankApplication
        val billRepository = app.container.billRepository
        val notificationHelper = app.container.notificationHelper

        val dueBills = billRepository.getDueBills()
        for (bill in dueBills) {
            when (val outcome = billRepository.processBill(bill)) {
                is BillPayOutcome.Paid ->
                    notificationHelper.showBillPayResult(outcome.bill.name, outcome.bill.amountMinor, success = true)
                is BillPayOutcome.Failed ->
                    notificationHelper.showBillPayResult(outcome.bill.name, outcome.bill.amountMinor, success = false)
            }
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "vanbank_bill_pay_sweep"
        const val REPEAT_INTERVAL = 15L // minutes -- WorkManager's minimum periodic interval
    }
}
