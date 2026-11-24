package com.SLM.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.SLM.MainActivity
import com.SLM.R
import com.SLM.data.local.entity.DebtEntity
import com.SLM.data.local.entity.DebtType
import com.SLM.data.repository.SettingsRepository
import com.SLM.ui.i18n.stringsForLanguage
import com.SLM.utils.CurrencyUtils

class DebtReminderManager(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "debt_reminders_channel"
        const val CHANNEL_NAME = "Debt & Due Date Reminders"
        const val DAILY_CHANNEL_ID = "daily_finance_reminder_channel"
        const val DAILY_CHANNEL_NAME = "Daily Expense Reminders"
        const val DAILY_NOTIFICATION_ID = 9001
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val debtChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about upcoming and overdue debts & receivables"
            }
            val dailyChannel = NotificationChannel(
                DAILY_CHANNEL_ID,
                DAILY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminder to record income and expenses"
            }
            manager.createNotificationChannel(debtChannel)
            manager.createNotificationChannel(dailyChannel)
        }
    }

    fun showDebtReminderNotification(debt: DebtEntity, remainingAmount: Double, statusLabel: String) {
        val settings = SettingsRepository(context)
        val lang = settings.language
        val symbol = settings.currencySymbol

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            debt.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmount = CurrencyUtils.formatAmount(remainingAmount, symbol)
        val title = if (lang == "en") {
            if (debt.debtType == DebtType.RECEIVABLE) "Receivable Alert: ${debt.personName}"
            else "Payable Due Alert: ${debt.personName}"
        } else {
            if (debt.debtType == DebtType.RECEIVABLE) "পাওনা তাগাদা: ${debt.personName}"
            else "দেনা পরিশোধের তাগাদা: ${debt.personName}"
        }

        val text = if (lang == "en") {
            "${debt.personName}: $formattedAmount ($statusLabel)"
        } else {
            "${debt.personName}-এর $formattedAmount ($statusLabel)"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(debt.id.hashCode(), builder.build())
        } catch (_: SecurityException) {
            // Notifications permission not granted
        }
    }

    fun showDailyExpenseReminder(todayExpense: Double, todayTxCount: Int) {
        val settings = SettingsRepository(context)
        val strings = stringsForLanguage(settings.language)
        val symbol = settings.currencySymbol

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            DAILY_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val body = if (todayTxCount > 0) {
            if (settings.language == "en") {
                "Today: $todayTxCount entries (${CurrencyUtils.formatAmount(todayExpense, symbol)} spent). Tap to add more."
            } else {
                "আজ মোট $todayTxCount টি হিসাব লিখেছেন (ব্যয়: ${CurrencyUtils.formatAmount(todayExpense, symbol)})। নতুন এন্ট্রি যোগ করতে ট্যাপ করুন।"
            }
        } else {
            strings.dailyReminderNotifBody
        }

        val builder = NotificationCompat.Builder(context, DAILY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(strings.dailyReminderNotifTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(DAILY_NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Notifications permission not granted
        }
    }
}
