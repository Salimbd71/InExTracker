package com.SLM.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.SLM.MainActivity
import com.SLM.R
import com.SLM.auth.AuthManager
import com.SLM.data.local.AppDatabase
import com.SLM.data.local.entity.TransactionType
import com.SLM.data.repository.SettingsRepository
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InExTrackerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.slm.inextracker.ACTION_REFRESH_WIDGET"
        const val EXTRA_WIDGET_ACTION = "extra_widget_action"
        const val WIDGET_ACTION_ADD_INCOME = "ADD_INCOME"
        const val WIDGET_ACTION_ADD_EXPENSE = "ADD_EXPENSE"

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, InExTrackerWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                for (id in ids) {
                    updateSingleWidget(appContext, manager, id)
                }
            }
        }

        private fun createRotatedSyncBitmap(context: Context, angleDegrees: Float): Bitmap? {
            val drawable = runCatching {
                ContextCompat.getDrawable(context, R.drawable.ic_widget_sync)
            }.getOrNull() ?: return null
            val sizePx = 72
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.save()
            canvas.rotate(angleDegrees, sizePx / 2f, sizePx / 2f)
            val pad = 10
            drawable.setBounds(pad, pad, sizePx - pad, sizePx - pad)
            drawable.draw(canvas)
            canvas.restore()
            return bitmap
        }

        suspend fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val settings = SettingsRepository(context)
            val lang = settings.language
            val symbol = settings.currencySymbol
            val uid = AuthManager(context).getCurrentUserId()
            val db = AppDatabase.getDatabase(context)

            val transactions = runCatching {
                db.transactionDao().getAllTransactions(uid).first()
            }.getOrDefault(emptyList())

            val (todayStart, todayEnd) = DateUtils.getDayBounds(0)
            var totalIncome = 0.0
            var totalExpense = 0.0
            var todayIncome = 0.0
            var todayExpense = 0.0

            for (tx in transactions) {
                val isToday = tx.date in todayStart..todayEnd
                if (tx.type == TransactionType.INCOME) {
                    totalIncome += tx.amount
                    if (isToday) todayIncome += tx.amount
                } else {
                    totalExpense += tx.amount
                    if (isToday) todayExpense += tx.amount
                }
            }
            val balance = totalIncome - totalExpense

            val views = RemoteViews(context.packageName, R.layout.widget_inex_tracker)
            val isEn = lang == "en"
            views.setTextViewText(
                R.id.widget_app_title,
                if (isEn) "InEx Tracker • Balance" else "InEx Tracker • বর্তমান ব্যালেন্স"
            )
            views.setTextViewText(
                R.id.widget_balance,
                CurrencyUtils.formatAmount(balance, symbol)
            )
            views.setTextViewText(
                R.id.widget_income_label,
                if (isEn) "Today's Income" else "আজকের আয়"
            )
            views.setTextViewText(
                R.id.widget_expense_label,
                if (isEn) "Today's Expense" else "আজকের ব্যয়"
            )
            views.setTextViewText(
                R.id.widget_today_income,
                "+ ${CurrencyUtils.formatCompact(todayIncome, symbol)}"
            )
            views.setTextViewText(
                R.id.widget_today_expense,
                "- ${CurrencyUtils.formatCompact(todayExpense, symbol)}"
            )
            views.setTextViewText(
                R.id.widget_btn_add_income,
                if (isEn) "+ Income" else "+ আয় যোগ"
            )
            views.setTextViewText(
                R.id.widget_btn_add_expense,
                if (isEn) "- Expense" else "- ব্যয় যোগ"
            )
            views.setImageViewResource(R.id.widget_refresh_btn, R.drawable.ic_widget_sync)

            // Open App on card click
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                1000 + appWidgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            // Quick Add Income
            val incomeIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, WIDGET_ACTION_ADD_INCOME)
            }
            val incomePending = PendingIntent.getActivity(
                context,
                2000 + appWidgetId,
                incomeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add_income, incomePending)

            // Quick Add Expense
            val expenseIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_WIDGET_ACTION, WIDGET_ACTION_ADD_EXPENSE)
            }
            val expensePending = PendingIntent.getActivity(
                context,
                3000 + appWidgetId,
                expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add_expense, expensePending)

            // Refresh Widget (rotates sync button + updates DB balances)
            val refreshIntent = Intent(context, InExTrackerWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                4000 + appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    updateSingleWidget(context.applicationContext, appWidgetManager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val ids = manager.getAppWidgetIds(
                ComponentName(appContext, InExTrackerWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val angles = listOf(45f, 90f, 135f, 180f, 225f, 270f, 315f, 360f)
                    for (angle in angles) {
                        val bmp = createRotatedSyncBitmap(appContext, angle)
                        if (bmp != null) {
                            val spinViews = RemoteViews(appContext.packageName, R.layout.widget_inex_tracker)
                            spinViews.setImageViewBitmap(R.id.widget_refresh_btn, bmp)
                            manager.partiallyUpdateAppWidget(ids, spinViews)
                        }
                        delay(50)
                    }
                    for (id in ids) {
                        updateSingleWidget(appContext, manager, id)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
