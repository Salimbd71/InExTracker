package com.SLM.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.ads.AdManager
import com.SLM.data.local.entity.DebtPaymentEntity
import com.SLM.data.local.entity.DebtType
import com.SLM.data.local.entity.PaymentMethod
import com.SLM.data.repository.DebtWithPayments
import com.SLM.ui.components.AdMobBannerView
import com.SLM.ui.components.BottomSheetKeyboardBackHandler
import com.SLM.ui.components.PremiumAppHeader
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.DebtPayable
import com.SLM.ui.theme.DebtReceivable
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.theme.PrimaryGreen
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.DateUtils
import com.SLM.utils.findActivity

@Composable
fun DebtPersonDetailScreen(
    debtId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEditDebt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
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
            .navigationBarsPadding()
    ) {
        PremiumAppHeader(
            title = debt.personName,
            subtitle = if (isReceivable) strings.receivable else strings.payable,
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
                IconButton(onClick = onEditDebt) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = strings.deleteAction, tint = Color(0xFFFCA5A5))
                }
            }
        )

        AdMobBannerView()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "debt_profile_card") {
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
                                        text = if (debt.phoneNumber.isNotBlank()) debt.phoneNumber else strings.noPhoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

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

                        Button(
                            onClick = {
                                val amtStr = CurrencyUtils.formatAmount(item.remainingAmount, currencySymbol)
                                val message = if (lang == "en") {
                                    if (isReceivable) {
                                        "Hi ${debt.personName}, this is a gentle reminder of the receivable balance of $amtStr. Thank you."
                                    } else {
                                        "Hi ${debt.personName}, I have a pending payable balance of $amtStr with you and will settle it soon. Thank you."
                                    }
                                } else {
                                    if (isReceivable) {
                                        "প্রিয় ${debt.personName}, আপনার কাছে $amtStr পাওনা রয়েছে। অনুগ্রহ করে সুবিধামতো পরিশোধ করুন। ধন্যবাদ।"
                                    } else {
                                        "প্রিয় ${debt.personName}, আপনার কাছে আমার $amtStr দেনা রয়েছে। আমি শীঘ্রই পরিশোধ করবো। ধন্যবাদ।"
                                    }
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, message)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, strings.shareReminderChooser))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.sendReminderButton, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item(key = "debt_summary_card") {
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
                                Text(strings.totalAmountLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(debt.totalAmount, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(strings.paidAmountLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = CurrencyUtils.formatAmount(item.totalPaid, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(strings.remainingAmountLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                text = "${strings.dueDatePrefix}: ${DateUtils.formatDate(debt.dueDate, lang)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (debt.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = debt.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

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
                            Text(strings.addPaymentButton, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item(key = "payment_history_header") {
                Text(
                    text = strings.paymentHistoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (item.payments.isEmpty()) {
                item(key = "no_payments") {
                    Text(
                        text = strings.noPaymentYet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(item.payments, key = { it.id }, contentType = { "debt_payment" }) { pmt ->
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
                                        text = "${DateUtils.formatDate(pmt.date, lang)} • ${pmt.paymentMethod.name}",
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
                                Icon(Icons.Default.Delete, contentDescription = strings.deleteAction, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentSheet(
            debtWithPayments = item,
            currencySymbol = currencySymbol,
            onDismiss = { showAddPaymentDialog = false },
            onSave = { amount, date, method, note ->
                viewModel.addDebtPayment(debt.id, amount, date, note, method)
                AdManager.maybeShowInterstitialAfterSave(context.findActivity())
                showAddPaymentDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(strings.deleteDebtTitle) },
            text = { Text("${debt.personName} — ${strings.deleteDebtConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebt(debt.id)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text(strings.deleteAction, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }

    paymentToDelete?.let { pmt ->
        AlertDialog(
            onDismissRequest = { paymentToDelete = null },
            title = { Text(strings.deletePaymentTitle) },
            text = { Text("${CurrencyUtils.formatAmount(pmt.amount, currencySymbol)} — ${strings.deletePaymentConfirm}") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDebtPayment(pmt.id)
                        paymentToDelete = null
                    }
                ) {
                    Text(strings.deleteAction, color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { paymentToDelete = null }) {
                    Text(strings.cancelAction)
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
    val strings = LocalAppStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember {
        mutableStateOf(
            if (debtWithPayments.remainingAmount % 1.0 == 0.0) debtWithPayments.remainingAmount.toInt().toString()
            else debtWithPayments.remainingAmount.toString()
        )
    }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var noteText by remember { mutableStateOf("") }
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
                text = strings.addPaymentSheetTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${debtWithPayments.debt.personName} • ${strings.remainingAmountLabel}: ${CurrencyUtils.formatAmount(debtWithPayments.remainingAmount, currencySymbol)}",
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
                label = { Text(strings.depositAmountLabel) },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_payment_amount_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(strings.paymentMethodLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(PaymentMethod.entries, key = { it.name }) { pm ->
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
                label = { Text(strings.noteOptionalLabel) },
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
                        errorMessage = strings.enterValidAmountError
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
                Text(strings.confirmPaymentButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
