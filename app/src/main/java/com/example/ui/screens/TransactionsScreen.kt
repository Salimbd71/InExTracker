package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthState
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionType
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.PremiumAppHeader
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.ExportUtils

@Composable
fun TransactionsScreen(
    viewModel: MainViewModel,
    onEditTransaction: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterDateRange by viewModel.filterDateRange.collectAsStateWithLifecycle()

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showPdfExportDialog by remember { mutableStateOf(false) }

    val userName = (authState as? AuthState.Authenticated)?.user?.displayName ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = strings.allTransactionsTitle,
            subtitle = "${transactions.size} ${if (lang == "bn") "টি লেনদেন" else "records"}",
            actions = {
                IconButton(
                    onClick = { showPdfExportDialog = true },
                    modifier = Modifier.testTag("export_transactions_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = strings.downloadPdfReportButton,
                        tint = Color.White
                    )
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text(strings.searchTransactionsHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = strings.closeLabel)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("transaction_search_input"),
            singleLine = true
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            item(key = "type_all") {
                FilterChip(
                    selected = filterType == null,
                    onClick = { viewModel.filterType.value = null },
                    label = { Text(strings.filterAll) }
                )
            }
            item(key = "type_income") {
                FilterChip(
                    selected = filterType == TransactionType.INCOME,
                    onClick = {
                        viewModel.filterType.value =
                            if (filterType == TransactionType.INCOME) null else TransactionType.INCOME
                    },
                    label = { Text(strings.filterIncome) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = 0.2f))
                )
            }
            item(key = "type_expense") {
                FilterChip(
                    selected = filterType == TransactionType.EXPENSE,
                    onClick = {
                        viewModel.filterType.value =
                            if (filterType == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                    },
                    label = { Text(strings.filterExpense) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseRed.copy(alpha = 0.2f))
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            items(DateFilterType.entries, key = { it.name }) { dateType ->
                val label = when (dateType) {
                    DateFilterType.ALL -> strings.periodAllTime
                    DateFilterType.TODAY -> strings.periodToday
                    DateFilterType.YESTERDAY -> strings.periodYesterday
                    DateFilterType.THIS_WEEK -> strings.periodThisWeek
                    DateFilterType.THIS_MONTH -> strings.periodThisMonth
                    DateFilterType.LAST_MONTH -> strings.periodLastMonth
                }
                FilterChip(
                    selected = filterDateRange == dateType,
                    onClick = { viewModel.filterDateRange.value = dateType },
                    label = { Text(label) }
                )
            }
        }

        if (transactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.noMatchingTransactions,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.changeFilterHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id },
                    contentType = { "tx_card" }
                ) { tx ->
                    TransactionItemCard(
                        transaction = tx,
                        currencySymbol = currencySymbol,
                        onClick = { onEditTransaction(tx) }
                    )
                }
            }
        }
    }

    if (showPdfExportDialog) {
        val listToExport = transactions.ifEmpty { allTransactions }
        PdfExportDialog(
            reportTitle = strings.allTransactionsTitle,
            suggestedFileName = "InExTracker_Transactions_${System.currentTimeMillis()}.pdf",
            generatePdf = {
                ExportUtils.generateTransactionsPdfFile(
                    context = context,
                    transactions = listToExport,
                    currencySymbol = currencySymbol,
                    lang = lang,
                    userName = userName
                )
            },
            onDismiss = { showPdfExportDialog = false }
        )
    }

    transactionToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text(strings.deleteTransactionTitle) },
            text = { Text("${tx.category} — ${tx.amount} (${strings.deleteTransactionConfirm})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTransaction(tx.id)
                        transactionToDelete = null
                    }
                ) {
                    Text(strings.deleteAction, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}
