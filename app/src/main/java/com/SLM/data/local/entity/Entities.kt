package com.SLM.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncStatus {
    SYNCED,
    PENDING_UPLOAD,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNC_FAILED
}

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class PaymentMethod {
    CASH,
    BANK,
    BKASH,
    NAGAD,
    CARD,
    OTHER
}

enum class DebtType {
    RECEIVABLE, // পাওনা (Money I will receive)
    PAYABLE     // দেনা (Money I have to pay)
}

enum class DebtStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    OVERDUE
}

enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Entity(
    tableName = "transactions",
    indices = [Index(value = ["userId"]), Index(value = ["date"]), Index(value = ["syncStatus"])]
)
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(
    tableName = "debts",
    indices = [Index(value = ["userId"]), Index(value = ["dueDate"]), Index(value = ["syncStatus"])]
)
data class DebtEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val personName: String,
    val phoneNumber: String = "",
    val debtType: DebtType, // RECEIVABLE ("পাওনা") or PAYABLE ("দেনা")
    val totalAmount: Double,
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["debtId"]), Index(value = ["userId"]), Index(value = ["syncStatus"])]
)
data class DebtPaymentEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val debtId: String,
    val userId: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["userId"]), Index(value = ["name", "type", "userId"], unique = true)]
)
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val name: String,
    val type: TransactionType,
    val iconName: String = "category",
    val colorHex: String = "#10B981",
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["userId"]), Index(value = ["category", "monthYear", "userId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val category: String,
    val monthlyLimit: Double,
    val monthYear: String, // Format: YYYY-MM
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(
    tableName = "recurring_transactions",
    indices = [Index(value = ["userId"])]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val title: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val frequency: RecurringFrequency,
    val nextDueDate: Long,
    val lastGeneratedDate: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
)

@Entity(tableName = "sync_meta")
data class SyncMetaEntity(
    @PrimaryKey val userId: String,
    val lastSyncTimestamp: Long = 0L,
    val status: String = "SYNCED",
    val pendingCount: Int = 0,
    val errorMessage: String? = null
)
