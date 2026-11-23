package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthState
import com.example.data.local.entity.DebtStatus
import com.example.data.local.entity.DebtType
import com.example.data.repository.DebtWithPayments
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.PremiumAppHeader
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AccentGold
import com.example.ui.theme.DebtPayable
import com.example.ui.theme.DebtReceivable
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ExportUtils

@Composable
fun DebtKhataScreen(
    viewModel: MainViewModel,
    onDebtClick: (DebtWithPayments) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val debtsWithPayments by viewModel.filteredDebts.collectAsStateWithLifecycle()
    val allDebtsWithPayments by viewModel.debtsWithPayments.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    val searchQuery by viewModel.debtSearchQuery.collectAsStateWithLifecycle()
    val filterStatus by viewModel.debtFilterStatus.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Receivable, 1: Payable
    var showPdfExportDialog by remember { mutableStateOf(false) }

    val currentTabType = if (selectedTab == 0) DebtType.RECEIVABLE else DebtType.PAYABLE
    val displayedDebts = remember(debtsWithPayments, currentTabType) {
        debtsWithPayments.filter { it.debt.debtType == currentTabType }
    }

    val (totalReceivable, totalPayable) = remember(allDebtsWithPayments) {
        var rec = 0.0
        var pay = 0.0
        for (d in allDebtsWithPayments) {
            if (d.debt.debtType == DebtType.RECEIVABLE) rec += d.remainingAmount else pay += d.remainingAmount
        }
        rec to pay
    }

    val userName = (authState as? AuthState.Authenticated)?.user?.displayName ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = strings.debtBookTitle,
            subtitle = "${allDebtsWithPayments.size} ${if (lang == "bn") "টি হিসাব" else "entries"}",
            actions = {
                IconButton(
                    onClick = { showPdfExportDialog = true },
                    modifier = Modifier.testTag("export_debts_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = strings.downloadPdfReportButton,
                        tint = Color.White
                    )
                }
            }
        )

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.receivableTabTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 0) DebtReceivable else Color.Gray
                        )
                        Text(
                            text = CurrencyUtils.formatAmount(totalReceivable, currencySymbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DebtReceivable
                        )
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = strings.payableTabTitle,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTab == 1) DebtPayable else Color.Gray
                        )
                        Text(
                            text = CurrencyUtils.formatAmount(totalPayable, currencySymbol),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = DebtPayable
                        )
                    }
                }
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.debtSearchQuery.value = it },
            placeholder = { Text(strings.searchDebtHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.debtSearchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = strings.closeLabel)
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("debt_search_input"),
            singleLine = true
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            item(key = "debt_status_all") {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { viewModel.debtFilterStatus.value = null },
                    label = { Text(strings.filterAll) }
                )
            }
            item(key = "debt_status_pending") {
                FilterChip(
                    selected = filterStatus == DebtStatus.PENDING,
                    onClick = {
                        viewModel.debtFilterStatus.value =
                            if (filterStatus == DebtStatus.PENDING) null else DebtStatus.PENDING
                    },
                    label = { Text(strings.statusPending) }
                )
            }
            item(key = "debt_status_partial") {
                FilterChip(
                    selected = filterStatus == DebtStatus.PARTIALLY_PAID,
                    onClick = {
                        viewModel.debtFilterStatus.value =
                            if (filterStatus == DebtStatus.PARTIALLY_PAID) null else DebtStatus.PARTIALLY_PAID
                    },
                    label = { Text(strings.statusPartiallyPaid) }
                )
            }
            item(key = "debt_status_overdue") {
                FilterChip(
                    selected = filterStatus == DebtStatus.OVERDUE,
                    onClick = {
                        viewModel.debtFilterStatus.value =
                            if (filterStatus == DebtStatus.OVERDUE) null else DebtStatus.OVERDUE
                    },
                    label = { Text(strings.statusOverdue) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ExpenseRed.copy(alpha = 0.2f))
                )
            }
            item(key = "debt_status_paid") {
                FilterChip(
                    selected = filterStatus == DebtStatus.PAID,
                    onClick = {
                        viewModel.debtFilterStatus.value =
                            if (filterStatus == DebtStatus.PAID) null else DebtStatus.PAID
                    },
                    label = { Text(strings.statusPaid) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = IncomeGreen.copy(alpha = 0.2f))
                )
            }
        }

        if (displayedDebts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Handshake,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedTab == 0) strings.noReceivableTitle else strings.noPayableTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = strings.addDebtHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = displayedDebts,
                    key = { it.debt.id },
                    contentType = { "debt_card" }
                ) { item ->
                    DebtItemCard(
                        debtWithPayments = item,
                        currencySymbol = currencySymbol,
                        onClick = { onDebtClick(item) }
                    )
                }
            }
        }
    }

    if (showPdfExportDialog) {
        PdfExportDialog(
            reportTitle = strings.debtBookTitle,
            suggestedFileName = "InExTracker_DebtBook_${System.currentTimeMillis()}.pdf",
            generatePdf = {
                ExportUtils.generateDebtsPdfFile(
                    context = context,
                    debts = allDebtsWithPayments,
                    currencySymbol = currencySymbol,
                    lang = lang,
                    userName = userName
                )
            },
            onDismiss = { showPdfExportDialog = false }
        )
    }
}

@Composable
fun DebtItemCard(
    debtWithPayments: DebtWithPayments,
    currencySymbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val debt = debtWithPayments.debt
    val isReceivable = debt.debtType == DebtType.RECEIVABLE
    val accentColor = if (isReceivable) DebtReceivable else DebtPayable

    val progress = if (debt.totalAmount > 0) (debtWithPayments.totalPaid / debt.totalAmount).toFloat() else 0f
    val formattedDue = remember(debt.dueDate, lang) { debt.dueDate?.let { DateUtils.formatDate(it, lang) } }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("debt_item_${debt.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = debt.personName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (debt.phoneNumber.isNotBlank()) {
                            Text(
                                text = debt.phoneNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (debtWithPayments.status) {
                        DebtStatus.PAID -> IncomeGreen.copy(alpha = 0.15f)
                        DebtStatus.OVERDUE -> ExpenseRed.copy(alpha = 0.15f)
                        DebtStatus.PARTIALLY_PAID -> AccentGold.copy(alpha = 0.15f)
                        DebtStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = when (debtWithPayments.status) {
                            DebtStatus.PAID -> strings.statusPaid
                            DebtStatus.OVERDUE -> strings.statusOverdue
                            DebtStatus.PARTIALLY_PAID -> strings.statusPartiallyPaid
                            DebtStatus.PENDING -> strings.statusPending
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (debtWithPayments.status) {
                            DebtStatus.PAID -> IncomeGreen
                            DebtStatus.OVERDUE -> ExpenseRed
                            DebtStatus.PARTIALLY_PAID -> AccentGold
                            DebtStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = strings.totalAmountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(debt.totalAmount, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = strings.paidAmountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(debtWithPayments.totalPaid, currencySymbol),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeGreen
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = strings.remainingAmountLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyUtils.formatAmount(debtWithPayments.remainingAmount, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = IncomeGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            if (formattedDue != null || debt.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (formattedDue != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (debtWithPayments.status == DebtStatus.OVERDUE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${strings.dueLabel}: $formattedDue",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (debtWithPayments.status == DebtStatus.OVERDUE) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
