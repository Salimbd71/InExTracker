package com.example.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.auth.AuthState
import com.example.sync.SyncUiState
import com.example.ui.components.AdMobBannerView
import com.example.ui.components.SyncStatusChip
import com.example.ui.theme.AccentGold
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncManager.syncState.collectAsStateWithLifecycle()
    val syncMeta by viewModel.syncMeta.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isPinLocked by viewModel.isPinLocked.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSetupHelpDialog by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "প্রোফাইল ও সেটিংস (Settings)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        AdMobBannerView()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // User Card
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                when (val state = authState) {
                                    is AuthState.Authenticated -> {
                                        Text(
                                            text = state.user.displayName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = state.user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = "গেস্ট ব্যবহারকারী (Guest User)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "অফলাইন ডেটা মোড সক্রিয়",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (authState is AuthState.Authenticated) {
                            Button(
                                onClick = {
                                    viewModel.signOut()
                                    onNavigateToLogin()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("logout_button")
                            ) {
                                Text("লগআউট করুন (Sign Out)", color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSigningIn = true
                                        val result = viewModel.signInWithGoogle()
                                        isSigningIn = false
                                        if (result.isFailure) {
                                            Toast.makeText(context, "গুগল সাইন-ইন সম্পন্ন করতে আপনার ডিভাইসে অ্যাকাউন্ট নির্বাচন করুন", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("google_sign_in_button"),
                                enabled = !isSigningIn
                            ) {
                                if (isSigningIn) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Text("গুগল দিয়ে সাইন-ইন ও ব্যাকআপ", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Cloud Sync & Backup Card
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("ক্লাউড সিঙ্ক ও ব্যাকআপ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = syncMeta?.let { "সর্বশেষ সিঙ্ক: ${DateUtils.formatDateTime(it.lastSyncTimestamp)}" } ?: "সিঙ্ক স্ট্যাটাস: প্রস্তুত",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            SyncStatusChip(
                                syncState = syncState,
                                onSyncClick = { viewModel.triggerSync() }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.triggerSync() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("এখনই সিঙ্ক করুন (Sync Now)", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Preferences Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("অ্যাপ সেটিংস", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("ডার্ক মোড (Dark Theme)")
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Currency Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCurrencyDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("মুদ্রার প্রতীক (Currency Symbol)")
                            }
                            Text(currencySymbol, fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // PIN Lock Setup
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("অ্যাপ পিন লক (PIN Security)")
                                    Text(
                                        text = if (isPinLocked) "লক চালু রয়েছে" else "লক বন্ধ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPinLocked) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (isPinLocked) "পরিবর্তন" else "সেট করুন",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // Firebase & Help Info
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSetupHelpDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = PrimaryGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("ক্লাউড ব্যাকআপ গাইড ও তথ্য", fontWeight = FontWeight.Bold)
                            Text("ফায়ারবেস ক্লাউড ডেটা সিকিউরিটি ও অফলাইন কাজ করার নিয়মাবলী", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // App Version
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "হিসাব খাতা • Hisab Khata v1.0.0",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "100% সুরক্ষিত • অফলাইন ও ক্লাউড সিঙ্ক সমৃদ্ধ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }

    // PIN Setup Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("৪ ডিজিটের পিন কোড সেট করুন") },
            text = {
                Column {
                    Text("অ্যাপ খোলার সময় এই পিন কোড প্রয়োজন হবে। বন্ধ করতে খালি রেখে সংরক্ষণ করুন।", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text("৪ ডিজিটের পিন") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setPinCode(if (pinInput.length == 4) pinInput else null)
                        showPinDialog = false
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("মুদ্রার প্রতীক নির্বাচন করুন") },
            text = {
                Column {
                    listOf("৳", "$", "₹", "€", "£", "AED", "SAR", "MYR").forEach { symbol ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrency(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(symbol, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryGreen)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                when (symbol) {
                                    "৳" -> "টাকা (BDT)"
                                    "$" -> "ডলার (USD)"
                                    "₹" -> "রুপি (INR)"
                                    "€" -> "ইউরো (EUR)"
                                    "£" -> "পাউন্ড (GBP)"
                                    "AED" -> "দিরহাম (AED)"
                                    "SAR" -> "রিয়াল (SAR)"
                                    else -> "রিংগিত (MYR)"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }

    // Help Dialog
    if (showSetupHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSetupHelpDialog = false },
            title = { Text("হিসাব খাতা ক্লাউড সিঙ্ক") },
            text = {
                Text("১. হিসাব খাতা অফলাইন ফার্স্ট অ্যাপ্লিকেশন। ইন্টারনেট সংযোগ না থাকলেও আপনি নিরবিচ্ছিন্নভাবে আয়, ব্যয় ও দেনা-পাওনা লিখতে পারবেন।\n\n২. গুগল অ্যাকাউন্ট দিয়ে সাইন ইন করলে ইন্টারনেট পাওয়া মাত্র স্বয়ংক্রিয়ভাবে ক্লাউডে ব্যাকআপ সংরক্ষিত হবে।\n\n৩. আপনার ডিভাইস হারালেও বা নতুন ডিভাইসে লগইন করলে সকল ডেটা স্বয়ংক্রিয়ভাবে ফিরে আসবে।")
            },
            confirmButton = {
                Button(onClick = { showSetupHelpDialog = false }) {
                    Text("বুঝেছি")
                }
            }
        )
    }
}
