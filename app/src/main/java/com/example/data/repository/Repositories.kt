package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.RecurringFrequency
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SyncStatus
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class DashboardSummary(
    val currentBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalReceivable: Double = 0.0, // পাওনা
    val totalPayable: Double = 0.0,    // দেনা
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val thisMonthIncome: Double = 0.0,
    val thisMonthExpense: Double = 0.0,
    val walletBalances: Map<PaymentMethod, Double> = emptyMap()
)

data class DebtWithPayments(
    val debt: DebtEntity,
    val payments: List<DebtPaymentEntity>,
    val totalPaid: Double,
    val remainingAmount: Double,
    val status: DebtStatus
)

data class BudgetProgress(
    val budget: BudgetEntity,
    val spent: Double,
    val remaining: Double,
    val percentageUsed: Float
)

class FinanceRepository(private val db: AppDatabase) {

    fun getTransactions(userId: String): Flow<List<TransactionEntity>> =
        db.transactionDao().getAllTransactions(userId)

    fun getRecentTransactions(userId: String, limit: Int = 10): Flow<List<TransactionEntity>> =
        db.transactionDao().getRecentTransactions(userId, limit)

    fun getCategories(userId: String): Flow<List<CategoryEntity>> =
        db.categoryDao().getAllCategories(userId)

    suspend fun addTransaction(
        userId: String,
        type: TransactionType,
        amount: Double,
        category: String,
        date: Long,
        note: String,
        paymentMethod: PaymentMethod,
        receiptUri: String? = null
    ) = withContext(Dispatchers.IO) {
        val entity = TransactionEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            type = type,
            amount = amount,
            category = category,
            date = date,
            note = note,
            paymentMethod = paymentMethod,
            receiptUri = receiptUri,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        db.transactionDao().insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        db.transactionDao().updateTransaction(
            transaction.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
        )
    }

    suspend fun deleteTransaction(id: String) = withContext(Dispatchers.IO) {
        db.transactionDao().softDeleteTransaction(id)
    }

    suspend fun initializeDefaultCategories(userId: String) = withContext(Dispatchers.IO) {
        val defaultCategories = listOf(
            // Incomes
            CategoryEntity(userId = userId, name = "Salary (বেতন)", type = TransactionType.INCOME, iconName = "payments", colorHex = "#10B981", isDefault = true),
            CategoryEntity(userId = userId, name = "Business (ব্যবসা)", type = TransactionType.INCOME, iconName = "store", colorHex = "#059669", isDefault = true),
            CategoryEntity(userId = userId, name = "Bonus (বোনাস)", type = TransactionType.INCOME, iconName = "redeem", colorHex = "#34D399", isDefault = true),
            CategoryEntity(userId = userId, name = "Gift (উপহার)", type = TransactionType.INCOME, iconName = "card_giftcard", colorHex = "#6EE7B7", isDefault = true),
            CategoryEntity(userId = userId, name = "Other Income (অন্যান্য)", type = TransactionType.INCOME, iconName = "attach_money", colorHex = "#14B8A6", isDefault = true),
            
            // Expenses
            CategoryEntity(userId = userId, name = "Food (খাবার)", type = TransactionType.EXPENSE, iconName = "restaurant", colorHex = "#EF4444", isDefault = true),
            CategoryEntity(userId = userId, name = "Shopping (কেনাকাটা)", type = TransactionType.EXPENSE, iconName = "shopping_bag", colorHex = "#F97316", isDefault = true),
            CategoryEntity(userId = userId, name = "Transport (যাতায়াত)", type = TransactionType.EXPENSE, iconName = "directions_car", colorHex = "#F59E0B", isDefault = true),
            CategoryEntity(userId = userId, name = "Medical (চিকিৎসা)", type = TransactionType.EXPENSE, iconName = "medical_services", colorHex = "#EC4899", isDefault = true),
            CategoryEntity(userId = userId, name = "Education (শিক্ষা)", type = TransactionType.EXPENSE, iconName = "school", colorHex = "#8B5CF6", isDefault = true),
            CategoryEntity(userId = userId, name = "Bills & Utilities (বিল)", type = TransactionType.EXPENSE, iconName = "receipt_long", colorHex = "#6366F1", isDefault = true),
            CategoryEntity(userId = userId, name = "Rent (ভাড়া)", type = TransactionType.EXPENSE, iconName = "home", colorHex = "#3B82F6", isDefault = true),
            CategoryEntity(userId = userId, name = "Entertainment (বিনোদন)", type = TransactionType.EXPENSE, iconName = "movie", colorHex = "#A855F7", isDefault = true),
            CategoryEntity(userId = userId, name = "Family (পরিবার)", type = TransactionType.EXPENSE, iconName = "family_restroom", colorHex = "#D946EF", isDefault = true),
            CategoryEntity(userId = userId, name = "Other Expense (অন্যান্য)", type = TransactionType.EXPENSE, iconName = "more_horiz", colorHex = "#64748B", isDefault = true)
        )
        db.categoryDao().insertCategories(defaultCategories)
    }

    suspend fun addCustomCategory(userId: String, name: String, type: TransactionType, colorHex: String) = withContext(Dispatchers.IO) {
        val category = CategoryEntity(
            userId = userId,
            name = name,
            type = type,
            colorHex = colorHex,
            isDefault = false
        )
        db.categoryDao().insertCategory(category)
    }

    // Process recurring transactions
    suspend fun processRecurringTransactions(userId: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val recurringList = db.recurringDao().getPendingSyncRecurring().filter { it.userId == userId && it.isActive && it.nextDueDate <= now }
        for (item in recurringList) {
            // Insert regular transaction
            addTransaction(
                userId = userId,
                type = item.type,
                amount = item.amount,
                category = item.category,
                date = item.nextDueDate,
                note = "স্বয়ংক্রিয় লেনদেন: ${item.title}",
                paymentMethod = item.paymentMethod
            )
            // Advance nextDueDate
            val nextDate = calculateNextDueDate(item.nextDueDate, item.frequency)
            db.recurringDao().updateRecurring(
                item.copy(
                    nextDueDate = nextDate,
                    lastGeneratedDate = now,
                    updatedAt = now,
                    syncStatus = SyncStatus.PENDING_UPDATE
                )
            )
        }
    }

    private fun calculateNextDueDate(current: Long, freq: RecurringFrequency): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        when (freq) {
            RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RecurringFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> cal.add(Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> cal.add(Calendar.YEAR, 1)
        }
        return cal.timeInMillis
    }
}

class DebtRepository(private val db: AppDatabase) {

    fun getDebts(userId: String): Flow<List<DebtEntity>> =
        db.debtDao().getAllDebts(userId)

    fun getPayments(userId: String): Flow<List<DebtPaymentEntity>> =
        db.debtPaymentDao().getAllPayments(userId)

    fun getDebtWithPayments(debtId: String): Flow<DebtWithPayments?> {
        val debtFlow = db.debtDao().getAllDebts("") // We will combine or get by id
        return combine(db.debtDao().getAllDebts(""), db.debtPaymentDao().getPaymentsForDebt(debtId)) { _, payments ->
            val debt = db.debtDao().getDebtById(debtId) ?: return@combine null
            val totalPaid = payments.sumOf { it.amount }
            val remaining = (debt.totalAmount - totalPaid).coerceAtLeast(0.0)
            val now = System.currentTimeMillis()
            val isOverdue = debt.dueDate != null && debt.dueDate < now && remaining > 0

            val status = when {
                remaining <= 0.0 -> DebtStatus.PAID
                isOverdue -> DebtStatus.OVERDUE
                totalPaid > 0.0 -> DebtStatus.PARTIALLY_PAID
                else -> DebtStatus.PENDING
            }

            DebtWithPayments(
                debt = debt,
                payments = payments,
                totalPaid = totalPaid,
                remainingAmount = remaining,
                status = status
            )
        }
    }

    suspend fun addDebt(
        userId: String,
        personName: String,
        phoneNumber: String,
        debtType: DebtType,
        totalAmount: Double,
        date: Long,
        dueDate: Long?,
        note: String
    ) = withContext(Dispatchers.IO) {
        val entity = DebtEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            personName = personName,
            phoneNumber = phoneNumber,
            debtType = debtType,
            totalAmount = totalAmount,
            date = date,
            dueDate = dueDate,
            note = note,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        db.debtDao().insertDebt(entity)
    }

    suspend fun updateDebt(debt: DebtEntity) = withContext(Dispatchers.IO) {
        db.debtDao().updateDebt(
            debt.copy(
                updatedAt = System.currentTimeMillis(),
                syncStatus = SyncStatus.PENDING_UPDATE
            )
        )
    }

    suspend fun deleteDebt(id: String) = withContext(Dispatchers.IO) {
        db.debtDao().softDeleteDebt(id)
    }

    suspend fun addPayment(
        debtId: String,
        userId: String,
        amount: Double,
        date: Long,
        note: String,
        paymentMethod: PaymentMethod
    ) = withContext(Dispatchers.IO) {
        val payment = DebtPaymentEntity(
            id = UUID.randomUUID().toString(),
            debtId = debtId,
            userId = userId,
            amount = amount,
            date = date,
            note = note,
            paymentMethod = paymentMethod,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        db.debtPaymentDao().insertPayment(payment)
    }

    suspend fun deletePayment(paymentId: String) = withContext(Dispatchers.IO) {
        db.debtPaymentDao().softDeletePayment(paymentId)
    }
}

class BudgetRepository(private val db: AppDatabase) {
    fun getBudgets(userId: String, monthYear: String): Flow<List<BudgetEntity>> =
        db.budgetDao().getBudgetsForMonth(userId, monthYear)

    suspend fun setBudget(userId: String, category: String, monthlyLimit: Double, monthYear: String) = withContext(Dispatchers.IO) {
        val budget = BudgetEntity(
            userId = userId,
            category = category,
            monthlyLimit = monthlyLimit,
            monthYear = monthYear,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        db.budgetDao().insertBudget(budget)
    }

    suspend fun deleteBudget(id: String) = withContext(Dispatchers.IO) {
        db.budgetDao().softDeleteBudget(id)
    }
}

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("hisab_khata_settings", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("is_dark_mode", false)
        set(value) = prefs.edit().putBoolean("is_dark_mode", value).apply()

    var language: String // "bn" or "en"
        get() = prefs.getString("app_language", "bn") ?: "bn"
        set(value) = prefs.edit().putString("app_language", value).apply()

    var currencySymbol: String
        get() = prefs.getString("currency_symbol", "৳") ?: "৳"
        set(value) = prefs.edit().putString("currency_symbol", value).apply()

    var pinCode: String?
        get() = prefs.getString("user_pin_code", null)
        set(value) = prefs.edit().putString("user_pin_code", value).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean("is_pin_enabled", false)
        set(value) = prefs.edit().putBoolean("is_pin_enabled", value).apply()

    var isAdFree: Boolean
        get() = prefs.getBoolean("is_ad_free", false)
        set(value) = prefs.edit().putBoolean("is_ad_free", value).apply()
}
