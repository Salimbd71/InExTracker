package com.SLM.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.data.local.entity.BudgetEntity
import com.SLM.data.local.entity.CategoryEntity
import com.SLM.data.local.entity.TransactionType
import com.SLM.data.repository.BudgetProgress
import com.SLM.ui.components.BottomSheetKeyboardBackHandler
import com.SLM.ui.components.PremiumAppHeader
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.AccentGold
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.theme.PrimaryGreen
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.DateUtils

@Composable
fun BudgetScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val budgetsWithProgress by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val allTransactions by viewModel.allTransactions.collectAsStateWithLifecycle()

    var showAddBudgetSheet by remember { mutableStateOf(false) }
    var preselectedCategoryForSheet by remember { mutableStateOf("") }
    var prefilledLimitForSheet by remember { mutableStateOf("") }
    var budgetToDelete by remember { mutableStateOf<BudgetEntity?>(null) }

    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE }
    }

    val spentByCategoryThisMonth = remember(allTransactions) {
        val (monthStart, monthEnd) = DateUtils.getMonthBounds(0)
        val map = mutableMapOf<String, Double>()
        for (tx in allTransactions) {
            if (tx.type == TransactionType.EXPENSE && tx.date in monthStart..monthEnd) {
                val key = tx.category.lowercase()
                map[key] = (map[key] ?: 0.0) + tx.amount
            }
        }
        map
    }

    val budgetProgressMap = remember(budgetsWithProgress) {
        budgetsWithProgress.associateBy { it.budget.category.lowercase() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = strings.budgetTitle,
            subtitle = DateUtils.formatMonthYear(System.currentTimeMillis(), lang),
            actions = {
                IconButton(
                    onClick = {
                        preselectedCategoryForSheet = ""
                        prefilledLimitForSheet = ""
                        showAddBudgetSheet = true
                    },
                    modifier = Modifier.testTag("add_budget_top_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.setBudgetButton, tint = Color.White)
                }
            }
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (budgetsWithProgress.isNotEmpty()) {
                item(key = "active_budgets_header") {
                    Text(
                        text = strings.activeBudgetsTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(
                    items = budgetsWithProgress,
                    key = { "active_${it.budget.id}" },
                    contentType = { "budget_card" }
                ) { item ->
                    BudgetItemCard(
                        budgetProgress = item,
                        currencySymbol = currencySymbol,
                        onEdit = {
                            preselectedCategoryForSheet = item.budget.category
                            prefilledLimitForSheet = if (item.budget.monthlyLimit % 1.0 == 0.0) {
                                item.budget.monthlyLimit.toInt().toString()
                            } else {
                                item.budget.monthlyLimit.toString()
                            }
                            showAddBudgetSheet = true
                        },
                        onDelete = { budgetToDelete = item.budget }
                    )
                }
            } else {
                item(key = "no_active_budget_banner") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PieChart,
                                        contentDescription = null,
                                        tint = PrimaryGreen
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = strings.noBudgetTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.noBudgetHint,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    preselectedCategoryForSheet = ""
                                    prefilledLimitForSheet = ""
                                    showAddBudgetSheet = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(strings.setBudgetButton)
                            }
                        }
                    }
                }
            }

            // All Expense Categories displayed directly in Budget screen
            item(key = "all_expense_categories_header") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.allExpenseCategoriesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = expenseCategories,
                key = { "cat_${it.id}" },
                contentType = { "expense_cat_row" }
            ) { category ->
                val spent = spentByCategoryThisMonth[category.name.lowercase()] ?: 0.0
                val progress = budgetProgressMap[category.name.lowercase()]
                ExpenseCategoryBudgetRow(
                    category = category,
                    spentThisMonth = spent,
                    budgetProgress = progress,
                    currencySymbol = currencySymbol,
                    onSetOrEditBudget = {
                        preselectedCategoryForSheet = category.name
                        prefilledLimitForSheet = progress?.budget?.monthlyLimit?.let { limit ->
                            if (limit % 1.0 == 0.0) limit.toInt().toString() else limit.toString()
                        } ?: ""
                        showAddBudgetSheet = true
                    }
                )
            }
        }
    }

    if (showAddBudgetSheet) {
        val catNames = remember(expenseCategories) { expenseCategories.map { it.name } }
        AddBudgetSheet(
            categories = catNames,
            initialCategory = preselectedCategoryForSheet,
            initialLimit = prefilledLimitForSheet,
            currencySymbol = currencySymbol,
            onDismiss = { showAddBudgetSheet = false },
            onSave = { category, limit ->
                viewModel.setBudget(category, limit)
                showAddBudgetSheet = false
            }
        )
    }

    budgetToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { budgetToDelete = null },
            title = { Text(strings.deleteBudgetTitle) },
            text = { Text("${b.category} — ${strings.deleteBudgetConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBudget(b.id)
                        budgetToDelete = null
                    }
                ) {
                    Text(strings.deleteAction, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { budgetToDelete = null }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}

@Composable
private fun ExpenseCategoryBudgetRow(
    category: CategoryEntity,
    spentThisMonth: Double,
    budgetProgress: BudgetProgress?,
    currencySymbol: String,
    onSetOrEditBudget: () -> Unit
) {
    val strings = LocalAppStrings.current
    val hasBudget = budgetProgress != null && budgetProgress.budget.monthlyLimit > 0
    val isOver = hasBudget && spentThisMonth > budgetProgress!!.budget.monthlyLimit
    val ratio = if (hasBudget) (spentThisMonth / budgetProgress!!.budget.monthlyLimit).toFloat().coerceIn(0f, 1f) else 0f
    val animatedRatio by animateFloatAsState(
        targetValue = ratio,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "cat_budget_bar_${category.id}"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSetOrEditBudget() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isOver) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = strings.overBudgetLabel,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else if (hasBudget) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.withinBudgetSuffix,
                            tint = IncomeGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${strings.budgetSpentPrefix}: ${CurrencyUtils.formatAmount(spentThisMonth, currencySymbol)}" +
                                if (hasBudget) " / ${CurrencyUtils.formatAmount(budgetProgress!!.budget.monthlyLimit, currencySymbol)}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOver) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (hasBudget) {
                    IconButton(onClick = onSetOrEditBudget) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.setBudgetButton,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onSetOrEditBudget,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.tapToSetBudgetLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (hasBudget) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { animatedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = when {
                        isOver -> ExpenseRed
                        ratio >= 0.8f -> AccentGold
                        else -> IncomeGreen
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun BudgetItemCard(
    budgetProgress: BudgetProgress,
    currencySymbol: String,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val b = budgetProgress.budget
    val pct = budgetProgress.percentageUsed
    val isOverBudget = budgetProgress.spent > b.monthlyLimit
    val isNearLimit = pct >= 0.8f

    val progressColor = when {
        isOverBudget -> ExpenseRed
        isNearLimit -> AccentGold
        else -> IncomeGreen
    }
    val animatedProgress by animateFloatAsState(
        targetValue = pct.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "budget_progress_${b.id}"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isOverBudget) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = strings.overBudgetLabel,
                            tint = ExpenseRed,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column {
                        Text(b.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${strings.budgetLimitPrefix}: ${CurrencyUtils.formatAmount(b.monthlyLimit, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = progressColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${(pct * 100).toInt()}${strings.budgetUsedSuffix}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = progressColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = strings.deleteAction, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.budgetSpentPrefix}: ${CurrencyUtils.formatAmount(budgetProgress.spent, currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOverBudget) {
                        "${strings.overBudgetByPrefix}: ${CurrencyUtils.formatAmount(budgetProgress.spent - b.monthlyLimit, currencySymbol)}"
                    } else {
                        "${strings.budgetRemainingPrefix}: ${CurrencyUtils.formatAmount(budgetProgress.remaining, currencySymbol)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isOverBudget) ExpenseRed else IncomeGreen
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    categories: List<String>,
    initialCategory: String = "",
    initialLimit: String = "",
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (category: String, limit: Double) -> Unit
) {
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var limitText by remember(initialLimit) { mutableStateOf(initialLimit) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        BottomSheetKeyboardBackHandler(
            sheetState = sheetState,
            onDismiss = onDismiss
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = strings.setMonthlyBudgetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = categoryDropdownExpanded,
                onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text(strings.selectCategoryPlaceholder) },
                    label = { Text(strings.selectCategoryLabel) },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded)
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    categories.forEach { catName ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = catName,
                                    fontWeight = if (selectedCategory == catName) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedCategory = catName
                                errorMessage = null
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = limitText,
                onValueChange = {
                    limitText = it
                    errorMessage = null
                },
                label = { Text(strings.monthlyLimitLabel) },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("budget_limit_input"),
                singleLine = true
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val limit = limitText.toDoubleOrNull()
                    if (selectedCategory.isBlank()) {
                        errorMessage = strings.selectCategoryError
                        return@Button
                    }
                    if (limit == null || limit <= 0) {
                        errorMessage = strings.enterValidAmountError
                        return@Button
                    }
                    onSave(selectedCategory, limit)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_budget_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(strings.saveBudgetButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
