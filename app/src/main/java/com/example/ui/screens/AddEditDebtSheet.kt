package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import com.example.data.local.entity.DebtEntity
import com.example.data.local.entity.DebtType
import com.example.ui.theme.DebtPayable
import com.example.ui.theme.DebtReceivable
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditDebtSheet(
    viewModel: MainViewModel,
    debtToEdit: DebtEntity? = null,
    initialType: DebtType = DebtType.RECEIVABLE,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf(debtToEdit?.debtType ?: initialType) }
    var personName by remember { mutableStateOf(debtToEdit?.personName ?: "") }
    var phoneNumber by remember { mutableStateOf(debtToEdit?.phoneNumber ?: "") }
    var amountText by remember { mutableStateOf(debtToEdit?.totalAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var selectedDate by remember { mutableLongStateOf(debtToEdit?.date ?: System.currentTimeMillis()) }
    var selectedDueDate by remember { mutableStateOf(debtToEdit?.dueDate) }
    var noteText by remember { mutableStateOf(debtToEdit?.note ?: "") }
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (debtToEdit != null) "খাতা এন্ট্রি এডিট করুন" else "নতুন খাতা এন্ট্রি (দেনা/পাওনা)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Debt Type Tabs (Receivable / Payable)
            TabRow(
                selectedTabIndex = if (selectedType == DebtType.RECEIVABLE) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedType == DebtType.RECEIVABLE,
                    onClick = { selectedType = DebtType.RECEIVABLE },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Handshake, contentDescription = null, tint = DebtReceivable, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("পাওনা টাকা (আমি পাবো)", fontWeight = FontWeight.Bold, color = if (selectedType == DebtType.RECEIVABLE) DebtReceivable else Color.Gray)
                        }
                    }
                )
                Tab(
                    selected = selectedType == DebtType.PAYABLE,
                    onClick = { selectedType = DebtType.PAYABLE },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = DebtPayable, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("দেনা টাকা (দিতে হবে)", fontWeight = FontWeight.Bold, color = if (selectedType == DebtType.PAYABLE) DebtPayable else Color.Gray)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Person Name
            OutlinedTextField(
                value = personName,
                onValueChange = {
                    personName = it
                    errorMessage = null
                },
                label = { Text("ব্যক্তি বা প্রতিষ্ঠানের নাম *") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_person_name_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phone Number
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("ফোন নম্বর (তাগাদা ও কলের জন্য)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_phone_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Amount Field
            OutlinedTextField(
                value = amountText,
                onValueChange = {
                    amountText = it
                    errorMessage = null
                },
                label = { Text("টাকার পরিমাণ (Total Amount) *") },
                prefix = { Text("$currencySymbol ", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_amount_input"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Date Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = Calendar.getInstance().apply { set(year, month, day) }
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
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "লেনদেনের তারিখ: ${DateUtils.formatDate(selectedDate)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text("পরিবর্তন", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Due Date Selection (Optional)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDueDate ?: (System.currentTimeMillis() + 7 * 86400000L)
                        }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = Calendar.getInstance().apply { set(year, month, day) }
                                selectedDueDate = newCal.timeInMillis
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
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Due Date", tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedDueDate?.let { "পরিশোধের শেষ তারিখ: ${DateUtils.formatDate(it)}" } ?: "পরিশোধের শেষ তারিখ (ঐচ্ছিক)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = if (selectedDueDate != null) "পরিবর্তন" else "নির্ধারণ করুন",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("নোট বা পণ্যের বিবরণ (ঐচ্ছিক)") },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = ExpenseRed, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (debtToEdit != null) {
                    Button(
                        onClick = {
                            viewModel.deleteDebt(debtToEdit.id)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                        modifier = Modifier.weight(0.4f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ExpenseRed)
                    }
                }

                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (personName.isBlank()) {
                            errorMessage = "ব্যক্তির নাম লিখুন"
                            return@Button
                        }
                        if (amount == null || amount <= 0) {
                            errorMessage = "সঠিক টাকার পরিমাণ লিখুন"
                            return@Button
                        }

                        if (debtToEdit != null) {
                            viewModel.updateDebt(
                                debtToEdit.copy(
                                    personName = personName.trim(),
                                    phoneNumber = phoneNumber.trim(),
                                    debtType = selectedType,
                                    totalAmount = amount,
                                    date = selectedDate,
                                    dueDate = selectedDueDate,
                                    note = noteText
                                )
                            )
                        } else {
                            viewModel.addDebt(
                                personName = personName.trim(),
                                phoneNumber = phoneNumber.trim(),
                                debtType = selectedType,
                                totalAmount = amount,
                                date = selectedDate,
                                dueDate = selectedDueDate,
                                note = noteText
                            )
                        }
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_debt_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("খাতায় সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
