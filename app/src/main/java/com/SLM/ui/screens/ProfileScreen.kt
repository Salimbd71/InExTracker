package com.SLM.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.OutlinedButton
import androidx.core.content.ContextCompat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.SLM.BuildConfig
import com.SLM.R
import com.SLM.auth.AuthState
import com.SLM.ui.components.PremiumAppHeader
import com.SLM.ui.components.SyncStatusChip
import com.SLM.ui.components.UserAvatar
import com.SLM.ui.i18n.LocalAppLanguage
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.AccentGold
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.FacebookBlue
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.theme.PrimaryGreen
import com.SLM.ui.theme.WhatsAppGreen
import com.SLM.ui.viewmodel.MainViewModel
import com.SLM.utils.DateUtils
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToLegal: (Int) -> Unit = {},
    onNavigateToRecurring: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val lang = LocalAppLanguage.current
    val coroutineScope = rememberCoroutineScope()
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncManager.syncState.collectAsStateWithLifecycle()
    val syncMeta by viewModel.syncMeta.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val isPinLocked by viewModel.isPinLocked.collectAsStateWithLifecycle()
    val isDailyReminderEnabled by viewModel.isDailyReminderEnabled.collectAsStateWithLifecycle()
    val reminderHour by viewModel.reminderHour.collectAsStateWithLifecycle()
    val reminderMinute by viewModel.reminderMinute.collectAsStateWithLifecycle()
    val isDebtAlertEnabled by viewModel.isDebtAlertEnabled.collectAsStateWithLifecycle()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.setDailyReminder(true)
        } else {
            Toast.makeText(context, strings.notificationPermissionNeeded, Toast.LENGTH_LONG).show()
        }
    }

    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSetupHelpDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var isDeletingAccount by remember { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }

    val authProfile = (authState as? AuthState.Authenticated)?.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        PremiumAppHeader(
            title = strings.profileSettingsTitle,
            subtitle = "InEx Tracker v${BuildConfig.VERSION_NAME}",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 48.dp, top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "profile_user_card") {
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
                            UserAvatar(
                                photoUrl = authProfile?.photoUrl,
                                displayName = authProfile?.displayName ?: strings.guestUserTitle,
                                size = 56.dp,
                                ringColor = PrimaryGreen.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                if (authProfile != null) {
                                    Text(
                                        text = authProfile.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = authProfile.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = strings.guestUserTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.guestOfflineSubtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (authProfile != null && !authProfile.isAnonymous) {
                            Button(
                                onClick = {
                                    viewModel.signOut()
                                    onNavigateToLogin()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.14f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("logout_button")
                            ) {
                                Text(strings.signOutButton, color = ExpenseRed, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSigningIn = true
                                        val result = viewModel.signInWithGoogle(context)
                                        isSigningIn = false
                                        if (result.isFailure) {
                                            Toast.makeText(
                                                context,
                                                "${strings.loginFailedPrefix}: ${result.exceptionOrNull()?.message ?: "Unknown"}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(context, strings.loginSuccessToast, Toast.LENGTH_SHORT).show()
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
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text(strings.signInWithGoogleBackup, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "cloud_sync_card") {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.cloudSyncTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = syncMeta?.let { "${strings.lastSyncPrefix}: ${DateUtils.formatDateTime(it.lastSyncTimestamp, lang)}" }
                                            ?: strings.syncStatusReady,
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
                            Text(strings.syncNowButton, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item(key = "app_settings_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.appSettingsTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Language Switcher (Dual Language: Bengali / English)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.languageLabel)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = language == "bn",
                                    onClick = { viewModel.setLanguage("bn") },
                                    label = { Text(strings.languageBengali, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.testTag("language_bn_chip")
                                )
                                FilterChip(
                                    selected = language == "en",
                                    onClick = { viewModel.setLanguage("en") },
                                    label = { Text(strings.languageEnglish, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryGreen.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.testTag("language_en_chip")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dark Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DarkMode, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.darkModeLabel)
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Currency Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCurrencyDialog = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.currencySymbolLabel)
                            }
                            Text(currencySymbol, fontWeight = FontWeight.Bold, color = PrimaryGreen, fontSize = 18.sp)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // PIN Lock Setup
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPinDialog = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.appPinLockLabel)
                                    Text(
                                        text = if (isPinLocked) strings.lockEnabledLabel else strings.lockDisabledLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPinLocked) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = if (isPinLocked) strings.changeLabel else strings.setupAction,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Recurring Transactions shortcut
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRecurring() }
                                .padding(vertical = 6.dp)
                                .testTag("profile_recurring_row"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Repeat, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.recurringTitle)
                                    Text(
                                        text = strings.recurringSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Reminders & Overdue Debt Auto-Alerts Card
            item(key = "reminders_alerts_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.remindersAlertsTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Daily Reminder Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(strings.dailyReminderLabel, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = strings.dailyReminderSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isDailyReminderEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        viewModel.setDailyReminder(false)
                                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.setDailyReminder(true)
                                    }
                                }
                            )
                        }

                        // Reminder Time Picker
                        val formattedTime = remember(reminderHour, reminderMinute) {
                            val isPm = reminderHour >= 12
                            val h12 = when (val h = reminderHour % 12) {
                                0 -> 12
                                else -> h
                            }
                            String.format(java.util.Locale.US, "%02d:%02d %s", h12, reminderMinute, if (isPm) "PM" else "AM")
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, m ->
                                            viewModel.setDailyReminder(isDailyReminderEnabled, h, m)
                                        },
                                        reminderHour,
                                        reminderMinute,
                                        false
                                    ).show()
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(strings.reminderTimeLabel)
                            }
                            Text(
                                text = formattedTime,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Debt Auto-Alert Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.debtAutoAlertLabel, fontWeight = FontWeight.Medium)
                                Text(
                                    text = strings.debtAutoAlertSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isDebtAlertEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    viewModel.setDebtAutoAlert(enabled)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.sendTestReminderNow()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(strings.testReminderAction, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Legal & Play Store Compliance Card
            item(key = "legal_compliance_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(strings.legalComplianceTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLegal(0) }
                                .padding(vertical = 8.dp)
                                .testTag("privacy_policy_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PrivacyTip, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.privacyPolicyTitle, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        strings.privacyPolicySubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLegal(1) }
                                .padding(vertical = 8.dp)
                                .testTag("terms_conditions_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Gavel, contentDescription = null, tint = PrimaryGreen)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.termsConditionsTitle, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        strings.termsConditionsSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteAccountDialog = true }
                                .padding(vertical = 8.dp)
                                .testTag("delete_account_row"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = ExpenseRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(strings.deleteAccountTitle, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                                    Text(
                                        strings.deleteAccountSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = ExpenseRed)
                        }
                    }
                }
            }

            item(key = "cloud_help_card") {
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
                            Text(strings.cloudGuideTitle, fontWeight = FontWeight.Bold)
                            Text(strings.cloudGuideSubtitle, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Dynamic Auto-Versioned Footer with Developer Signature
            item(key = "app_version_footer") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "InEx Tracker v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Developer: SLM Technology",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FooterIconAction(
                            tint = PrimaryGreen,
                            contentDescription = stringResource(R.string.cd_call_developer),
                            testTag = "footer_call_button",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:01737462871"))
                                    )
                                }.onFailure {
                                    Toast.makeText(context, strings.actionNotSupportedToast, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        FooterIconAction(
                            tint = WhatsAppGreen,
                            contentDescription = stringResource(R.string.cd_whatsapp_developer),
                            testTag = "footer_whatsapp_button",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801737462871"))
                                    )
                                }.onFailure {
                                    Toast.makeText(context, strings.actionNotSupportedToast, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_whatsapp),
                                contentDescription = null,
                                tint = WhatsAppGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        FooterIconAction(
                            tint = FacebookBlue,
                            contentDescription = stringResource(R.string.cd_facebook_developer),
                            testTag = "footer_facebook_button",
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://fb.com/salim.naogaon"))
                                    )
                                }.onFailure {
                                    Toast.makeText(context, strings.actionNotSupportedToast, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_facebook),
                                contentDescription = null,
                                tint = FacebookBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        FooterIconAction(
                            tint = AccentGold,
                            contentDescription = stringResource(R.string.cd_share_app),
                            testTag = "footer_share_button",
                            onClick = {
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, strings.appName)
                                        putExtra(Intent.EXTRA_TEXT, strings.shareAppMessage)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, strings.shareAppChooserTitle)
                                    )
                                }.onFailure {
                                    Toast.makeText(context, strings.actionNotSupportedToast, Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = AccentGold,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = strings.footerSecurityNote,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text(strings.setupPinTitle) },
            text = {
                Column {
                    Text(strings.setupPinDesc, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                        label = { Text(strings.fourDigitPinLabel) },
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
                    Text(strings.saveAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text(strings.cancelAction)
                }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(strings.selectCurrencyTitle) },
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
                                    "৳" -> if (lang == "bn") "টাকা (BDT)" else "Bangladeshi Taka (BDT)"
                                    "$" -> if (lang == "bn") "ডলার (USD)" else "US Dollar (USD)"
                                    "₹" -> if (lang == "bn") "রুপি (INR)" else "Indian Rupee (INR)"
                                    "€" -> if (lang == "bn") "ইউরো (EUR)" else "Euro (EUR)"
                                    "£" -> if (lang == "bn") "পাউন্ড (GBP)" else "British Pound (GBP)"
                                    "AED" -> if (lang == "bn") "দিরহাম (AED)" else "UAE Dirham (AED)"
                                    "SAR" -> if (lang == "bn") "রিয়াল (SAR)" else "Saudi Riyal (SAR)"
                                    else -> if (lang == "bn") "রিংগিত (MYR)" else "Malaysian Ringgit (MYR)"
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(strings.closeLabel)
                }
            }
        )
    }

    if (showSetupHelpDialog) {
        AlertDialog(
            onDismissRequest = { showSetupHelpDialog = false },
            title = { Text(strings.cloudGuideTitle) },
            text = { Text(strings.cloudGuideDialogBody) },
            confirmButton = {
                Button(onClick = { showSetupHelpDialog = false }) {
                    Text(strings.understoodAction)
                }
            }
        )
    }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { if (!isDeletingAccount) showDeleteAccountDialog = false },
            title = { Text(strings.deleteAccountConfirmTitle, color = ExpenseRed, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.deleteAccountConfirmBody, style = MaterialTheme.typography.bodyMedium)
                    if (isDeletingAccount) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ExpenseRed)
                            Text(if (lang == "bn") "মুছে ফেলা হচ্ছে…" else "Deleting account & data…")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isDeletingAccount) return@Button
                        isDeletingAccount = true
                        viewModel.deleteAccountAndData { success, reauthRequired, error ->
                            isDeletingAccount = false
                            showDeleteAccountDialog = false
                            if (success) {
                                Toast.makeText(context, strings.deleteAccountSuccess, Toast.LENGTH_LONG).show()
                                onNavigateToLogin()
                            } else if (reauthRequired) {
                                Toast.makeText(context, strings.deleteAccountReauthRequired, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, error ?: "Error", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isDeletingAccount,
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
                ) {
                    Text(strings.deleteAction, color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteAccountDialog = false },
                    enabled = !isDeletingAccount
                ) {
                    Text(strings.cancelAction)
                }
            }
        )
    }
}

@Composable
private fun FooterIconAction(
    tint: Color,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = tint.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .semantics { this.contentDescription = contentDescription }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
