package com.SLM.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.SLM.R
import com.SLM.ui.i18n.LocalAppStrings
import com.SLM.ui.theme.AccentGold
import com.SLM.ui.theme.IncomeGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val density = LocalDensity.current.density
    val prefs = remember(context) {
        context.getSharedPreferences("hisab_khata_settings", Context.MODE_PRIVATE)
    }

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun hasStoragePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

    fun buildPendingPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && !hasStoragePermission()) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    var notifGranted by remember { mutableStateOf(hasNotificationPermission()) }
    var storageGranted by remember { mutableStateOf(hasStoragePermission()) }
    var showPermissionPrompt by remember { mutableStateOf(false) }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        notifGranted = hasNotificationPermission()
        storageGranted = hasStoragePermission()
        prefs.edit().putBoolean("splash_permissions_ack", true).apply()
        onTimeout()
    }

    val singleNotifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted || hasNotificationPermission()
    }

    val singleStorageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storageGranted = granted || hasStoragePermission()
    }

    // 3D Entrance Animatables
    val introScale = remember { Animatable(0.32f) }
    val introAlpha = remember { Animatable(0f) }
    val introFlipY = remember { Animatable(-28f) }

    // Continuous 3D Perspective Float & Orbital Motion
    val infiniteTransition = rememberInfiniteTransition(label = "splash_3d")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )
    val tiltY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltY"
    )
    val tiltX by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tiltX"
    )
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    LaunchedEffect(Unit) {
        launch {
            introAlpha.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
        }
        launch {
            introScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
        }
        launch {
            introFlipY.animateTo(0f, tween(850, easing = FastOutSlowInEasing))
        }
        delay(1250)

        val acked = prefs.getBoolean("splash_permissions_ack", false)
        val pending = buildPendingPermissions()
        if (!acked || pending.isNotEmpty()) {
            showPermissionPrompt = true
        } else {
            delay(350)
            onTimeout()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A102B),
                        Color(0xFF0F2647),
                        Color(0xFF0B353A),
                        Color(0xFF07131E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("splash_screen")
    ) {
        // 3D Ambient Orbital Rings & Glowing Particles Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height * if (showPermissionPrompt) 0.27f else 0.40f
            val rad = Math.toRadians(orbitAngle.toDouble())

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.22f),
                        Color(0xFF10B981).copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = 440f
                ),
                center = Offset(cx, cy),
                radius = 440f
            )

            drawCircle(
                color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                radius = 158f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f)
            )
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.20f),
                radius = 212f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.8f)
            )

            // Orbiting 3D light nodes
            val p1x = cx + (158f * cos(rad)).toFloat()
            val p1y = cy + (158f * sin(rad) * 0.55f).toFloat()
            drawCircle(
                color = AccentGold.copy(alpha = 0.9f),
                radius = 7f,
                center = Offset(p1x, p1y)
            )

            val rad2 = Math.toRadians((-orbitAngle * 0.75 + 120).toDouble())
            val p2x = cx + (212f * cos(rad2)).toFloat()
            val p2y = cy + (212f * sin(rad2) * 0.6f).toFloat()
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = 0.85f),
                radius = 6f,
                center = Offset(p2x, p2y)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // 3D Depth Shadow + Floating Perspective Card
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .alpha(introAlpha.value)
                    .size(134.dp)
            ) {
                // Soft 3D ground shadow ellipse underneath floating icon
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(width = 96.dp, height = 16.dp)
                        .graphicsLayer {
                            val shadowScale = 1f - (floatY / 50f)
                            scaleX = shadowScale
                            scaleY = shadowScale
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.52f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Main 3D Perspective Card
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFF111A41),
                    shadowElevation = 22.dp,
                    border = BorderStroke(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AccentGold.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.55f),
                                Color(0xFF34D399).copy(alpha = 0.75f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .size(108.dp)
                        .graphicsLayer {
                            cameraDistance = 18f * density
                            scaleX = introScale.value
                            scaleY = introScale.value
                            translationY = floatY * density
                            rotationY = introFlipY.value + tiltY
                            rotationX = tiltX
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.inex_icon_3d_v2_1795491417065),
                            contentDescription = "InEx Tracker 3D Emblem",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Subtle top-left 3D glass sheen
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.22f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = strings.appName,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 0.8.sp,
                modifier = Modifier
                    .alpha(introAlpha.value)
                    .testTag("splash_app_name")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.appTagline,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFD1FAE5).copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(introAlpha.value)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.alpha(introAlpha.value)
            ) {
                FeatureBadge(icon = Icons.Default.CloudSync, label = "Cloud Sync")
                FeatureBadge(icon = Icons.Default.Security, label = "Secure")
                FeatureBadge(icon = Icons.Default.TrendingUp, label = "PDF Reports")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3D Glass Permission Prompt Card on Launch
            AnimatedVisibility(
                visible = showPermissionPrompt,
                enter = fadeIn(tween(380)) + slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                ) { it / 3 }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.10f),
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.40f),
                                Color(0xFF34D399).copy(alpha = 0.30f)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                        .testTag("splash_permissions_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = strings.splashPermTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = strings.splashPermSubtitle,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.78f),
                                lineHeight = 16.sp
                            )
                        }

                        PermissionItemRow(
                            icon = Icons.Default.NotificationsActive,
                            title = strings.splashPermNotifTitle,
                            desc = strings.splashPermNotifDesc,
                            isGranted = notifGranted,
                            grantedLabel = strings.splashPermGrantedBadge,
                            allowLabel = strings.splashPermAllowAction,
                            tag = "splash_allow_notif_button",
                            onAllowClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    singleNotifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    notifGranted = true
                                }
                            }
                        )

                        PermissionItemRow(
                            icon = Icons.Default.FolderOpen,
                            title = strings.splashPermStorageTitle,
                            desc = strings.splashPermStorageDesc,
                            isGranted = storageGranted,
                            grantedLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                strings.splashPermScopedReadyBadge
                            } else {
                                strings.splashPermGrantedBadge
                            },
                            allowLabel = strings.splashPermAllowAction,
                            tag = "splash_allow_storage_button",
                            onAllowClick = {
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                    singleStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    storageGranted = true
                                }
                            }
                        )

                        val pending = buildPendingPermissions()
                        Button(
                            onClick = {
                                if (pending.isNotEmpty()) {
                                    multiplePermissionLauncher.launch(pending.toTypedArray())
                                } else {
                                    prefs.edit().putBoolean("splash_permissions_ack", true).apply()
                                    onTimeout()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("splash_allow_all_button")
                        ) {
                            Text(
                                text = if (pending.isNotEmpty()) strings.splashPermAllowAllBtn else strings.splashPermContinueBtn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (pending.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    prefs.edit().putBoolean("splash_permissions_ack", true).apply()
                                    onTimeout()
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .testTag("splash_skip_permissions_button")
                            ) {
                                Text(
                                    text = strings.splashPermSkipBtn,
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.72f)
                                )
                            }
                        }
                    }
                }
            }

            if (!showPermissionPrompt) {
                Spacer(modifier = Modifier.height(20.dp))
                CircularProgressIndicator(
                    color = AccentGold,
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .size(28.dp)
                        .alpha(introAlpha.value)
                )
            }
        }

        if (!showPermissionPrompt) {
            Text(
                text = strings.splashPoweredBy,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.65f),
                fontWeight = FontWeight.Normal,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .alpha(introAlpha.value)
            )
        }
    }
}

@Composable
private fun PermissionItemRow(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    grantedLabel: String,
    allowLabel: String,
    tag: String,
    onAllowClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isGranted) IncomeGreen.copy(alpha = 0.24f) else AccentGold.copy(alpha = 0.22f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) Color(0xFF6EE7B7) else AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = desc,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f),
                    lineHeight = 14.sp
                )
            }

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IncomeGreen.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, Color(0xFF6EE7B7).copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = grantedLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                    }
                }
            } else {
                Button(
                    onClick = onAllowClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentGold,
                        contentColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag(tag)
                ) {
                    Text(
                        text = allowLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
