package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.TransactionType
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.CategoryDonutChart
import com.example.ui.theme.DebtPayable
import com.example.ui.theme.DebtReceivable
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.DateFilterType
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils
import com.example.utils.ExportUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val allDebts by viewModel.debtsWithPayments.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedPeriod by remember { mutableStateOf(DateFilterType.THIS_MONTH) }

    val filteredTransactions = remember(allTransactions, selectedPeriod) {
        allTransactions.filter { tx ->
            when (selectedPeriod) {
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
        }
    }

    val totalIncome = filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceAtLeast(0.0).toInt() else 0

    val totalReceivable = allDebts.filter { it.debt.debtType == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
    val totalPayable = allDebts.filter { it.debt.debtType == DebtType.PAYABLE }.sumOf { it.remainingAmount }

    val expenseCategories = filteredTransactions.filter { it.type == TransactionType.EXPENSE }
        .groupBy { it.category }
        .mapValues { it.value.sumOf { tx -> tx.amount } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "আয়-ব্যয় রিপোর্ট (Financial Report)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(
                    onClick = {
                        ExportUtils.exportReportSummary(
                            context = context,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            balance = netSavings,
                            totalReceivable = totalReceivable,
                            totalPayable = totalPayable,
                            transactions = filteredTransactions,
                            currencySymbol = currencySymbol
                        )
                    },
                    modifier = Modifier.testTag("share_financial_report_button")
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.primary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        AdMobBannerView()

        // Time Period Filter
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DateFilterType.values()) { period ->
                val label = when (period) {
                    DateFilterType.ALL -> "সকল সময়"
                    DateFilterType.TODAY -> "আজ"
                    DateFilterType.YESTERDAY -> "গতকাল"
                    DateFilterType.THIS_WEEK -> "এই সপ্তাহ"
                    DateFilterType.THIS_MONTH -> "চলতি মাস"
                    DateFilterType.LAST_MONTH -> "গত মাস"
                }
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { selectedPeriod = period },
                    label = { Text(label) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Net Savings Card
            item {
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
                                Text("নেট সঞ্চয় / উদ্বৃত্ত (Net Savings)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(netSavings, currencySymbol),
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
                                    Text("$savingsRate% সঞ্চয়", fontWeight = FontWeight.Bold, color = IncomeGreen, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Total Income
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = IncomeGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("মোট আয়", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyUtils.formatAmount(totalIncome, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }
                            }

                            // Total Expense
                            Surface(
                                modifier = Modifier.weight(1f),
                                color = ExpenseRed.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("মোট ব্যয়", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyUtils.formatAmount(totalExpense, currencySymbol),
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

            // Visual Donut Chart
            item {
                CategoryDonutChart(
                    transactions = filteredTransactions,
                    type = TransactionType.EXPENSE,
                    currencySymbol = currencySymbol
                )
            }

            // Category Breakdown List
            item {
                Text(
                    text = "ক্যাটাগরি ভিত্তিক ব্যয়ের বিবরণ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (expenseCategories.isEmpty()) {
                item {
                    Text(
                        text = "এই সময়ে কোন ব্যয়ের হিসাব নেই",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(expenseCategories) { item ->
                    val catName = item.first
                    val catAmount = item.second
                    val pct = if (totalExpense > 0) (catAmount / totalExpense).toFloat() else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                progress = { pct.coerceIn(0f, 1f) },
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

            // Export Actions
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        ExportUtils.exportTransactionsToCsv(context, filteredTransactions, currencySymbol)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("এই সময়ের CSV রিপোর্ট ডাউনলোড ও শেয়ার করুন")
                }
            }
        }
    }
}
