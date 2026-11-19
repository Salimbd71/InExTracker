package com.example.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.entity.SyncMetaEntity
import com.example.data.local.entity.SyncStatus
import com.example.data.remote.FirebaseRealtimeDbService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SyncUiState {
    IDLE,
    SYNCING,
    SYNCED,
    OFFLINE,
    ERROR
}

class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val realtimeDbService: FirebaseRealtimeDbService,
    private val coroutineScope: CoroutineScope
) {
    private val tag = "SyncManager"
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _syncState = MutableStateFlow(SyncUiState.IDLE)
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _isOnline = MutableStateFlow(checkInitialConnectivity())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        registerNetworkObserver()
    }

    private fun checkInitialConnectivity(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun registerNetworkObserver() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
                coroutineScope.launch {
                    // Trigger auto-sync when online
                    // Get current active user from database or sync meta
                }
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
                _syncState.value = SyncUiState.OFFLINE
            }
        }

        try {
            networkCallback?.let { connectivityManager.registerNetworkCallback(request, it) }
        } catch (e: Exception) {
            Log.w(tag, "Failed to register network callback: ${e.message}")
        }
    }

    suspend fun syncAll(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.startsWith("guest_") || userId.isBlank()) {
            // Guest mode does not sync to cloud
            database.syncMetaDao().updateSyncMeta(
                SyncMetaEntity(
                    userId = userId,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "OFFLINE",
                    pendingCount = 0,
                    errorMessage = null
                )
            )
            _syncState.value = SyncUiState.OFFLINE
            return@withContext true
        }

        if (!_isOnline.value) {
            _syncState.value = SyncUiState.OFFLINE
            return@withContext false
        }

        _syncState.value = SyncUiState.SYNCING
        try {
            val lastMeta = database.syncMetaDao().getSyncMetaDirect(userId)
            val sinceTimestamp = lastMeta?.lastSyncTimestamp ?: 0L

            // 1. Upload Pending Local Transactions
            val pendingTx = database.transactionDao().getPendingSyncTransactions()
            for (tx in pendingTx) {
                val success = realtimeDbService.uploadTransaction(tx)
                if (success) {
                    database.transactionDao().updateTransaction(tx.copy(syncStatus = SyncStatus.SYNCED))
                }
            }

            // 2. Upload Pending Local Debts
            val pendingDebts = database.debtDao().getPendingSyncDebts()
            for (debt in pendingDebts) {
                val success = realtimeDbService.uploadDebt(debt)
                if (success) {
                    database.debtDao().updateDebt(debt.copy(syncStatus = SyncStatus.SYNCED))
                }
            }

            // 3. Upload Pending Local Debt Payments
            val pendingPayments = database.debtPaymentDao().getPendingSyncPayments()
            for (payment in pendingPayments) {
                val success = realtimeDbService.uploadDebtPayment(payment)
                if (success) {
                    database.debtPaymentDao().insertPayment(payment.copy(syncStatus = SyncStatus.SYNCED))
                }
            }

            // 4. Download Cloud Changes from Realtime Database (Two-way sync with timestamp comparison)
            val remoteTransactions = realtimeDbService.fetchTransactions(userId, sinceTimestamp)
            for (remoteTx in remoteTransactions) {
                val localTx = database.transactionDao().getTransactionById(remoteTx.id)
                if (localTx == null || remoteTx.updatedAt >= localTx.updatedAt) {
                    database.transactionDao().insertTransaction(remoteTx)
                }
            }

            val remoteDebts = realtimeDbService.fetchDebts(userId, sinceTimestamp)
            for (remoteDebt in remoteDebts) {
                val localDebt = database.debtDao().getDebtById(remoteDebt.id)
                if (localDebt == null || remoteDebt.updatedAt >= localDebt.updatedAt) {
                    database.debtDao().insertDebt(remoteDebt)
                }
            }

            val remotePayments = realtimeDbService.fetchDebtPayments(userId, sinceTimestamp)
            if (remotePayments.isNotEmpty()) {
                database.debtPaymentDao().insertPayments(remotePayments)
            }

            val now = System.currentTimeMillis()
            database.syncMetaDao().updateSyncMeta(
                SyncMetaEntity(
                    userId = userId,
                    lastSyncTimestamp = now,
                    status = "SYNCED",
                    pendingCount = 0,
                    errorMessage = null
                )
            )
            _syncState.value = SyncUiState.SYNCED
            true
        } catch (e: Exception) {
            Log.e(tag, "Sync failed: ${e.message}", e)
            _syncState.value = SyncUiState.ERROR
            database.syncMetaDao().updateSyncMeta(
                SyncMetaEntity(
                    userId = userId,
                    lastSyncTimestamp = System.currentTimeMillis(),
                    status = "SYNC_FAILED",
                    pendingCount = 0,
                    errorMessage = e.message
                )
            )
            false
        }
    }
}
