package com.epatay.digitalwallet.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.epatay.digitalwallet.MainActivity
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.TransactionDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class DailyProfitLossWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = TransactionDatabase.getDatabase(context)
            val currencyDao = database.currencyRateDao()
            val goldDao = database.goldRateDao()
            val investmentDao = database.investmentDao()
            val userGoldAssetDao = database.userGoldAssetDao()

            val currencies = currencyDao.getAllRates().associateBy { it.currencyCode }
            val golds = goldDao.getAllOnce().associateBy { it.type }

            val investments = investmentDao.getAllInvestmentsSync()
            val goldAssets = userGoldAssetDao.getAllSync()

            var totalCurrentValue = 0.0
            var totalProfit = 0.0

            for (investment in investments) {
                // Piyasanın sizden alış fiyatı (kullanıcının tahmini satış değeri)
                val currentRate = currencies[investment.assetName]?.forexBuying
                    ?: currencies[investment.assetName]?.forexSelling ?: 0.0
                if (currentRate > 0.0) {
                    val currentVal = investment.amount * currentRate
                    val cost = investment.amount * investment.buyPrice
                    totalCurrentValue += currentVal
                    totalProfit += (currentVal - cost)
                }
            }

            for (asset in goldAssets) {
                // Kuyumcunun/piyasanın alış fiyatı (kullanıcının tahmini satış değeri)
                val currentRate = golds[asset.goldType]?.buyingPrice
                    ?: golds[asset.goldType]?.sellingPrice ?: 0.0
                if (currentRate > 0.0) {
                    val currentVal = asset.quantity * currentRate
                    val cost = asset.totalPurchaseCost
                        ?: (asset.quantity * (asset.purchaseUnitPrice ?: 0.0))
                    totalCurrentValue += currentVal
                    totalProfit += (currentVal - cost)
                }
            }

            if (totalCurrentValue > 0) {
                showNotification(totalCurrentValue, totalProfit)
            }

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun showNotification(totalCurrentValue: Double, totalProfit: Double) {
        val format = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val totalValStr = format.format(totalCurrentValue)
        val profitStr = format.format(Math.abs(totalProfit))
        val profitLabel = when {
            totalProfit > 0.001 -> "Genel Kâr: +$profitStr"
            totalProfit < -0.001 -> "Genel Zarar: -$profitStr"
            else -> "Genel Kâr/Zarar: $profitStr"
        }

        val messageText = "Güncel Portföy Değeri: $totalValStr\n$profitLabel"

        val channelId = "daily_portfolio_channel"
        val notificationId = 1001

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Günlük Portföy Durumu")
            .setContentText("Güncel Portföy Değeri: $totalValStr")
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Portföy Bildirimleri"
            val descriptionText = "Günlük kâr/zarar durumunuzu gösterir"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            notificationManager.createNotificationChannel(channel)
        }

        try {
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
