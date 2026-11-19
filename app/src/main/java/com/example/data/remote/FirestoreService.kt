package com.example.data.remote

import android.util.Log
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.RecurringFrequency
import com.example.data.local.entity.RecurringTransactionEntity
import com.example.data.local.entity.SyncStatus
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreService {
    private val tag = "FirestoreService"

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (e: Throwable) {
            Log.w(tag, "FirebaseFirestore unavailable: ${e.message}")
            null
        }

    suspend fun uploadTransaction(entity: TransactionEntity): Boolean {
        return try {
            val db = firestore ?: return false
            val map = hashMapOf(
                "id" to entity.id,
                "userId" to entity.userId,
                "type" to entity.type.name,
                "amount" to entity.amount,
                "category" to entity.category,
                "date" to entity.date,
                "note" to entity.note,
                "paymentMethod" to entity.paymentMethod.name,
                "receiptUri" to (entity.receiptUri ?: ""),
                "createdAt" to entity.createdAt,
                "updatedAt" to entity.updatedAt,
                "isDeleted" to entity.isDeleted,
                "deletedAt" to (entity.deletedAt ?: 0L)
            )
            db.collection("users")
                .document(entity.userId)
                .collection("transactions")
                .document(entity.id)
                .set(map, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload transaction: ${e.message}")
            false
        }
    }

    suspend fun fetchTransactions(userId: String, sinceTimestamp: Long): List<TransactionEntity> {
        return try {
            val db = firestore ?: return emptyList()
            val query = db.collection("users")
                .document(userId)
                .collection("transactions")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get()
                .await()

            query.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val typeStr = doc.getString("type") ?: "EXPENSE"
                    val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
                    val amount = doc.getDouble("amount") ?: 0.0
                    val category = doc.getString("category") ?: "Other"
                    val date = doc.getLong("date") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    val pmStr = doc.getString("paymentMethod") ?: "CASH"
                    val pm = runCatching { PaymentMethod.valueOf(pmStr) }.getOrDefault(PaymentMethod.CASH)
                    val receiptUri = doc.getString("receiptUri")
                    val createdAt = doc.getLong("createdAt") ?: date
                    val updatedAt = doc.getLong("updatedAt") ?: date
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val deletedAt = doc.getLong("deletedAt")

                    TransactionEntity(
                        id = id,
                        userId = userId,
                        type = type,
                        amount = amount,
                        category = category,
                        date = date,
                        note = note,
                        paymentMethod = pm,
                        receiptUri = receiptUri,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isDeleted = isDeleted,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch transactions: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadDebt(entity: DebtEntity): Boolean {
        return try {
            val db = firestore ?: return false
            val map = hashMapOf(
                "id" to entity.id,
                "userId" to entity.userId,
                "personName" to entity.personName,
                "phoneNumber" to entity.phoneNumber,
                "debtType" to entity.debtType.name,
                "totalAmount" to entity.totalAmount,
                "date" to entity.date,
                "dueDate" to (entity.dueDate ?: 0L),
                "note" to entity.note,
                "createdAt" to entity.createdAt,
                "updatedAt" to entity.updatedAt,
                "isDeleted" to entity.isDeleted,
                "deletedAt" to (entity.deletedAt ?: 0L)
            )
            db.collection("users")
                .document(entity.userId)
                .collection("debts")
                .document(entity.id)
                .set(map, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload debt: ${e.message}")
            false
        }
    }

    suspend fun fetchDebts(userId: String, sinceTimestamp: Long): List<DebtEntity> {
        return try {
            val db = firestore ?: return emptyList()
            val query = db.collection("users")
                .document(userId)
                .collection("debts")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get()
                .await()

            query.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val personName = doc.getString("personName") ?: ""
                    val phoneNumber = doc.getString("phoneNumber") ?: ""
                    val debtTypeStr = doc.getString("debtType") ?: "RECEIVABLE"
                    val debtType = runCatching { DebtType.valueOf(debtTypeStr) }.getOrDefault(DebtType.RECEIVABLE)
                    val totalAmount = doc.getDouble("totalAmount") ?: 0.0
                    val date = doc.getLong("date") ?: System.currentTimeMillis()
                    val dueDate = doc.getLong("dueDate")
                    val note = doc.getString("note") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: date
                    val updatedAt = doc.getLong("updatedAt") ?: date
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val deletedAt = doc.getLong("deletedAt")

                    DebtEntity(
                        id = id,
                        userId = userId,
                        personName = personName,
                        phoneNumber = phoneNumber,
                        debtType = debtType,
                        totalAmount = totalAmount,
                        date = date,
                        dueDate = if (dueDate != null && dueDate > 0) dueDate else null,
                        note = note,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isDeleted = isDeleted,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch debts: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadDebtPayment(entity: DebtPaymentEntity): Boolean {
        return try {
            val db = firestore ?: return false
            val map = hashMapOf(
                "id" to entity.id,
                "debtId" to entity.debtId,
                "userId" to entity.userId,
                "amount" to entity.amount,
                "date" to entity.date,
                "note" to entity.note,
                "paymentMethod" to entity.paymentMethod.name,
                "createdAt" to entity.createdAt,
                "updatedAt" to entity.updatedAt,
                "isDeleted" to entity.isDeleted,
                "deletedAt" to (entity.deletedAt ?: 0L)
            )
            db.collection("users")
                .document(entity.userId)
                .collection("debt_payments")
                .document(entity.id)
                .set(map, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload payment: ${e.message}")
            false
        }
    }

    suspend fun fetchDebtPayments(userId: String, sinceTimestamp: Long): List<DebtPaymentEntity> {
        return try {
            val db = firestore ?: return emptyList()
            val query = db.collection("users")
                .document(userId)
                .collection("debt_payments")
                .whereGreaterThan("updatedAt", sinceTimestamp)
                .get()
                .await()

            query.documents.mapNotNull { doc ->
                try {
                    val id = doc.getString("id") ?: doc.id
                    val debtId = doc.getString("debtId") ?: ""
                    val amount = doc.getDouble("amount") ?: 0.0
                    val date = doc.getLong("date") ?: System.currentTimeMillis()
                    val note = doc.getString("note") ?: ""
                    val pmStr = doc.getString("paymentMethod") ?: "CASH"
                    val pm = runCatching { PaymentMethod.valueOf(pmStr) }.getOrDefault(PaymentMethod.CASH)
                    val createdAt = doc.getLong("createdAt") ?: date
                    val updatedAt = doc.getLong("updatedAt") ?: date
                    val isDeleted = doc.getBoolean("isDeleted") ?: false
                    val deletedAt = doc.getLong("deletedAt")

                    DebtPaymentEntity(
                        id = id,
                        debtId = debtId,
                        userId = userId,
                        amount = amount,
                        date = date,
                        note = note,
                        paymentMethod = pm,
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        isDeleted = isDeleted,
                        deletedAt = deletedAt,
                        syncStatus = SyncStatus.SYNCED
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch debt payments: ${e.message}")
            emptyList()
        }
    }
}
