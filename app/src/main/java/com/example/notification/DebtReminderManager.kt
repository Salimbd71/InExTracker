package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import java.text.NumberFormat
import java.util.Locale

class DebtReminderManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "debt_reminders_channel"
        const val CHANNEL_NAME = "Debt & Due Date Reminders"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about upcoming and overdue debts & receivables"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showDebtReminderNotification(debt: DebtEntity, remainingAmount: Double, statusLabel: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            debt.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = "৳ " + NumberFormat.getNumberInstance(Locale.US).format(remainingAmount)
        val title = if (debt.debtType == DebtType.RECEIVABLE) {
            "পাওনা তাগাদা: ${debt.personName}"
        } else {
            "দেনা পরিশোধের তাগাদা: ${debt.personName}"
        }
        val text = "${debt.personName} এর $formattedAmount ($statusLabel)"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(debt.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            // Notifications permission not granted
        }
    }
}
