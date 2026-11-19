package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SyncMetaEntity
import com.example.data.local.entity.SyncStatus
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 AND type = :type ORDER BY date DESC")
    fun getTransactionsByType(userId: String, type: TransactionType): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 AND date BETWEEN :startTime AND :endTime ORDER BY date DESC")
    fun getTransactionsInRange(userId: String, startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(userId: String, limit: Int = 10): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :timestamp, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun hardDeleteTransaction(id: String)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE userId = :userId AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDebts(userId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id LIMIT 1")
    suspend fun getDebtById(id: String): DebtEntity?

    @Query("SELECT * FROM debts WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncDebts(): List<DebtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebts(debts: List<DebtEntity>)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("UPDATE debts SET isDeleted = 1, deletedAt = :timestamp, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeleteDebt(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun hardDeleteDebt(id: String)
}

@Dao
interface DebtPaymentDao {
    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId AND isDeleted = 0 ORDER BY date DESC")
    fun getPaymentsForDebt(debtId: String): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllPayments(userId: String): Flow<List<DebtPaymentEntity>>

    @Query("SELECT * FROM debt_payments WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncPayments(): List<DebtPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<DebtPaymentEntity>)

    @Query("UPDATE debt_payments SET isDeleted = 1, deletedAt = :timestamp, syncStatus = 'PENDING_DELETE', updatedAt = :timestamp WHERE id = :id")
    suspend fun softDeletePayment(id: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId AND isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteCategory(id: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId AND isDeleted = 0 AND monthYear = :monthYear")
    fun getBudgetsForMonth(userId: String, monthYear: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncBudgets(): List<BudgetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("UPDATE budgets SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteBudget(id: String)
}

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isDeleted = 0 AND isActive = 1")
    fun getActiveRecurring(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId AND isDeleted = 0")
    fun getAllRecurring(userId: String): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSyncRecurring(): List<RecurringTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringTransactionEntity)

    @Update
    suspend fun updateRecurring(recurring: RecurringTransactionEntity)

    @Query("UPDATE recurring_transactions SET isDeleted = 1, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDeleteRecurring(id: String)
}

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE userId = :userId LIMIT 1")
    fun getSyncMeta(userId: String): Flow<SyncMetaEntity?>

    @Query("SELECT * FROM sync_meta WHERE userId = :userId LIMIT 1")
    suspend fun getSyncMetaDirect(userId: String): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSyncMeta(meta: SyncMetaEntity)
}
