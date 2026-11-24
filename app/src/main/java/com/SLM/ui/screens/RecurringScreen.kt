package com.SLM.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.data.local.entity.PaymentMethod
import com.SLM.data.local.entity.RecurringFrequency
import com.SLM.data.local.entity.RecurringTransactionEntity
import com.SLM.data.local.entity.TransactionType
import com.SLM.ui.components.BottomSheetKeyboardBackHandler
import com.SLM.ui.components.PremiumAppHeader
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.theme.PrimaryGreen
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.DateUtils
import java.util.Calendar

@Composable
fun RecurringScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val recurringList by viewModel.allRecurring.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<RecurringTransactionEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<RecurringTransactionEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PremiumAppHeader(
            title = strings.recurringTitle,
            subtitle = strings.recurringSubtitle,
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        itemToEdit = null
                        showSheet = true
                    },
                    modifier = Modifier.testTag("add_recurring_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = strings.addRecurringTitle, tint = Color.White)
                }
            }
        )

        if (recurringList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.noRecurringTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.noRecurringHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            itemToEdit = null
                            showSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.addRecurringTitle)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = recurringList,
                    key = { it.id },
                    contentType = { "recurring_card" }
                ) { item ->
                    val isIncome = item.type == TransactionType.INCOME
                    val accent = if (isIncome) IncomeGreen else ExpenseRed
                    val freqLabel = when (item.frequency) {
                        RecurringFrequency.DAILY -> strings.freqDaily
                        RecurringFrequency.WEEKLY -> strings.freqWeekly
                        RecurringFrequency.MONTHLY -> strings.freqMonthly
                        RecurringFrequency.YEARLY -> strings.freqYearly
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                itemToEdit = item
                                showSheet = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(accent.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = accent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.category} • $freqLabel • ${item.paymentMethod.name}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = item.isActive,
                                    onCheckedChange = { active -> viewModel.toggleRecurring(item.id, active) }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = CurrencyUtils.formatAmount(item.amount, currencySymbol),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accent
                                    )
                                    Text(
                                        text = "${strings.nextDueDateLabel}: ${DateUtils.formatDate(item.nextDueDate, lang)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (item.isActive) PrimaryGreen.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (item.isActive) strings.recurringActiveLabel else strings.recurringPausedLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isActive) PrimaryGreen else Color.Gray,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.postRecurringNow(item)
                                            Toast.makeText(context, strings.postedSuccessToast, Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(strings.postNowAction, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(onClick = { itemToDelete = item }) {
                                        Icon(Icons.Default.Delete, contentDescription = strings.deleteAction, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        AddEditRecurringSheet(
            viewModel = viewModel,
            itemToEdit = itemToEdit,
            onDismiss = {
                showSheet = false
                itemToEdit = null
            }
        )
    }

    itemToDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text(strings.deleteTransactionTitle) },
            text = { Text("${rec.title} — ${strings.deleteTransactionConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteRecurring(rec.id)
                        itemToDelete = null
                    }
                ) {
                    Text(strings.deleteAction, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringSheet(
    viewModel: MainViewModel,
    itemToEdit: RecurringTransactionEntity? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var titleText by remember { mutableStateOf(itemToEdit?.title ?: "") }
    var selectedType by remember { mutableStateOf(itemToEdit?.type ?: TransactionType.EXPENSE) }
    var amountText by remember {
        mutableStateOf(
            itemToEdit?.amount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var selectedCategory by remember { mutableStateOf(itemToEdit?.category ?: "") }
    var selectedPaymentMethod by remember { mutableStateOf(itemToEdit?.paymentMethod ?: PaymentMethod.CASH) }
    var selectedFrequency by remember { mutableStateOf(itemToEdit?.frequency ?: RecurringFrequency.MONTHLY) }
    var nextDueDate by remember { mutableLongStateOf(itemToEdit?.nextDueDate ?: System.currentTimeMillis()) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filteredCategories = remember(categories, selectedType) {
        categories.filter { it.type == selectedType }
    }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (itemToEdit != null) strings.editRecurringTitle else strings.addRecurringTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.closeLabel)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            TabRow(
                selectedTabIndex = if (selectedType == TransactionType.INCOME) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = {
                        if (selectedType != TransactionType.INCOME) {
                            selectedType = TransactionType.INCOME
                            selectedCategory = ""
                        }
                    },
                    text = {
                        Text(
                            text = strings.income,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.INCOME) IncomeGreen else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        if (selectedType != TransactionType.EXPENSE) {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = ""
                        }
                    },
                    text = {
                        Text(
                            text = strings.expense,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedType == TransactionType.EXPENSE) ExpenseRed else Color.Gray
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    errorMessage = null
                },
                label = { Text(strings.recurringNameLabel) },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text(strings.amountInputLabel) },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                    filteredCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategory = cat.name
                                errorMessage = null
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(strings.frequencyLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(RecurringFrequency.entries, key = { it.name }) { freq ->
                    val label = when (freq) {
                        RecurringFrequency.DAILY -> strings.freqDaily
                        RecurringFrequency.WEEKLY -> strings.freqWeekly
                        RecurringFrequency.MONTHLY -> strings.freqMonthly
                        RecurringFrequency.YEARLY -> strings.freqYearly
                    }
                    FilterChip(
                        selected = selectedFrequency == freq,
                        onClick = { selectedFrequency = freq },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(strings.paymentMethodLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PaymentMethod.entries, key = { it.name }) { pm ->
                    val label = when (pm) {
                        PaymentMethod.CASH -> strings.walletCash
                        PaymentMethod.BANK -> strings.walletBank
                        PaymentMethod.BKASH -> strings.walletBkash
                        PaymentMethod.NAGAD -> strings.walletNagad
                        PaymentMethod.CARD -> strings.walletCard
                        PaymentMethod.OTHER -> strings.walletOther
                    }
                    FilterChip(
                        selected = selectedPaymentMethod == pm,
                        onClick = { selectedPaymentMethod = pm },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = nextDueDate }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val nextCal = Calendar.getInstance().apply { set(y, m, d) }
                                nextDueDate = nextCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${strings.nextDueDateLabel}: ${DateUtils.formatDate(nextDueDate, lang)}")
                    }
                    Text(strings.changeLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (titleText.isBlank()) {
                        errorMessage = strings.enterTitleError
                        return@Button
                    }
                    if (amount == null || amount <= 0) {
                        errorMessage = strings.enterValidAmountError
                        return@Button
                    }
                    if (selectedCategory.isBlank()) {
                        errorMessage = strings.selectCategoryError
                        return@Button
                    }
                    if (itemToEdit != null) {
                        viewModel.updateRecurringTransaction(
                            itemToEdit.copy(
                                title = titleText.trim(),
                                type = selectedType,
                                amount = amount,
                                category = selectedCategory,
                                paymentMethod = selectedPaymentMethod,
                                frequency = selectedFrequency,
                                nextDueDate = nextDueDate
                            )
                        )
                    } else {
                        viewModel.addRecurringTransaction(
                            title = titleText.trim(),
                            type = selectedType,
                            amount = amount,
                            category = selectedCategory,
                            paymentMethod = selectedPaymentMethod,
                            frequency = selectedFrequency,
                            nextDueDate = nextDueDate
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text(strings.saveAction, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
