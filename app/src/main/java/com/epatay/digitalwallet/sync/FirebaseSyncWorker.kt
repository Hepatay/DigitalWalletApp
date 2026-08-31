package com.epatay.digitalwallet.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth

/**
 * Arka plan Uçtan Uca Şifreli (Zero-Knowledge) Firebase Senkronizasyon İşçisi.
 */
class FirebaseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success() // Misafir modunda senkronizasyon yapılmaz
        val uid = user.uid

        Log.d(TAG, "Starting Zero-Knowledge sync worker for user $uid")

        return try {
            val syncManager = FirebaseSyncManager(applicationContext)
            syncManager.pushDataToFirebase(uid)
            Log.d(TAG, "Zero-Knowledge sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync in worker", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "FirebaseSyncWorker"

        fun trigger(context: Context) {
            val req = androidx.work.OneTimeWorkRequestBuilder<FirebaseSyncWorker>().build()
            androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                "FirebaseSyncWorker",
                androidx.work.ExistingWorkPolicy.REPLACE,
                req
            )
        }
    }
}

