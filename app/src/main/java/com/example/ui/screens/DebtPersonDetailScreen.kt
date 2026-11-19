package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.DebtPaymentEntity
import com.example.data.local.entity.DebtType
import com.example.data.local.entity.PaymentMethod
import com.example.data.repository.DebtWithPayments
import com.example.ui.components.AdMobBannerView
import com.example.ui.theme.DebtPayable
import com.example.ui.theme.DebtReceivable
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.CurrencyUtils
import com.example.utils.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtPersonDetailScreen(
    debtId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditDebt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allDebtsWithPayments by viewModel.debtsWithPayments.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    val item = allDebtsWithPayments.find { it.debt.id == debtId }
    if (item == null) {
        onBack()
        return
    }

    val debt = item.debt
    val isReceivable = debt.debtType == DebtType.RECEIVABLE
    val accentColor = if (isReceivable) DebtReceivable else DebtPayable

    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<DebtPaymentEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        TopAppBar(
            title = { Text(debt.personName, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onEditDebt) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        AdMobBannerView()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile & Contact Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = debt.personName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = debt.personName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (debt.phoneNumber.isNotBlank()) debt.phoneNumber else "কোন ফোন নম্বর নেই",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Call button if phone exists
                            if (debt.phoneNumber.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${debt.phoneNumber}"))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call", tint = IncomeGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Share Reminder SMS / WhatsApp Button
                        Button(
                            onClick = {
                                val message = if (isReceivable) {
                                    "প্রিয় ${debt.personName}, আপনার কাছে ${CurrencyUtils.formatAmount(item.remainingAmount, currencySymbol)} পাওনা আছে। অনুগ্রহ করে পরিশোধ করুন। ধন্যবাদ।"
                                } else {
                                    "প্রিয় ${debt.personName}, আপনার কাছে আমার ${CurrencyUtils.formatAmount(item.remainingAmount, currencySymbol)} দেনা আছে। আমি শীঘ্রই পরিশোধ করবো। ধন্যবাদ।"
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "তাগাদা বার্তা পাঠান"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("তাগাদা বার্তা বা রসিদ পাঠান", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            // Financial Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("মোট পরিমাণ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(debt.totalAmount, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("পরিশোধ হয়েছে", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(item.totalPaid, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("বাকি পরিমাণ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(item.remainingAmount, currencySymbol),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = accentColor
                                )
                            }
                        }

                        if (debt.dueDate != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "পরিশোধের শেষ তারিখ: ${DateUtils.formatDate(debt.dueDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (debt.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "নোট: ${debt.note}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Add Payment Button
                        Button(
                            onClick = { showAddPaymentDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_debt_payment_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("জমা / পরিশোধ এন্ট্রি দিন (Add Payment)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Payment History Header
            item {
                Text(
                    text = "পরিশোধের ইতিহাস (Payment History)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Payment History Items
            if (item.payments.isEmpty()) {
                item {
                    Text(
                        text = "এখনও কোন টাকা পরিশোধ করা হয়নি",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(item.payments) { pmt ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = CurrencyUtils.formatAmount(pmt.amount, currencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                    Text(
                                        text = "${DateUtils.formatDate(pmt.date)} • ${pmt.paymentMethod.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (pmt.note.isNotBlank()) {
                                        Text(
                                            text = pmt.note,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            IconButton(onClick = { paymentToDelete = pmt }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Payment", tint = ExpenseRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Payment Bottom Sheet
    if (showAddPaymentDialog) {
        AddPaymentSheet(
            debtWithPayments = item,
            currencySymbol = currencySymbol,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { amount, date, method, note ->
                viewModel.addDebtPayment(debt.id, amount, date, note, method)
                showAddPaymentDialog = false
            }
        )
    }

    // Delete Debt Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("খাতা রেকর্ড মুছবেন?") },
            text = { Text("${debt.personName} এর এই খাতা এন্ট্রি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debt.id)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("মুছুন", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Payment Confirmation
    paymentToDelete?.let { pmt ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text("পেমেন্ট রেকর্ড মুছবেন?") },
            text = { Text("${pmt.amount} টাকার পরিশোধ রেকর্ড মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebtPayment(pmt.id)
                        paymentToDelete = null
                    }
                ) {
                    Text("মুছুন", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentSheet(
    debtWithPayments: DebtWithPayments,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, date: Long, method: PaymentMethod, note: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf(if (debtWithPayments.remainingAmount % 1.0 == 0.0) debtWithPayments.remainingAmount.toInt().toString() else debtWithPayments.remainingAmount.toString()) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "পরিশোধ / জমা এন্ট্রি দিন",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${debtWithPayments.debt.personName} • বাকি: ${CurrencyUtils.formatAmount(debtWithPayments.remainingAmount, currencySymbol)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("জমা টাকার পরিমাণ") },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_payment_amount_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("পেমেন্ট মাধ্যম", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PaymentMethod.values()) { pm ->
                    FilterChip(
                        selected = selectedMethod == pm,
                        onClick = { selectedMethod = pm },
                        label = { Text(pm.name) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("নোট (ঐচ্ছিক)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        errorMessage = "সঠিক টাকার পরিমাণ লিখুন"
                        return@Button
                    }
                    onSave(amount, System.currentTimeMillis(), selectedMethod, noteText)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_debt_payment_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("জমা নিশ্চিত করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
