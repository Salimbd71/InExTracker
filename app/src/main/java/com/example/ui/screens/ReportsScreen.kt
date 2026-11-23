package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.TransactionType
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.PdfExportDialog
import com.example.ui.components.PremiumAppHeader
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ExportUtils

@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allDebts by viewModel.debtsWithPayments.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(DateFilterType.THIS_MONTH) }
    var showPdfDialog by remember { mutableStateOf(false) }

    val filteredTransactions = remember(allTransactions, selectedPeriod) {
        val bounds: Pair<Long, Long>? = when (selectedPeriod) {
            DateFilterType.ALL -> null
            DateFilterType.TODAY -> DateUtils.getDayBounds(0)
            DateFilterType.YESTERDAY -> DateUtils.getDayBounds(-1)
            DateFilterType.THIS_WEEK -> DateUtils.getWeekBounds()
            DateFilterType.THIS_MONTH -> DateUtils.getMonthBounds(0)
            DateFilterType.LAST_MONTH -> DateUtils.getMonthBounds(-1)
        }
        if (bounds == null) allTransactions
        else allTransactions.filter { it.date in bounds.first..bounds.second }
    }

    val (totalIncome, totalExpense) = remember(filteredTransactions) {
        var inc = 0.0
        var exp = 0.0
        for (tx in filteredTransactions) {
            if (tx.type == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
        }
        inc to exp
    }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceAtLeast(0.0).toInt() else 0

    val animatedIncome by animateFloatAsState(
        targetValue = totalIncome.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "anim_income"
    )
    val animatedExpense by animateFloatAsState(
        targetValue = totalExpense.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "anim_expense"
    )
    val animatedNet by animateFloatAsState(
        targetValue = netSavings.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "anim_net"
    )

    val (totalReceivable, totalPayable) = remember(allDebts) {
        var rec = 0.0
        var pay = 0.0
        for (d in allDebts) {
            if (d.debt.debtType == DebtType.RECEIVABLE) rec += d.remainingAmount else pay += d.remainingAmount
        }
        rec to pay
    }

    val expenseCategories = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val periodLabel = when (selectedPeriod) {
        DateFilterType.ALL -> strings.periodAllTime
        DateFilterType.TODAY -> strings.periodToday
        DateFilterType.YESTERDAY -> strings.periodYesterday
        DateFilterType.THIS_WEEK -> strings.periodThisWeek
        DateFilterType.THIS_MONTH -> strings.periodThisMonth
        DateFilterType.LAST_MONTH -> strings.periodLastMonth
    }
    val userName = (authState as? AuthState.Authenticated)?.user?.displayName ?: ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = strings.reportsTitle,
            subtitle = periodLabel,
            actions = {
                IconButton(
                    onClick = { showPdfDialog = true },
                    modifier = Modifier.testTag("share_financial_report_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = strings.downloadPdfReportButton,
                        tint = Color.White
                    )
                }
            }
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DateFilterType.entries, key = { it.name }) { period ->
                val label = when (period) {
                    DateFilterType.ALL -> strings.periodAllTime
                    DateFilterType.TODAY -> strings.periodToday
                    DateFilterType.YESTERDAY -> strings.periodYesterday
                    DateFilterType.THIS_WEEK -> strings.periodThisWeek
                    DateFilterType.THIS_MONTH -> strings.periodThisMonth
                    DateFilterType.LAST_MONTH -> strings.periodLastMonth
                }
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(label) }
                )
            }
        }

        AnimatedContent(
            targetState = selectedPeriod,
            transitionSpec = {
                fadeIn(tween(240)) togetherWith fadeOut(tween(180))
            },
            label = "report_period_transition"
        ) { _ ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "net_savings_card", contentType = "summary") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = strings.netSavingsLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyUtils.formatAmount(animatedNet.toDouble(), currencySymbol),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (netSavings >= 0) IncomeGreen else ExpenseRed
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = IncomeGreen.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Savings, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$savingsRate${strings.savingsRateSuffix}",
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = IncomeGreen.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(strings.totalIncomeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = CurrencyUtils.formatAmount(animatedIncome.toDouble(), currencySymbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    color = ExpenseRed.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(strings.totalExpenseLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = CurrencyUtils.formatAmount(animatedExpense.toDouble(), currencySymbol),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ExpenseRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "donut_chart", contentType = "chart") {
                    CategoryDonutChart(
                        transactions = filteredTransactions,
                        type = TransactionType.EXPENSE,
                        currencySymbol = currencySymbol
                    )
                }

                item(key = "cat_breakdown_header", contentType = "header") {
                    Text(
                        text = strings.categoryBreakdownTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expenseCategories.isEmpty()) {
                    item(key = "no_expense_item") {
                        Text(
                            text = strings.noExpenseInPeriod,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(
                        items = expenseCategories,
                        key = { it.first },
                        contentType = { "category_bar" }
                    ) { item ->
                        val catName = item.first
                        val catAmount = item.second
                        val pct = if (totalExpense > 0) (catAmount / totalExpense).toFloat().coerceIn(0f, 1f) else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = pct,
                            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
                            label = "cat_progress_$catName"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(catName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = CurrencyUtils.formatAmount(catAmount, currencySymbol),
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ExpenseRed,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }

                item(key = "pdf_export_button", contentType = "action") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { showPdfDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("download_pdf_report_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.downloadPdfReportButton, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showPdfDialog) {
        PdfExportDialog(
            reportTitle = "${strings.reportsTitle} ($periodLabel)",
            suggestedFileName = "InExTracker_Report_${System.currentTimeMillis()}.pdf",
            generatePdf = {
                ExportUtils.generateFinancialReportPdfFile(
                    context = context,
                    periodLabel = periodLabel,
                    totalIncome = totalIncome,
                    totalExpense = totalExpense,
                    balance = netSavings,
                    totalReceivable = totalReceivable,
                    totalPayable = totalPayable,
                    transactions = filteredTransactions,
                    currencySymbol = currencySymbol,
                    lang = lang,
                    userName = userName
                )
            },
            onDismiss = { showPdfDialog = false }
        )
    }
}
