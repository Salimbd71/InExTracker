package com.SLM.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.SLM.auth.AuthManager
import com.SLM.data.local.AppDatabase
import com.SLM.data.local.entity.DebtType
import com.SLM.data.local.entity.TransactionType
import com.SLM.data.repository.FinanceRepository
import com.SLM.data.repository.SettingsRepository
import com.SLM.ui.i18n.stringsForLanguage
import com.SLM.utils.DateUtils
import com.SLM.widget.InExTrackerWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_CODE_DAILY = 4401

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            cal.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun syncScheduleFromSettings(context: Context) {
        val settings = SettingsRepository(context)
        if (settings.isDailyReminderEnabled) {
            scheduleDailyReminder(context, settings.reminderHour, settings.reminderMinute)
        } else {
            cancelDailyReminder(context)
        }
    }

    suspend fun runDailyAndDebtCheck(context: Context, forceShowDaily: Boolean = false) {
        val settings = SettingsRepository(context)
        val authManager = AuthManager(context)
        val uid = authManager.getCurrentUserId()
        val db = AppDatabase.getDatabase(context)
        val financeRepo = FinanceRepository(db)
        val reminderManager = DebtReminderManager(context)
        val strings = stringsForLanguage(settings.language)

        // 1. Auto-process any due recurring transactions
        runCatching { financeRepo.processRecurringTransactions(uid) }

        // 2. Daily expense reminder
        if (forceShowDaily || settings.isDailyReminderEnabled) {
            val (todayStart, todayEnd) = DateUtils.getDayBounds(0)
            val txs = db.transactionDao().getTransactionsInRange(uid, todayStart, todayEnd).first()
            val todayExpense = txs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            reminderManager.showDailyExpenseReminder(todayExpense, txs.size)
        }

        // 3. Overdue & due-soon debt auto-notifications
        if (settings.isDebtAlertEnabled) {
            val prefs = context.getSharedPreferences("inex_reminder_state", Context.MODE_PRIVATE)
            val todayKey = DateUtils.formatDate(System.currentTimeMillis(), "en")
            val lastNotifiedDay = prefs.getString("last_debt_notified_day", null)
            if (forceShowDaily || lastNotifiedDay != todayKey) {
                val debts = db.debtDao().getAllDebts(uid).first()
                val payments = db.debtPaymentDao().getAllPayments(uid).first()
                val paymentsByDebt = payments.groupBy { it.debtId }
                val now = System.currentTimeMillis()
                val dueThreshold = now + 24L * 60L * 60L * 1000L

                var notifiedAny = false
                for (debt in debts) {
                    val paid = paymentsByDebt[debt.id].orEmpty().sumOf { it.amount }
                    val remaining = (debt.totalAmount - paid).coerceAtLeast(0.0)
                    val due = debt.dueDate
                    if (remaining > 0.0 && due != null && due <= dueThreshold) {
                        val statusLabel = if (due < now) strings.statusOverdue else {
                            if (debt.debtType == DebtType.RECEIVABLE) strings.receivable else strings.payable
                        }
                        reminderManager.showDebtReminderNotification(debt, remaining, statusLabel)
                        notifiedAny = true
                    }
                }
                if (notifiedAny) {
                    prefs.edit().putString("last_debt_notified_day", todayKey).apply()
                }
            }
        }

        InExTrackerWidgetProvider.updateAllWidgets(context)
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DAILY_REMINDER = "com.slm.inextracker.ACTION_DAILY_REMINDER"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.runDailyAndDebtCheck(context.applicationContext, forceShowDaily = false)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
