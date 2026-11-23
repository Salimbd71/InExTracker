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
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    // Debts with calculated payments (O(N + M) grouping on Default dispatcher)
    val debtsWithPayments: StateFlow<List<DebtWithPayments>> = combine(allDebts, allDebtPayments) { debts, payments ->
        val now = System.currentTimeMillis()
        val paymentsByDebt = payments.groupBy { it.debtId }
        debts.map { debt ->
            val debtPmts = paymentsByDebt[debt.id].orEmpty()
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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard summary calculations (bounds computed once per emission on Default dispatcher)
    val dashboardSummary: StateFlow<DashboardSummary> = combine(
        allTransactions,
        debtsWithPayments
    ) { transactions, debtList ->
        val (todayStart, todayEnd) = DateUtils.getDayBounds(0)
        val (monthStart, monthEnd) = DateUtils.getMonthBounds(0)

        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayIncome = 0.0
        var todayExpense = 0.0
        var monthIncome = 0.0
        var monthExpense = 0.0
        val walletMap = mutableMapOf<PaymentMethod, Double>()

        for (t in transactions) {
            val amt = t.amount
            val isToday = t.date in todayStart..todayEnd
            val isMonth = t.date in monthStart..monthEnd
            if (t.type == TransactionType.INCOME) {
                totalIncome += amt
                walletMap[t.paymentMethod] = (walletMap[t.paymentMethod] ?: 0.0) + amt
                if (isToday) todayIncome += amt
                if (isMonth) monthIncome += amt
            } else {
                totalExpense += amt
                walletMap[t.paymentMethod] = (walletMap[t.paymentMethod] ?: 0.0) - amt
                if (isToday) todayExpense += amt
                if (isMonth) monthExpense += amt
            }
        }

        var totalReceivable = 0.0
        var totalPayable = 0.0
        for (d in debtList) {
            if (d.debt.debtType == DebtType.RECEIVABLE) totalReceivable += d.remainingAmount
            else totalPayable += d.remainingAmount
        }

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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardSummary())

    private val txFilterState = combine(
        searchQuery,
        filterType,
        filterCategory,
        filterPaymentMethod,
        filterDateRange
    ) { query, type, cat, pm, dateRange ->
        TxFilterState(query, type, cat, pm, dateRange)
    }

    // Filtered transactions (bounds computed once outside loop)
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        txFilterState
    ) { txs, filter ->
        val dateBounds: Pair<Long, Long>? = when (filter.dateRange) {
            DateFilterType.ALL -> null
            DateFilterType.TODAY -> DateUtils.getDayBounds(0)
            DateFilterType.YESTERDAY -> DateUtils.getDayBounds(-1)
            DateFilterType.THIS_WEEK -> DateUtils.getWeekBounds()
            DateFilterType.THIS_MONTH -> DateUtils.getMonthBounds(0)
            DateFilterType.LAST_MONTH -> DateUtils.getMonthBounds(-1)
        }
        val query = filter.query.trim()
        txs.filter { tx ->
            val matchesQuery = query.isEmpty() ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.note.contains(query, ignoreCase = true) ||
                    tx.amount.toString().contains(query)
            val matchesType = filter.type == null || tx.type == filter.type
            val matchesCat = filter.category == null || tx.category == filter.category
            val matchesPm = filter.paymentMethod == null || tx.paymentMethod == filter.paymentMethod
            val matchesDate = dateBounds == null || tx.date in dateBounds.first..dateBounds.second
            matchesQuery && matchesType && matchesCat && matchesPm && matchesDate
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Debts
    val filteredDebts: StateFlow<List<DebtWithPayments>> = combine(
        debtsWithPayments,
        debtSearchQuery,
        debtFilterType,
        debtFilterStatus
    ) { debts, rawQuery, type, status ->
        val query = rawQuery.trim()
        debts.filter { d ->
            val matchesQuery = query.isEmpty() ||
                    d.debt.personName.contains(query, ignoreCase = true) ||
                    d.debt.phoneNumber.contains(query) ||
                    d.debt.note.contains(query, ignoreCase = true) ||
                    d.debt.totalAmount.toString().contains(query)
            val matchesType = type == null || d.debt.debtType == type
            val matchesStatus = status == null || d.status == status
            matchesQuery && matchesType && matchesStatus
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget progress (pre-grouped O(N + M))
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(allBudgets, allTransactions) { budgets, txs ->
        val (monthStart, monthEnd) = DateUtils.getMonthBounds(0)
        val spentByCategory = mutableMapOf<String, Double>()
        for (tx in txs) {
            if (tx.type == TransactionType.EXPENSE && tx.date in monthStart..monthEnd) {
                val key = tx.category.lowercase()
                spentByCategory[key] = (spentByCategory[key] ?: 0.0) + tx.amount
            }
        }
        budgets.map { b ->
            val spent = spentByCategory[b.category.lowercase()] ?: 0.0
            val remaining = (b.monthlyLimit - spent).coerceAtLeast(0.0)
            val pct = if (b.monthlyLimit > 0) (spent / b.monthlyLimit).toFloat() else 0f
            BudgetProgress(budget = b, spent = spent, remaining = remaining, percentageUsed = pct)
        }
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun deleteAccountAndData(onResult: (success: Boolean, reauthRequired: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            val state = authState.value
            val uid = (state as? AuthState.Authenticated)?.user?.uid ?: _currentUserId.value
            val isGuest = (state as? AuthState.Authenticated)?.user?.isAnonymous ?: true

            if (!isGuest) {
                realtimeDbService.deleteUserCloudData(uid)
                val authResult = authManager.deleteFirebaseAuthAccount()
                if (authResult.isFailure) {
                    val ex = authResult.exceptionOrNull()
                    val reauth = ex is FirebaseAuthRecentLoginRequiredException
                    onResult(false, reauth, ex?.message)
                    return@launch
                }
            } else {
                authManager.signOut()
            }

            withContext(Dispatchers.IO) {
                db.transactionDao().deleteAllForUser(uid)
                db.debtDao().deleteAllForUser(uid)
                db.debtPaymentDao().deleteAllForUser(uid)
                db.categoryDao().deleteAllForUser(uid)
                db.budgetDao().deleteAllForUser(uid)
                db.recurringDao().deleteAllForUser(uid)
                db.syncMetaDao().deleteAllForUser(uid)
            }
            setPinCode(null)
            _currentUserId.value = authManager.getCurrentUserId()
            onResult(true, false, null)
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
