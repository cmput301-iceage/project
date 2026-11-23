package com.example.campsitelottery

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * Background worker that checks for expired lotteries and executes drawing logic.
 */
class LotteryDrawWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Running periodic check for expired lotteries...")
        return suspendCancellableCoroutine { continuation ->
            LotteryDrawManager.checkAndDrawAllExpiredLotteries {
                if (continuation.isActive) {
                    continuation.resume(Result.success())
                }
            }
        }
    }

    companion object {
        private const val TAG = "LotteryDrawWorker"
        private const val WORK_NAME = "LotteryDrawWorker"

        /**
         * Schedules periodic background execution to draw expired lotteries.
         */
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<LotteryDrawWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
