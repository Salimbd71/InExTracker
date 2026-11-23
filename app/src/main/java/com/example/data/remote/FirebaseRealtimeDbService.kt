package com.example.data.remote

import android.util.Log
import com.example.auth.UserProfile
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PaymentMethod
import com.example.data.local.entity.SyncStatus
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRealtimeDbService {
    private val tag = "FirebaseRealtimeDb"

    private val dbInstance: FirebaseDatabase?
        get() = try {
            try {
                FirebaseDatabase.getInstance()
            } catch (e: Throwable) {
                FirebaseDatabase.getInstance("https://inex-tracker-c3ecb-default-rtdb.asia-southeast1.firebasedatabase.app")
            }
        } catch (e: Throwable) {
            Log.w(tag, "FirebaseDatabase instance unavailable: ${e.message}")
            null
        }

    suspend fun uploadTransaction(entity: TransactionEntity): Boolean {
        return try {
            val db = dbInstance ?: return false
            val map = hashMapOf<String, Any?>(
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
            db.reference
                .child("users")
                .child(entity.userId)
                .child("transactions")
                .child(entity.id)
                .setValue(map)
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload transaction to Realtime DB: ${e.message}")
            false
        }
    }

    suspend fun fetchTransactions(userId: String, sinceTimestamp: Long): List<TransactionEntity> {
        return try {
            val db = dbInstance ?: return emptyList()
            val snapshot = db.reference
                .child("users")
                .child(userId)
                .child("transactions")
                .get()
                .await()

            val list = mutableListOf<TransactionEntity>()
            for (child in snapshot.children) {
                try {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val typeStr = child.child("type").getValue(String::class.java) ?: "EXPENSE"
                    val type = runCatching { TransactionType.valueOf(typeStr) }.getOrDefault(TransactionType.EXPENSE)
                    val amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0
                    val category = child.child("category").getValue(String::class.java) ?: "Other"
                    val date = (child.child("date").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val note = child.child("note").getValue(String::class.java) ?: ""
                    val pmStr = child.child("paymentMethod").getValue(String::class.java) ?: "CASH"
                    val pm = runCatching { PaymentMethod.valueOf(pmStr) }.getOrDefault(PaymentMethod.CASH)
                    val receiptUri = child.child("receiptUri").getValue(String::class.java)
                    val createdAt = (child.child("createdAt").value as? Number)?.toLong() ?: date
                    val updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: date
                    val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                    val deletedAt = (child.child("deletedAt").value as? Number)?.toLong()

                    if (updatedAt >= sinceTimestamp || sinceTimestamp == 0L) {
                        list.add(
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
                        )
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Parse transaction error: ${e.message}")
                }
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch transactions from Realtime DB: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadDebt(entity: DebtEntity): Boolean {
        return try {
            val db = dbInstance ?: return false
            val map = hashMapOf<String, Any?>(
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
            db.reference
                .child("users")
                .child(entity.userId)
                .child("debts")
                .child(entity.id)
                .setValue(map)
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload debt to Realtime DB: ${e.message}")
            false
        }
    }

    suspend fun fetchDebts(userId: String, sinceTimestamp: Long): List<DebtEntity> {
        return try {
            val db = dbInstance ?: return emptyList()
            val snapshot = db.reference
                .child("users")
                .child(userId)
                .child("debts")
                .get()
                .await()

            val list = mutableListOf<DebtEntity>()
            for (child in snapshot.children) {
                try {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val personName = child.child("personName").getValue(String::class.java) ?: ""
                    val phoneNumber = child.child("phoneNumber").getValue(String::class.java) ?: ""
                    val debtTypeStr = child.child("debtType").getValue(String::class.java) ?: "RECEIVABLE"
                    val debtType = runCatching { DebtType.valueOf(debtTypeStr) }.getOrDefault(DebtType.RECEIVABLE)
                    val totalAmount = (child.child("totalAmount").value as? Number)?.toDouble() ?: 0.0
                    val date = (child.child("date").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val dueDate = (child.child("dueDate").value as? Number)?.toLong()
                    val note = child.child("note").getValue(String::class.java) ?: ""
                    val createdAt = (child.child("createdAt").value as? Number)?.toLong() ?: date
                    val updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: date
                    val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                    val deletedAt = (child.child("deletedAt").value as? Number)?.toLong()

                    if (updatedAt >= sinceTimestamp || sinceTimestamp == 0L) {
                        list.add(
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
                        )
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Parse debt error: ${e.message}")
                }
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch debts from Realtime DB: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadDebtPayment(entity: DebtPaymentEntity): Boolean {
        return try {
            val db = dbInstance ?: return false
            val map = hashMapOf<String, Any?>(
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
            db.reference
                .child("users")
                .child(entity.userId)
                .child("debt_payments")
                .child(entity.id)
                .setValue(map)
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload payment to Realtime DB: ${e.message}")
            false
        }
    }

    suspend fun fetchDebtPayments(userId: String, sinceTimestamp: Long): List<DebtPaymentEntity> {
        return try {
            val db = dbInstance ?: return emptyList()
            val snapshot = db.reference
                .child("users")
                .child(userId)
                .child("debt_payments")
                .get()
                .await()

            val list = mutableListOf<DebtPaymentEntity>()
            for (child in snapshot.children) {
                try {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val debtId = child.child("debtId").getValue(String::class.java) ?: ""
                    val amount = (child.child("amount").value as? Number)?.toDouble() ?: 0.0
                    val date = (child.child("date").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val note = child.child("note").getValue(String::class.java) ?: ""
                    val pmStr = child.child("paymentMethod").getValue(String::class.java) ?: "CASH"
                    val pm = runCatching { PaymentMethod.valueOf(pmStr) }.getOrDefault(PaymentMethod.CASH)
                    val createdAt = (child.child("createdAt").value as? Number)?.toLong() ?: date
                    val updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: date
                    val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false
                    val deletedAt = (child.child("deletedAt").value as? Number)?.toLong()

                    if (updatedAt >= sinceTimestamp || sinceTimestamp == 0L) {
                        list.add(
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
                        )
                    }
                } catch (e: Exception) {
                    Log.w(tag, "Parse payment error: ${e.message}")
                }
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch debt payments from Realtime DB: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadBudget(entity: BudgetEntity): Boolean {
        return try {
            val db = dbInstance ?: return false
            val map = hashMapOf<String, Any?>(
                "id" to entity.id,
                "userId" to entity.userId,
                "category" to entity.category,
                "monthlyLimit" to entity.monthlyLimit,
                "monthYear" to entity.monthYear,
                "createdAt" to entity.createdAt,
                "updatedAt" to entity.updatedAt,
                "isDeleted" to entity.isDeleted
            )
            db.reference
                .child("users")
                .child(entity.userId)
                .child("budgets")
                .child(entity.id)
                .setValue(map)
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload budget to Realtime DB: ${e.message}")
            false
        }
    }

    suspend fun fetchBudgets(userId: String): List<BudgetEntity> {
        return try {
            val db = dbInstance ?: return emptyList()
            val snapshot = db.reference
                .child("users")
                .child(userId)
                .child("budgets")
                .get()
                .await()

            val list = mutableListOf<BudgetEntity>()
            for (child in snapshot.children) {
                try {
                    val id = child.child("id").getValue(String::class.java) ?: child.key ?: continue
                    val category = child.child("category").getValue(String::class.java) ?: "Other"
                    val monthlyLimit = (child.child("monthlyLimit").value as? Number)?.toDouble() ?: 0.0
                    val monthYear = child.child("monthYear").getValue(String::class.java) ?: ""
                    val createdAt = (child.child("createdAt").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val updatedAt = (child.child("updatedAt").value as? Number)?.toLong() ?: System.currentTimeMillis()
                    val isDeleted = child.child("isDeleted").getValue(Boolean::class.java) ?: false

                    list.add(
                        BudgetEntity(
                            id = id,
                            userId = userId,
                            category = category,
                            monthlyLimit = monthlyLimit,
                            monthYear = monthYear,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            isDeleted = isDeleted,
                            syncStatus = SyncStatus.SYNCED
                        )
                    )
                } catch (e: Exception) {
                    Log.w(tag, "Parse budget error: ${e.message}")
                }
            }
            list
        } catch (e: Exception) {
            Log.w(tag, "Failed to fetch budgets from Realtime DB: ${e.message}")
            emptyList()
        }
    }

    suspend fun uploadUserProfile(profile: UserProfile): Boolean {
        return try {
            val db = dbInstance ?: return false
            val map = hashMapOf<String, Any?>(
                "uid" to profile.uid,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "photoUrl" to (profile.photoUrl ?: ""),
                "isAnonymous" to profile.isAnonymous,
                "creationTimestamp" to profile.creationTimestamp,
                "lastActive" to System.currentTimeMillis()
            )
            db.reference
                .child("users")
                .child(profile.uid)
                .child("profile")
                .setValue(map)
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to upload profile to Realtime DB: ${e.message}")
            false
        }
    }
}
