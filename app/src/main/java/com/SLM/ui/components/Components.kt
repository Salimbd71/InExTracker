package com.SLM.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.SLM.ads.AdManager
import com.SLM.data.local.entity.PaymentMethod
import com.SLM.data.local.entity.TransactionEntity
import com.SLM.data.local.entity.TransactionType
import com.SLM.sync.SyncUiState
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.AccentGold
import com.SLM.ui.theme.DebtPayable
import com.SLM.ui.theme.DebtReceivable
import com.SLM.ui.theme.ExpenseRed
import com.SLM.ui.theme.IncomeGreen
import com.SLM.ui.theme.PrimaryGreen
import com.SLM.utils.CurrencyUtils
import com.SLM.utils.ExportUtils
import com.SLM.utils.findActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Production-grade emerald gradient header with subtle layered geometric glow,
 * edge-to-edge status bar inset handling, and crisp white typography.
 */
@Composable
fun PremiumAppHeader(
    title: String,
    subtitle: String? = null,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f || isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF071B1E), Color(0xFF0D2F33), Color(0xFF114044))
    } else {
        listOf(Color(0xFF09453F), Color(0xFF0F766E), Color(0xFF115E59))
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        shadowElevation = 6.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(colors = gradientColors))
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.07f),
                        radius = size.height * 0.85f,
                        center = Offset(size.width * 0.92f, size.height * 0.15f)
                    )
                    drawCircle(
                        color = Color(0xFF2DD4BF).copy(alpha = 0.09f),
                        radius = size.height * 0.55f,
                        center = Offset(size.width * 0.72f, size.height * 0.95f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.04f),
                        radius = size.height * 0.45f,
                        center = Offset(size.width * 0.12f, size.height * 0.85f)
                    )
                }
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (navigationIcon != null) {
                        navigationIcon()
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.82f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
        }
    }
}

/**
 * Displays Google Profile Photo via Coil AsyncImage with a crisp circular ring
 * and clean initial-letter fallback when offline or guest.
 */
@Composable
fun UserAvatar(
    photoUrl: String?,
    displayName: String,
    size: Dp = 36.dp,
    ringColor: Color = Color.White.copy(alpha = 0.85f),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val initial = displayName.trim().firstOrNull()?.uppercase() ?: ""

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PrimaryGreen.copy(alpha = 0.25f))
            .border(1.5.dp, ringColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (initial.isNotBlank()) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.42f).sp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = displayName,
                tint = Color.White,
                modifier = Modifier.size(size * 0.58f)
            )
        }

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = remember(photoUrl) {
                    ImageRequest.Builder(context)
                        .data(photoUrl)
                        .crossfade(true)
                        .build()
                },
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
    }
}

/**
 * Intercepts hardware/gesture Back press inside ModalBottomSheet when
 * ModalBottomSheetProperties(shouldDismissOnBackPress = false) is active:
 * - First Back press dismisses only the soft keyboard (if open)
 * - Second Back press (when keyboard is closed) hides and dismisses the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BottomSheetKeyboardBackHandler(
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    val isImeVisible = WindowInsets.isImeVisible
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    var imeJustClosed by remember { mutableStateOf(false) }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible) {
            imeJustClosed = true
        } else if (imeJustClosed) {
            delay(250)
            imeJustClosed = false
        }
    }

    BackHandler(enabled = true) {
        if (isImeVisible || imeJustClosed) {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        } else {
            coroutineScope.launch {
                runCatching { sheetState.hide() }
                onDismiss()
            }
        }
    }
}

/**
 * Floating frosted-glass Quick Add menu without blurring the screen behind it.
 */
@Composable
fun GlassSpeedDialOverlay(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddDebt: () -> Unit
) {
    val strings = LocalAppStrings.current
    if (expanded) {
        BackHandler(enabled = true) { onDismiss() }
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(200)),
        exit = fadeOut(tween(160))
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            animationSpec = tween(250),
            label = "overlay_fab_rot"
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.20f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 148.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn(tween(200, 30)) + slideInVertically(tween(220)) { it / 3 } + scaleIn(tween(220), initialScale = 0.88f),
                    exit = fadeOut(tween(140)) + slideOutVertically { it / 4 } + scaleOut(targetScale = 0.92f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.25.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.55f),
                                    Color(0xFF2DD4BF).copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.18f)
                                )
                            )
                        ),
                        shadowElevation = 12.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xE00C3531),
                                            Color(0xCC072420),
                                            Color(0xE60F3F39)
                                        )
                                    )
                                )
                                .padding(14.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = strings.quickAddSubtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.88f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            GlassActionPill(
                                label = strings.quickAddIncome,
                                icon = Icons.Default.ArrowDownward,
                                accentColor = IncomeGreen,
                                tag = "glass_add_income_btn",
                                onClick = {
                                    onDismiss()
                                    onAddIncome()
                                }
                            )
                            GlassActionPill(
                                label = strings.quickAddExpense,
                                icon = Icons.Default.ArrowUpward,
                                accentColor = ExpenseRed,
                                tag = "glass_add_expense_btn",
                                onClick = {
                                    onDismiss()
                                    onAddExpense()
                                }
                            )
                            GlassActionPill(
                                label = strings.quickAddDebt,
                                icon = Icons.Default.Handshake,
                                accentColor = Color(0xFF14B8A6),
                                tag = "glass_add_debt_btn",
                                onClick = {
                                    onDismiss()
                                    onAddDebt()
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xBD092926),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = strings.tapOutsideToClose,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    FloatingActionButton(
                        onClick = onDismiss,
                        containerColor = Color(0xCC0F3E38),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .border(1.2.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                            .testTag("glass_speed_dial_close_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.closeLabel,
                            modifier = Modifier
                                .size(28.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassActionPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
        shadowElevation = 0.dp,
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Reusable PDF generation, Downloads saving, and Sharing dialog.
 * Triggers Interstitial ad politely upon export completion.
 */
@Composable
fun PdfExportDialog(
    reportTitle: String,
    suggestedFileName: String,
    generatePdf: suspend () -> File?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var isGenerating by remember { mutableStateOf(true) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var savedUri by remember { mutableStateOf<Uri?>(null) }
    var failed by remember { mutableStateOf(false) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { targetUri ->
        val src = generatedFile
        if (targetUri != null && src != null) {
            scope.launch(Dispatchers.IO) {
                val ok = ExportUtils.writeFileToUri(context, src, targetUri)
                withContext(Dispatchers.Main) {
                    if (ok) {
                        savedUri = targetUri
                        Toast.makeText(context, strings.pdfSavedSuccess, Toast.LENGTH_SHORT).show()
                        context.findActivity()?.let { AdManager.showInterstitial(it) }
                    } else {
                        Toast.makeText(context, strings.pdfExportFailed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isGenerating = true
        failed = false
        val file = withContext(Dispatchers.IO) { generatePdf() }
        generatedFile = file
        isGenerating = false
        failed = file == null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = strings.pdfExportOptionsTitle,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = reportTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isGenerating) {
                    CircularProgressIndicator(
                        color = PrimaryGreen,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    Text(strings.pdfGenerating, style = MaterialTheme.typography.bodySmall)
                } else if (failed || generatedFile == null) {
                    Text(
                        text = strings.pdfExportFailed,
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    if (savedUri != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = IncomeGreen.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    text = strings.pdfSavedSuccess,
                                    color = IncomeGreen,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val src = generatedFile ?: return@Button
                            scope.launch(Dispatchers.IO) {
                                val uri = ExportUtils.savePdfToDownloads(context, src)
                                withContext(Dispatchers.Main) {
                                    if (uri != null) {
                                        savedUri = uri
                                        Toast.makeText(context, strings.pdfSavedSuccess, Toast.LENGTH_SHORT).show()
                                        context.findActivity()?.let { AdManager.showInterstitial(it) }
                                    } else {
                                        createDocumentLauncher.launch(suggestedFileName)
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_pdf_device_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.savePdfToDeviceButton, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val src = generatedFile ?: return@OutlinedButton
                            val activity = context.findActivity()
                            if (activity != null) {
                                AdManager.showInterstitial(activity) {
                                    ExportUtils.sharePdfFile(context, src, reportTitle)
                                }
                            } else {
                                ExportUtils.sharePdfFile(context, src, reportTitle)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_pdf_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.sharePdfButton, fontWeight = FontWeight.Bold)
                    }

                    TextButton(
                        onClick = {
                            val src = generatedFile ?: return@TextButton
                            ExportUtils.openPdfFile(context, src, reportTitle)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (strings.langCode == "bn") "PDF প্রিভিউ দেখুন" else "Open PDF Viewer")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.closeLabel, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun SyncStatusChip(
    syncState: SyncUiState,
    onSyncClick: () -> Unit,
    onLightHeader: Boolean = false,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val (label, icon, baseColor) = when (syncState) {
        SyncUiState.SYNCED -> Triple(strings.syncSynced, Icons.Default.CloudDone, if (onLightHeader) Color(0xFF99F6E4) else IncomeGreen)
        SyncUiState.SYNCING -> Triple(strings.syncSyncing, Icons.Default.Sync, if (onLightHeader) Color(0xFFFDE68A) else AccentGold)
        SyncUiState.OFFLINE -> Triple(strings.syncOffline, Icons.Default.CloudOff, if (onLightHeader) Color(0xFFE2E8F0) else Color.Gray)
        SyncUiState.ERROR -> Triple(strings.syncFailed, Icons.Default.Warning, if (onLightHeader) Color(0xFFFECACA) else ExpenseRed)
        SyncUiState.IDLE -> Triple(strings.syncReady, Icons.Default.CloudDone, if (onLightHeader) Color(0xFF99F6E4) else IncomeGreen)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSyncClick() }
            .testTag("sync_status_chip"),
        color = if (onLightHeader) Color.White.copy(alpha = 0.16f) else baseColor.copy(alpha = 0.15f),
        border = if (onLightHeader) BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)) else null,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (syncState == SyncUiState.SYNCING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = baseColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(14.dp),
                    tint = baseColor
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = baseColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FinancialSummaryCard(
    currentBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    totalReceivable: Double,
    totalPayable: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("financial_summary_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = strings.currentBalance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = CurrencyUtils.formatAmount(currentBalance, currencySymbol),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentBalance >= 0) MaterialTheme.colorScheme.primary else ExpenseRed
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = IncomeGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(IncomeGreen.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = strings.income,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.income,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (totalIncome >= 10_000_000.0) {
                                    CurrencyUtils.formatCompact(totalIncome, currencySymbol)
                                } else {
                                    CurrencyUtils.formatAmount(totalIncome, currencySymbol)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = IncomeGreen,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = ExpenseRed.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(ExpenseRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = strings.expense,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.expense,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (totalExpense >= 10_000_000.0) {
                                    CurrencyUtils.formatCompact(totalExpense, currencySymbol)
                                } else {
                                    CurrencyUtils.formatAmount(totalExpense, currencySymbol)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ExpenseRed,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DebtReceivable.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Handshake,
                                    contentDescription = strings.receivable,
                                    tint = DebtReceivable,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.receivable,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(totalReceivable, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DebtReceivable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = DebtPayable.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = strings.payable,
                                    tint = DebtPayable,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.payable,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = CurrencyUtils.formatAmount(totalPayable, currencySymbol),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = DebtPayable,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletBreakdownRow(
    walletBalances: Map<PaymentMethod, Double>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = strings.walletAndPaymentMethods,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(PaymentMethod.entries, key = { it.name }) { pm ->
                val balance = walletBalances[pm] ?: 0.0
                val (icon, name, color) = when (pm) {
                    PaymentMethod.CASH -> Triple(Icons.Default.Money, strings.walletCash, Color(0xFF10B981))
                    PaymentMethod.BANK -> Triple(Icons.Default.AccountBalance, strings.walletBank, Color(0xFF3B82F6))
                    PaymentMethod.BKASH -> Triple(Icons.Default.PhoneAndroid, strings.walletBkash, Color(0xFFE2136E))
                    PaymentMethod.NAGAD -> Triple(Icons.Default.PhoneAndroid, strings.walletNagad, Color(0xFFF7941D))
                    PaymentMethod.CARD -> Triple(Icons.Default.CreditCard, strings.walletCard, Color(0xFF8B5CF6))
                    PaymentMethod.OTHER -> Triple(Icons.Default.Payment, strings.walletOther, Color(0xFF64748B))
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = name,
                                tint = color,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatAmount(balance, currencySymbol),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else ExpenseRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(
    income: Double,
    expense: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val maxVal = maxOf(income, expense, 1.0)
    val targetIncomeRatio = (income / maxVal).toFloat()
    val targetExpenseRatio = (expense / maxVal).toFloat()

    val animatedIncomeRatio by animateFloatAsState(
        targetValue = targetIncomeRatio,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "income_bar"
    )
    val animatedExpenseRatio by animateFloatAsState(
        targetValue = targetExpenseRatio,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "expense_bar"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_bar_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.monthlyChartTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = CurrencyUtils.formatCompact(income, currencySymbol),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height((animatedIncomeRatio * 84f).coerceAtLeast(10f).dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(IncomeGreen, Color(0xFF059669))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.income,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = CurrencyUtils.formatCompact(expense, currencySymbol),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height((animatedExpenseRatio * 84f).coerceAtLeast(10f).dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(ExpenseRed, Color(0xFFDC2626))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.expense,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    transactions: List<TransactionEntity>,
    type: TransactionType,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val categoryTotals = remember(transactions, type) {
        transactions.filter { it.type == type }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }
    val total = remember(categoryTotals) { categoryTotals.sumOf { it.second } }

    val sweepProgress = remember { Animatable(0f) }
    LaunchedEffect(categoryTotals) {
        sweepProgress.snapTo(0f)
        sweepProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_chart"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (type == TransactionType.EXPENSE) strings.expenseByCategory else strings.incomeByCategory,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noDataInPeriod,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val chartColors = remember {
                    listOf(
                        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
                        Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6),
                        Color(0xFFEC4899), Color(0xFF64748B)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(134.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val animRatio = sweepProgress.value
                            categoryTotals.forEachIndexed { index, pair ->
                                val sweep = if (total > 0) (pair.second / total * 360f).toFloat() * animRatio else 0f
                                val color = chartColors[index % chartColors.size]
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = 24.dp.toPx(), cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = strings.totalLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyUtils.formatCompact(total, currencySymbol),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryTotals.take(4).forEachIndexed { index, pair ->
                            val color = chartColors[index % chartColors.size]
                            val pct = if (total > 0) (pair.second / total * 100).toInt() else 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = pair.first,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdMobBannerView(
    modifier: Modifier = Modifier
) {
    val isAdsEnabled by AdManager.isAdsEnabled.collectAsStateWithLifecycle(initialValue = true)
    if (!isAdsEnabled) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = AdManager.BANNER_AD_UNIT_ID
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { adView ->
                adView.destroy()
            }
        )
    }
}
