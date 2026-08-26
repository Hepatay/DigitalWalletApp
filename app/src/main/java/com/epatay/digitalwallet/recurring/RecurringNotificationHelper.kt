package com.epatay.digitalwallet.recurring

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.epatay.digitalwallet.MainActivity
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurringNotificationHelper {

    private const val CHANNEL_ID =
        "recurring_transaction_reminders"
    private const val CHANNEL_NAME =
        "Düzenli ödeme ve gelir hatırlatmaları"
    private const val NOTIFICATION_ID_BASE = 20_000

    fun createChannel(
        context: Context
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description =
                "Yaklaşan düzenli ödeme ve gelirleri hatırlatır."
        }

        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun canPostNotifications(
        context: Context
    ): Boolean {
        val runtimePermissionGranted =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        return runtimePermissionGranted &&
            NotificationManagerCompat.from(context)
                .areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    fun showDueReminder(
        context: Context,
        recurringTransaction: RecurringTransaction,
        dueDate: Calendar
    ): Boolean {
        if (!canPostNotifications(context)) {
            return false
        }

        createChannel(context)

        val pendingIntent = PendingIntent.getActivity(
            context,
            recurringTransaction.uuid.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val reminderKind =
            if (recurringTransaction.type == TransactionType.EXPENSE) {
                "Yaklaşan ödeme"
            } else {
                "Yaklaşan gelir"
            }

        val dueText =
            SimpleDateFormat(
                "d MMMM yyyy",
                Locale.forLanguageTag("tr-TR")
            ).format(dueDate.time)

        val amountText =
            NumberFormat.getCurrencyInstance(
                Locale.forLanguageTag("tr-TR")
            ).format(recurringTransaction.amount)

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_bill)
            .setContentTitle(reminderKind)
            .setContentText(
                "${recurringTransaction.title} • $dueText • $amountText"
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID_BASE + recurringTransaction.uuid.hashCode(),
            notification
        )

        return true
    }

    fun cancelReminder(
        context: Context,
        recurringTransactionId: String
    ) {
        NotificationManagerCompat.from(context).cancel(
            NOTIFICATION_ID_BASE + recurringTransactionId.hashCode()
        )
    }
}
