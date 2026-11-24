package com.SLM.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.ads.AdManager
import com.SLM.data.local.entity.PaymentMethod
import com.SLM.data.local.entity.TransactionEntity
import com.SLM.data.local.entity.TransactionType
import com.SLM.ui.components.BottomSheetKeyboardBackHandler
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.utils.DateUtils
import com.SLM.utils.findActivity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    viewModel: MainViewModel,
    transactionToEdit: TransactionEntity? = null,
    initialType: TransactionType = TransactionType.EXPENSE,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(transactionToEdit?.type ?: initialType) }
    var amountText by remember {
        mutableStateOf(
            transactionToEdit?.amount?.let {
                if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
            } ?: ""
        )
    }
    var selectedCategory by remember { mutableStateOf(transactionToEdit?.category ?: "") }
    var selectedDate by remember { mutableLongStateOf(transactionToEdit?.date ?: System.currentTimeMillis()) }
    var selectedPaymentMethod by remember { mutableStateOf(transactionToEdit?.paymentMethod ?: PaymentMethod.CASH) }
    var noteText by remember { mutableStateOf(transactionToEdit?.note ?: "") }

    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
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
                    text = if (transactionToEdit != null) strings.editTransactionTitle else strings.addTransactionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.closeLabel)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                            errorMessage = null
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.income,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == TransactionType.INCOME) IncomeGreen else Color.Gray
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = {
                        if (selectedType != TransactionType.EXPENSE) {
                            selectedType = TransactionType.EXPENSE
                            selectedCategory = ""
                            errorMessage = null
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.expense,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedType == TransactionType.EXPENSE) ExpenseRed else Color.Gray
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text(strings.amountInputLabel) },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_amount_input"),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(50, 100, 500, 1000, 5000).forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val current = amountText.toDoubleOrNull() ?: 0.0
                                amountText = (current + preset).toInt().toString()
                            },
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "+$preset",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.selectCategoryLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { showCustomCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(strings.newCategoryAction, style = MaterialTheme.typography.labelMedium)
                }
            }

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
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("category_dropdown_field")
                )

                ExposedDropdownMenu(
                    expanded = categoryDropdownExpanded,
                    onDismissRequest = { categoryDropdownExpanded = false }
                ) {
                    filteredCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = cat.name,
                                    fontWeight = if (selectedCategory == cat.name) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                selectedCategory = cat.name
                                errorMessage = null
                                categoryDropdownExpanded = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.newCategoryAction,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        onClick = {
                            categoryDropdownExpanded = false
                            showCustomCategoryDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = strings.paymentMethodLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = Calendar.getInstance().apply {
                                    set(year, month, day)
                                }
                                selectedDate = newCal.timeInMillis
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
                        Icon(Icons.Default.CalendarToday, contentDescription = strings.datePrefix, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${strings.datePrefix}: ${DateUtils.formatDate(selectedDate, lang)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = strings.changeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text(strings.noteOptionalLabel) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (transactionToEdit != null) {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(transactionToEdit.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = strings.deleteAction, tint = ExpenseRed)
                    }
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMessage = strings.enterValidAmountError
                            return@Button
                        }
                        if (selectedCategory.isBlank()) {
                            errorMessage = strings.selectCategoryError
                            return@Button
                        }

                        if (transactionToEdit != null) {
                            viewModel.updateTransaction(
                                transactionToEdit.copy(
                                    type = selectedType,
                                    amount = amount,
                                    category = selectedCategory,
                                    date = selectedDate,
                                    paymentMethod = selectedPaymentMethod,
                                    note = noteText
                                )
                            )
                        } else {
                            viewModel.addTransaction(
                                type = selectedType,
                                amount = amount,
                                category = selectedCategory,
                                date = selectedDate,
                                note = noteText,
                                paymentMethod = selectedPaymentMethod
                            )
                            AdManager.maybeShowInterstitialAfterSave(context.findActivity())
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_transaction_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(strings.saveAction, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showCustomCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text(strings.newCategoryTitle) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(strings.categoryNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            val hex = if (selectedType == TransactionType.INCOME) "#10B981" else "#EF4444"
                            viewModel.addCustomCategory(newCategoryName.trim(), selectedType, hex)
                            selectedCategory = newCategoryName.trim()
                            newCategoryName = ""
                            showCustomCategoryDialog = false
                        }
                    }
                ) {
                    Text(strings.createAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}
