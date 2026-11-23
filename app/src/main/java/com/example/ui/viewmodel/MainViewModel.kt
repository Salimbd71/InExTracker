package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.auth.AuthManager
import com.example.auth.AuthState
import com.example.auth.UserProfile
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
import com.example.data.local.entity.SyncMetaEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.data.remote.FirebaseRealtimeDbService
import com.example.data.repository.BudgetProgress
import com.example.data.repository.BudgetRepository
import com.example.data.repository.DashboardSummary
import com.example.data.repository.DebtRepository
import com.example.data.repository.DebtWithPayments
import com.example.data.repository.FinanceRepository
import com.example.data.repository.SettingsRepository
import com.example.notification.DebtReminderManager
import com.example.sync.SyncManager
import com.example.sync.SyncUiState
import com.example.utils.DateUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class DateFilterType {
    ALL,
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH,
    LAST_MONTH
}

data class TxFilterState(
    val query: String = "",
    val type: TransactionType? = null,
    val category: String? = null,
    val paymentMethod: PaymentMethod? = null,
    val dateRange: DateFilterType = DateFilterType.ALL
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val realtimeDbService = FirebaseRealtimeDbService()
    val authManager = AuthManager(application)
    val syncManager = SyncManager(application, db, realtimeDbService, viewModelScope)
    val reminderManager = DebtReminderManager(application)
    val settingsRepo = SettingsRepository(application)

    val financeRepo = FinanceRepository(db)
    val debtRepo = DebtRepository(db)
    val budgetRepo = BudgetRepository(db)

    val authState: StateFlow<AuthState> = authManager.authState

    private val _currentUserId = MutableStateFlow(authManager.getCurrentUserId())
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Preferences State
    private val _isDarkMode = MutableStateFlow(settingsRepo.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _language = MutableStateFlow(settingsRepo.language)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _currencySymbol = MutableStateFlow(settingsRepo.currencySymbol)
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _isPinLocked = MutableStateFlow(settingsRepo.isPinLockEnabled && !settingsRepo.pinCode.isNullOrBlank())
    val isPinLocked: StateFlow<Boolean> = _isPinLocked.asStateFlow()

    private val _isPinVerified = MutableStateFlow(false)
    val isPinVerified: StateFlow<Boolean> = _isPinVerified.asStateFlow()

    // Search and Filters
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow<TransactionType?>(null)
    val filterCategory = MutableStateFlow<String?>(null)
    val filterPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val filterDateRange = MutableStateFlow(DateFilterType.ALL)

    // Debt Search & Filter
    val debtSearchQuery = MutableStateFlow("")
    val debtFilterType = MutableStateFlow<DebtType?>(null)
    val debtFilterStatus = MutableStateFlow<DebtStatus?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val allTransactions: StateFlow<List<TransactionEntity>> = _currentUserId.flatMapLatest { uid ->
        financeRepo.getTransactions(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allCategories: StateFlow<List<CategoryEntity>> = _currentUserId.flatMapLatest { uid ->
        financeRepo.getCategories(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDebts: StateFlow<List<DebtEntity>> = _currentUserId.flatMapLatest { uid ->
        debtRepo.getDebts(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allDebtPayments: StateFlow<List<DebtPaymentEntity>> = _currentUserId.flatMapLatest { uid ->
        debtRepo.getPayments(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allBudgets: StateFlow<List<BudgetEntity>> = _currentUserId.flatMapLatest { uid ->
        budgetRepo.getBudgets(uid, DateUtils.getMonthYearKey())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRecurring: StateFlow<List<RecurringTransactionEntity>> = _currentUserId.flatMapLatest { uid ->
        db.recurringDao().getAllRecurring(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val syncMeta: StateFlow<SyncMetaEntity?> = _currentUserId.flatMapLatest { uid ->
        db.syncMetaDao().getSyncMeta(uid)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Debts with calculated payments
    val debtsWithPayments: StateFlow<List<DebtWithPayments>> = combine(allDebts, allDebtPayments) { debts, payments ->
        val now = System.currentTimeMillis()
        debts.map { debt ->
            val debtPmts = payments.filter { it.debtId == debt.id }
            val paid = debtPmts.sumOf { it.amount }
            val remaining = (debt.totalAmount - paid).coerceAtLeast(0.0)
            val isOverdue = debt.dueDate != null && debt.dueDate < now && remaining > 0

            val status = when {
                remaining <= 0.0 -> DebtStatus.PAID
                isOverdue -> DebtStatus.OVERDUE
                paid > 0.0 -> DebtStatus.PARTIALLY_PAID
                else -> DebtStatus.PENDING
            }

            DebtWithPayments(
                debt = debt,
                payments = debtPmts,
                totalPaid = paid,
                remainingAmount = remaining,
                status = status
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard summary calculations
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allTransactions,
        debtsWithPayments
    ) { transactions, debtList ->
        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayIncome = 0.0
        var todayExpense = 0.0
        var monthIncome = 0.0
        var monthExpense = 0.0
        val walletMap = mutableMapOf<PaymentMethod, Double>()

        for (t in transactions) {
            val amt = t.amount
            if (t.type == TransactionType.INCOME) {
                totalIncome += amt
                walletMap[t.paymentMethod] = (walletMap[t.paymentMethod] ?: 0.0) + amt
                if (DateUtils.isToday(t.date)) todayIncome += amt
                if (DateUtils.isThisMonth(t.date)) monthIncome += amt
            } else {
                totalExpense += amt
                walletMap[t.paymentMethod] = (walletMap[t.paymentMethod] ?: 0.0) - amt
                if (DateUtils.isToday(t.date)) todayExpense += amt
                if (DateUtils.isThisMonth(t.date)) monthExpense += amt
            }
        }

        val totalReceivable = debtList.filter { it.debt.debtType == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
        val totalPayable = debtList.filter { it.debt.debtType == DebtType.PAYABLE }.sumOf { it.remainingAmount }

        DashboardSummary(
            currentBalance = totalIncome - totalExpense,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            totalReceivable = totalReceivable,
            totalPayable = totalPayable,
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            thisMonthIncome = monthIncome,
            thisMonthExpense = monthExpense,
            walletBalances = walletMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private val txFilterState = combine(
        searchQuery,
        filterType,
        filterCategory,
        filterPaymentMethod,
        filterDateRange
    ) { query, type, cat, pm, dateRange ->
        TxFilterState(query, type, cat, pm, dateRange)
    }

    // Filtered transactions
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        txFilterState
    ) { txs, filter ->
        txs.filter { tx ->
            val matchesQuery = filter.query.isBlank() ||
                    tx.category.contains(filter.query, ignoreCase = true) ||
                    tx.note.contains(filter.query, ignoreCase = true) ||
                    tx.amount.toString().contains(filter.query)
            val matchesType = filter.type == null || tx.type == filter.type
            val matchesCat = filter.category == null || tx.category == filter.category
            val matchesPm = filter.paymentMethod == null || tx.paymentMethod == filter.paymentMethod
            val matchesDate = when (filter.dateRange) {
                DateFilterType.ALL -> true
                DateFilterType.TODAY -> DateUtils.isToday(tx.date)
                DateFilterType.YESTERDAY -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
                }
                DateFilterType.THIS_WEEK -> {
                    val cal = Calendar.getInstance()
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) && cal.get(Calendar.WEEK_OF_YEAR) == txCal.get(Calendar.WEEK_OF_YEAR)
                }
                DateFilterType.THIS_MONTH -> DateUtils.isThisMonth(tx.date)
                DateFilterType.LAST_MONTH -> {
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) && cal.get(Calendar.MONTH) == txCal.get(Calendar.MONTH)
                }
            }
            matchesQuery && matchesType && matchesCat && matchesPm && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Debts
    val filteredDebts: StateFlow<List<DebtWithPayments>> = combine(
        debtsWithPayments,
        debtSearchQuery,
        debtFilterType,
        debtFilterStatus
    ) { debts, query, type, status ->
        debts.filter { d ->
            val matchesQuery = query.isBlank() ||
                    d.debt.personName.contains(query, ignoreCase = true) ||
                    d.debt.phoneNumber.contains(query) ||
                    d.debt.note.contains(query, ignoreCase = true) ||
                    d.debt.totalAmount.toString().contains(query)
            val matchesType = type == null || d.debt.debtType == type
            val matchesStatus = status == null || d.status == status
            matchesQuery && matchesType && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget progress
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(allBudgets, allTransactions) { budgets, txs ->
        budgets.map { b ->
            val spent = txs.filter {
                it.type == TransactionType.EXPENSE &&
                it.category.equals(b.category, ignoreCase = true) &&
                DateUtils.isThisMonth(it.date)
            }.sumOf { it.amount }
            val remaining = (b.monthlyLimit - spent).coerceAtLeast(0.0)
            val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            BudgetProgress(budget = b, spent = spent, remaining = remaining, percentageUsed = pct)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        AdManager.initialize(application)
        AdManager.setAdsEnabled(!settingsRepo.isAdFree)

        viewModelScope.launch {
            authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _currentUserId.value = state.user.uid
                        financeRepo.initializeDefaultCategories(state.user.uid)
                        financeRepo.processRecurringTransactions(state.user.uid)
                        syncManager.syncAll(state.user.uid)
                    }
                    is AuthState.Unauthenticated -> {
                        // Keep current guest or default
                    }
                    else -> {}
                }
            }
        }
    }

    fun continueAsGuest() {
        authManager.continueAsGuest()
    }

    suspend fun signInWithGoogle(activityContext: android.content.Context = getApplication()): Result<UserProfile> {
        val result = authManager.signInWithGoogle(activityContext = activityContext)
        if (result.isSuccess) {
            val user = result.getOrNull()
            if (user != null) {
                _currentUserId.value = user.uid
                syncManager.syncAll(user.uid)
            }
        }
        return result
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
            _currentUserId.value = authManager.getCurrentUserId()
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncManager.syncAll(_currentUserId.value)
        }
    }

    // Transactions operations
    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        date: Long,
        note: String,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            financeRepo.addTransaction(
                userId = _currentUserId.value,
                type = type,
                amount = amount,
                category = category,
                date = date,
                note = note,
                paymentMethod = paymentMethod
            )
            triggerSync()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            financeRepo.updateTransaction(transaction)
            triggerSync()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            financeRepo.deleteTransaction(id)
            triggerSync()
        }
    }

    fun addCustomCategory(name: String, type: TransactionType, colorHex: String) {
        viewModelScope.launch {
            financeRepo.addCustomCategory(_currentUserId.value, name, type, colorHex)
        }
    }

    // Debt operations
    fun addDebt(
        personName: String,
        phoneNumber: String,
        debtType: DebtType,
        totalAmount: Double,
        date: Long,
        dueDate: Long?,
        note: String
    ) {
        viewModelScope.launch {
            debtRepo.addDebt(
                userId = _currentUserId.value,
                personName = personName,
                phoneNumber = phoneNumber,
                debtType = debtType,
                totalAmount = totalAmount,
                date = date,
                dueDate = dueDate,
                note = note
            )
            triggerSync()
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            debtRepo.updateDebt(debt)
            triggerSync()
        }
    }

    fun deleteDebt(id: String) {
        viewModelScope.launch {
            debtRepo.deleteDebt(id)
            triggerSync()
        }
    }

    fun addDebtPayment(
        debtId: String,
        amount: Double,
        date: Long,
        note: String,
        paymentMethod: PaymentMethod
    ) {
        viewModelScope.launch {
            debtRepo.addPayment(
                debtId = debtId,
                userId = _currentUserId.value,
                amount = amount,
                date = date,
                note = note,
                paymentMethod = paymentMethod
            )
            triggerSync()
        }
    }

    fun deleteDebtPayment(paymentId: String) {
        viewModelScope.launch {
            debtRepo.deletePayment(paymentId)
            triggerSync()
        }
    }

    // Budget operations
    fun setBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            budgetRepo.setBudget(
                userId = _currentUserId.value,
                category = category,
                monthlyLimit = monthlyLimit,
                monthYear = DateUtils.getMonthYearKey()
            )
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            budgetRepo.deleteBudget(id)
        }
    }

    // Recurring operations
    fun addRecurringTransaction(
        title: String,
        type: TransactionType,
        amount: Double,
        category: String,
        paymentMethod: PaymentMethod,
        frequency: RecurringFrequency,
        nextDueDate: Long
    ) {
        viewModelScope.launch {
            val entity = RecurringTransactionEntity(
                userId = _currentUserId.value,
                title = title,
                type = type,
                amount = amount,
                category = category,
                paymentMethod = paymentMethod,
                frequency = frequency,
                nextDueDate = nextDueDate
            )
            db.recurringDao().insertRecurring(entity)
        }
    }

    fun toggleRecurring(id: String, isActive: Boolean) {
        viewModelScope.launch {
            val item = db.recurringDao().getAllRecurring(_currentUserId.value)
            // We can toggle
        }
    }

    fun deleteRecurring(id: String) {
        viewModelScope.launch {
            db.recurringDao().softDeleteRecurring(id)
        }
    }

    // Settings
    fun toggleDarkMode(enabled: Boolean) {
        settingsRepo.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    fun setLanguage(lang: String) {
        settingsRepo.language = lang
        _language.value = lang
    }

    fun setCurrency(symbol: String) {
        settingsRepo.currencySymbol = symbol
        _currencySymbol.value = symbol
    }

    fun setPinCode(pin: String?) {
        settingsRepo.pinCode = pin
        settingsRepo.isPinLockEnabled = !pin.isNullOrBlank()
        _isPinLocked.value = !pin.isNullOrBlank()
    }

    fun verifyPin(pin: String): Boolean {
        val valid = settingsRepo.pinCode == pin
        if (valid) {
            _isPinVerified.value = true
        }
        return valid
    }

    fun setAdFree(adFree: Boolean) {
        settingsRepo.isAdFree = adFree
        AdManager.setAdsEnabled(!adFree)
    }
}
