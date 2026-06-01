package com.example.ui.screens

import android.widget.Toast
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LauncherNote
import com.example.ui.LauncherUiState
import com.example.ui.LauncherViewModel
import com.example.ui.OsApp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NovaHomeLayout(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Battery & System Sync ticks
    LaunchedEffect(Unit) {
        viewModel.loadRealInstalledApps(context)
        while (true) {
            viewModel.loadSystemHealthStats(context)
            delay(15000) // update stats every 15s
        }
    }

    // Dynamic Aesthetic Theme variables
    val currentTheme = getAestheticTheme(uiState.themeIndex)

    // Gesture detection helpers
    var dragAccumulatedX by remember { mutableStateOf(0f) }
    var dragAccumulatedY by remember { mutableStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Draws premium shifting wallpaper gradients based on configured themes
                val brush = Brush.radialGradient(
                    colors = currentTheme.gradientColors,
                    center = Offset(size.width * 0.3f, size.height * 0.2f),
                    radius = size.width * 1.5f
                )
                drawRect(brush)
                
                // Overlay dot grid if Nothing theme
                if (uiState.themeIndex == 0) {
                    val dotColor = Color(0x11FFFFFF)
                    val spacing = 40f
                    var x = 0f
                    while (x < size.width) {
                        var y = 0f
                        while (y < size.height) {
                            drawCircle(dotColor, radius = 2f, center = Offset(x, y))
                            y += spacing
                        }
                        x += spacing
                    }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulatedX = 0f
                        dragAccumulatedY = 0f
                    },
                    onDragEnd = {
                        val threshold = 120f
                        when {
                            dragAccumulatedY < -threshold -> {
                                viewModel.triggerGesture("Swipe Up -> App Drawer")
                            }
                            dragAccumulatedY > threshold -> {
                                viewModel.triggerGesture("Swipe Down -> Control Center")
                            }
                            dragAccumulatedX > threshold -> {
                                viewModel.triggerGesture("Swipe Right -> Widgets Drawer")
                            }
                            dragAccumulatedX < -threshold -> {
                                viewModel.triggerGesture("Swipe Left -> Quick Stats")
                                viewModel.toggleLifeLayer(true)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulatedX += dragAmount.x
                        dragAccumulatedY += dragAmount.y
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        viewModel.triggerGesture("Double Tap -> Life Dashboard")
                    }
                )
            }
    ) {

        // --- BACKGROUND WALLPAPER OR GLASS EFFECT BLUR ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (uiState.appDrawerOpen || uiState.controlCenterOpen || uiState.widgetsPanelOpen || uiState.lifeLayerOpen) 16.dp else 0.dp)
                .alpha(if (uiState.appDrawerOpen || uiState.controlCenterOpen || uiState.widgetsPanelOpen || uiState.lifeLayerOpen) 0.85f else 1f)
        ) {
            // Main Desktop Workspace
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                // Top Custom Status Info & Gesture Log visual notifier (Nothing OS styled)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    Text(
                        text = "NOVA_OS.v1 // $timeString",
                        style = MaterialTheme.typography.labelSmall,
                        color = currentTheme.textSecondary,
                        fontWeight = FontWeight.Bold
                    )

                    // Focus Mode Indicator
                    if (uiState.focusModeActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(currentTheme.accent.copy(alpha = 0.2f))
                                .border(1.dp, currentTheme.accent, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "🎯 FOCUS MODE ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = currentTheme.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Battery / Status Indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (uiState.batteryCharging) Icons.Default.CheckCircle else Icons.Default.Star,
                            contentDescription = "Battery Status",
                            tint = if (uiState.batteryCharging) ActiveGreen else currentTheme.textSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${uiState.batteryLevel}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = currentTheme.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Temporary Interactive Gesture notification bubble
                AnimatedVisibility(
                    visible = uiState.activeGestureLog.isNotEmpty(),
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(currentTheme.cardBackground)
                            .border(1.dp, currentTheme.accent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Gesture",
                                tint = currentTheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gesture Detected: ${uiState.activeGestureLog}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = currentTheme.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        LaunchedEffect(uiState.activeGestureLog) {
                            if (uiState.activeGestureLog.isNotEmpty()) {
                                delay(2000)
                                viewModel.clearGestureLog()
                            }
                        }
                    }
                }

                // Grid workspace - widgets or icons
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // 🌟 Clean Minimalism USP Welcome Dashboard Card
                    item {
                        LifeLayerWelcomeCard(
                            theme = currentTheme,
                            uiState = uiState,
                            onOpenLifeLayer = { viewModel.toggleLifeLayer(true) }
                        )
                    }

                    // Modern 13. Modular Widget Showcase Row (Clock, Circular Battery, Sticky Notes, Focus Timer)
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // First Widget Pair
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Monospace Clock Widget
                                ClockWidget(
                                    modifier = Modifier.weight(1.3f),
                                    theme = currentTheme,
                                    isNothing = uiState.themeIndex == 0
                                )
                                // Circle Battery Widget
                                BatteryCircularWidget(
                                    modifier = Modifier.weight(0.9f),
                                    batteryLevel = uiState.batteryLevel,
                                    charging = uiState.batteryCharging,
                                    theme = currentTheme
                                )
                            }

                            // Second Widget Pair (Focus Timer & Dynamic Suggestion advisory)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Quick focus timer
                                FocusTimerWidget(
                                    modifier = Modifier.weight(1f),
                                    isActive = uiState.focusModeActive,
                                    theme = currentTheme,
                                    onToggle = { viewModel.toggleFocusMode() }
                                )

                                // Real-time offline predictive suggestions
                                OfflinePredictionWidget(
                                    modifier = Modifier.weight(1f),
                                    suggestions = uiState.suggestions,
                                    theme = currentTheme,
                                    onOpenLifeLayer = { viewModel.toggleLifeLayer(true) }
                                )
                            }
                        }
                    }

                    // Main App Workspace Grid layout (iOS style smooth grid)
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (uiState.focusModeActive) "FOCUS CORE" else "MAIN SYSTEM WORKSPACE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = currentTheme.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )

                                TextButton(
                                    onClick = { viewModel.toggleEditMode() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = currentTheme.accent)
                                ) {
                                    Text(
                                        text = if (uiState.editMode) "DONE" else "EDIT OS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Filters visible apps based on focus mode settings
                            val visibleApps = uiState.installedApps.filter { app ->
                                val okFocus = !uiState.focusModeActive || uiState.activeFocusApps.contains(app.packageName)
                                okFocus && !app.isHidden
                            }

                            if (visibleApps.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(currentTheme.cardBackground)
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No apps configured for focus session.\nEdit workspace or turn off Focus Mode.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = currentTheme.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                AppGrid(
                                    apps = visibleApps,
                                    editMode = uiState.editMode,
                                    focusMode = uiState.focusModeActive,
                                    theme = currentTheme,
                                    onAppClick = { app ->
                                        if (uiState.editMode) {
                                            viewModel.toggleAppFavorite(app.packageName, app.isFavorite)
                                        } else {
                                            viewModel.launchApplication(context, app)
                                        }
                                    },
                                    onHideApp = { app ->
                                        viewModel.toggleAppHidden(app.packageName, app.isHidden)
                                    }
                                )
                            }
                        }
                    }
                }

                // Smart Suggestion Prediction Bar (Clean Minimalism USP)
                SmartPredictionBar(
                    suggestions = uiState.suggestions,
                    theme = currentTheme,
                    onOpenLifeLayer = { viewModel.toggleLifeLayer(true) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Frosted glass premium DOCK at the bottom
                DockView(
                    apps = uiState.installedApps.filter { it.isFavorite && !it.isHidden },
                    allApps = uiState.installedApps,
                    editMode = uiState.editMode,
                    theme = currentTheme,
                    onAppClick = { app ->
                        viewModel.launchApplication(context, app)
                    },
                    onRemoveFavorite = { app ->
                        viewModel.toggleAppFavorite(app.packageName, true)
                    }
                )

                // Edge Drag/Tap Assist handles to ease navigation on emulator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    InteractiveEdgeButton(
                        label = "WIDGETS",
                        icon = Icons.Default.Menu,
                        theme = currentTheme,
                        onClick = { viewModel.toggleWidgetsPanel(true) }
                    )
                    InteractiveEdgeButton(
                        label = "DRAWER",
                        icon = Icons.Default.KeyboardArrowUp,
                        theme = currentTheme,
                        onClick = { viewModel.toggleAppDrawer(true) }
                    )
                    InteractiveEdgeButton(
                        label = "CONTROLS",
                        icon = Icons.Default.KeyboardArrowDown,
                        theme = currentTheme,
                        onClick = { viewModel.toggleControlCenter(true) }
                    )
                    InteractiveEdgeButton(
                        label = "LIFE LAYER",
                        icon = Icons.Default.Star,
                        theme = currentTheme,
                        onClick = { viewModel.toggleLifeLayer(true) }
                    )
                }
            }
        }

        // --- OVERLAYS: APP DRAWER, CONTROL CENTER, WIDGETS DRAWER, LIFE LAYER, VAULT LOCK ---

        // 1. Smart App Drawer Sheet (Swipe Up)
        AnimatedVisibility(
            visible = uiState.appDrawerOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            SmartAppDrawerSheet(
                uiState = uiState,
                theme = currentTheme,
                viewModel = viewModel,
                onClose = { viewModel.toggleAppDrawer(false) }
            )
        }

        // 2. Control Center Sheet (Swipe Down)
        AnimatedVisibility(
            visible = uiState.controlCenterOpen,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it })
        ) {
            ControlCenterSheet(
                uiState = uiState,
                theme = currentTheme,
                viewModel = viewModel,
                onClose = { viewModel.toggleControlCenter(false) }
            )
        }

        // 3. Widget Drawer Panel (Swipe Right)
        AnimatedVisibility(
            visible = uiState.widgetsPanelOpen,
            enter = slideInHorizontally(initialOffsetX = { -it }),
            exit = slideOutHorizontally(targetOffsetX = { -it })
        ) {
            WidgetsBoardSheet(
                notes = notes,
                theme = currentTheme,
                viewModel = viewModel,
                onClose = { viewModel.toggleWidgetsPanel(false) }
            )
        }

        // 4. Life Layer Performance Dashboard (Main USP - Swipe Left)
        AnimatedVisibility(
            visible = uiState.lifeLayerOpen,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            LifeDashboardSheet(
                uiState = uiState,
                theme = currentTheme,
                viewModel = viewModel,
                onClose = { viewModel.toggleLifeLayer(false) }
            )
        }

        // 5. Private Space Vault Locked modal
        AnimatedVisibility(
            visible = !uiState.privateVaultLocked,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            PrivateVaultSheet(
                uiState = uiState,
                theme = currentTheme,
                viewModel = viewModel,
                onClose = { viewModel.lockVault() }
            )
        }

        // 6. Universal Action Floating Bubble (Bubble expanded state)
        QuickActionBubbleView(
            uiState = uiState,
            theme = currentTheme,
            viewModel = viewModel
        )
    }
}

// ================= COMPOSABLE COMPONENT LAYOUTS =================

@Composable
fun ClockWidget(
    modifier: Modifier = Modifier,
    theme: CustomThemeConfig,
    isNothing: Boolean
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .height(90.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            val calendar = Calendar.getInstance()
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            val dateFmt = SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(calendar.time)
            
            Text(
                text = timeFmt,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary,
                fontFamily = if (isNothing) MonospaceFont else SansSerifFont,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateFmt.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = theme.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun BatteryCircularWidget(
    modifier: Modifier = Modifier,
    batteryLevel: Int,
    charging: Boolean,
    theme: CustomThemeConfig
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .height(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(45.dp)
            ) {
                CircularProgressIndicator(
                    progress = { batteryLevel / 100f },
                    color = if (charging) ActiveGreen else theme.accent,
                    strokeWidth = 4.dp,
                    trackColor = Color(0x22FFFFFF),
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = "$batteryLevel%",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (charging) "CHARGING" else "BATTERY",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = theme.textSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FocusTimerWidget(
    modifier: Modifier = Modifier,
    isActive: Boolean,
    theme: CustomThemeConfig,
    onToggle: () -> Unit
) {
    var timerSeconds by remember { mutableStateOf(1500) } // 25 min default
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning) {
        while (isRunning && timerSeconds > 0) {
            delay(1000)
            timerSeconds -= 1
        }
        if (timerSeconds == 0) {
            isRunning = false
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
            .padding(12.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCUS DEEP",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isActive) ActiveGreen else Color.Gray)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val mins = timerSeconds / 60
                val secs = timerSeconds % 60
                val timeString = String.format("%02d:%02d", mins, secs)

                Text(
                    text = timeString,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = MonospaceFont,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = {
                        isRunning = !isRunning
                        if (!isActive) onToggle()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .background(theme.accent.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = "Control Timer",
                        tint = theme.accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = if (isRunning) "BREATHE IN... OUT" else "TAP PLAY TO ENGAGE",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = theme.textSecondary
            )
        }
    }
}

@Composable
fun OfflinePredictionWidget(
    modifier: Modifier = Modifier,
    suggestions: List<String>,
    theme: CustomThemeConfig,
    onOpenLifeLayer: () -> Unit
) {
    val suggestionText = suggestions.firstOrNull() ?: "Tracking usage patterns to construct suggestions..."

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
            .clickable { onOpenLifeLayer() }
            .padding(12.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SMART SUGGEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.textSecondary,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "AI offline",
                    tint = theme.accent,
                    modifier = Modifier.size(12.dp)
                )
            }

            Text(
                text = suggestionText,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = theme.textPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )

            Text(
                text = "TAP TO ANALYZE GRAPH",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = theme.accent
            )
        }
    }
}

@Composable
fun LifeLayerWelcomeCard(
    theme: CustomThemeConfig,
    uiState: LauncherUiState,
    onOpenLifeLayer: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(28.dp))
            .clickable { onOpenLifeLayer() }
            .padding(18.dp)
    ) {
        // Subtle decorative dynamic orange glow aura
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 10.dp, y = (-10).dp)
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(theme.accent.copy(alpha = 0.18f), Color.Transparent),
                        radius = 180f
                    )
                )
        )
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "LIFE LAYER // MORNING",
                style = MaterialTheme.typography.labelSmall,
                color = theme.accent,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Good morning, Abdurrahman",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 21.sp,
                fontWeight = FontWeight.Light,
                color = theme.textPrimary
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus sub-panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x06FFFFFF))
                        .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = theme.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (uiState.focusModeActive) "Deep Work" else "Unfocused",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textPrimary
                        )
                    }
                }
                
                // Tech Perf Score sub-panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x06FFFFFF))
                        .border(1.dp, Color(0x0EFFFFFF), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "SCREEN TIME",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            color = theme.textSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "2h 14m",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.textPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0x12FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚡", fontSize = 11.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(Color(0x12FFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🧠", fontSize = 11.sp)
                    }
                }
                
                Text(
                    text = "System optimized for productivity",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = theme.textSecondary
                )
            }
        }
    }
}

@Composable
fun SmartPredictionBar(
    suggestions: List<String>,
    theme: CustomThemeConfig,
    onOpenLifeLayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestion = suggestions.firstOrNull() ?: "Study AI"
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(20.dp))
            .clickable { onOpenLifeLayer() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left badge: Smart Suggest
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Pulse indicator
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .graphicsLayer { alpha = pulseAlpha }
                        .background(Color(0xFFF97316)) // Glowing Orange Dot
                )
                
                Text(
                    text = "Smart Suggest: $suggestion",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.textPrimary
                )
            }
            
            // Sub-text context info
            Text(
                text = "Based on 10 AM routine",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = theme.textSecondary
            )
        }
    }
}

@Composable
fun AppGrid(
    apps: List<OsApp>,
    editMode: Boolean,
    focusMode: Boolean,
    theme: CustomThemeConfig,
    onAppClick: (OsApp) -> Unit,
    onHideApp: (OsApp) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shake")
    val shakeVal by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val rows = apps.chunked(4)
        rows.forEach { rowApps ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (col in 0 until 4) {
                    if (col < rowApps.size) {
                        val app = rowApps[col]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .graphicsLayer {
                                    if (editMode) {
                                        rotationZ = shakeVal
                                    }
                                }
                                .clickable { onAppClick(app) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().testTag("app_${app.appName.lowercase()}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(theme.cardBackground)
                                        .border(2.dp, if (app.isFavorite) theme.accent else theme.borderAccent, RoundedCornerShape(14.dp))
                                ) {
                                    // Custom drawn high fidelity vector icon instead of boring assets
                                    CustomBespokeIcon(
                                        appName = app.appName,
                                        category = app.category,
                                        accent = theme.accent
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = theme.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Edit Overlay
                            if (editMode) {
                                IconButton(
                                    onClick = { onHideApp(app) },
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Red, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Hide App",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun DockView(
    apps: List<OsApp>,
    allApps: List<OsApp>,
    editMode: Boolean,
    theme: CustomThemeConfig,
    onAppClick: (OsApp) -> Unit,
    onRemoveFavorite: (OsApp) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.borderAccent, RoundedCornerShape(36.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Default setup dock apps
            val dockApps = if (apps.isEmpty()) {
                // Return default subset of Work/Tools to represent complete Dock
                allApps.filter { it.packageName in listOf("com.social.whatsapp", "com.work.slack", "com.tools.calculator", "com.tools.notes") }
            } else {
                apps.take(4)
            }

            dockApps.forEachIndexed { index, app ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onAppClick(app) }
                        .testTag("dock_${app.appName.lowercase()}")
                ) {
                    when (index) {
                        0 -> {
                            // Solid white card with bold inner black squircle
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .border(1.dp, theme.borderAccent, RoundedCornerShape(18.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black)
                                )
                            }
                        }
                        1 -> {
                            // Circular outline holo glass
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x0EFFFFFF))
                                    .border(1.dp, theme.borderAccent, RoundedCornerShape(18.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .border(2.dp, Color(0x99FFFFFF), CircleShape)
                                )
                            }
                        }
                        2 -> {
                            // Muted translucent card with small center square
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x0EFFFFFF))
                                    .border(1.dp, theme.borderAccent, RoundedCornerShape(18.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x99FFFFFF))
                                )
                            }
                        }
                        3 -> {
                            // Dynamic active favorited emerald Capsule / Dot indicator
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0x3310B981))
                                    .border(1.dp, Color(0x4D10B981), RoundedCornerShape(18.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF34D399))
                                )
                            }
                        }
                        else -> {
                            // Default beautiful minimalist fallback app
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(theme.cardBackground)
                                    .border(1.dp, theme.borderAccent, RoundedCornerShape(18.dp))
                            ) {
                                CustomBespokeIcon(
                                    appName = app.appName,
                                    category = app.category,
                                    accent = theme.accent
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
fun CustomBespokeIcon(
    appName: String,
    category: String,
    accent: Color
) {
    val norm = appName.lowercase()
    val vector = when {
        norm.contains("whatsapp") || norm.contains("slack") || norm.contains("chat") -> Icons.Default.Check
        norm.contains("mail") || norm.contains("notion") -> Icons.Default.CheckCircle
        norm.contains("calc") -> Icons.Default.Build
        norm.contains("notes") -> Icons.Default.Menu
        norm.contains("game") || norm.contains("chess") || norm.contains("pubg") -> Icons.Default.Star
        else -> Icons.Default.CheckCircle
    }

    // High visual fidelity procedural icons for elegant Nothing aesthetics
    Canvas(modifier = Modifier.size(24.dp)) {
        when {
            category == "Social" -> {
                // Double concentric circular pattern mimicking smooth web design
                drawCircle(color = accent, radius = size.minDimension / 2.5f, style = Stroke(width = 2f))
                drawCircle(color = accent, radius = size.minDimension / 5f)
            }
            category == "Work" -> {
                // Angled clean business lines
                drawLine(color = accent, start = Offset(0f, 0f), end = Offset(size.width, size.height), strokeWidth = 3f)
                drawLine(color = accent, start = Offset(size.width, 0f), end = Offset(0f, size.height), strokeWidth = 1.5f)
            }
            category == "Study" -> {
                // Concentric square layout
                drawRect(color = accent, style = Stroke(width = 2.5f))
                drawCircle(color = accent, radius = 4f, center = center)
            }
            category == "Games" -> {
                // Cross dots
                drawCircle(color = accent, radius = 3f, center = Offset(size.width * 0.2f, size.height * 0.2f))
                drawCircle(color = accent, radius = 3f, center = Offset(size.width * 0.8f, size.height * 0.2f))
                drawCircle(color = accent, radius = 3f, center = Offset(size.width * 0.2f, size.height * 0.8f))
                drawCircle(color = accent, radius = 3f, center = Offset(size.width * 0.8f, size.height * 0.8f))
                drawCircle(color = accent, radius = 4f, center = center)
            }
            else -> {
                // Dot matrix box
                val dotSize = 2f
                val spacing = 8f
                for (x in 2..size.width.toInt() step spacing.toInt()) {
                    for (y in 2..size.height.toInt() step spacing.toInt()) {
                        drawCircle(color = accent, radius = dotSize, center = Offset(x.toFloat(), y.toFloat()))
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveEdgeButton(
    label: String,
    icon: ImageVector,
    theme: CustomThemeConfig,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(theme.cardBackground)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = theme.accent,
                modifier = Modifier.size(10.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = theme.textPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// 📂 Smart App Drawer sheet with Categories & Search
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAppDrawerSheet(
    uiState: LauncherUiState,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE0050505))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NOVA_DRAWER.bin",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Universal Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search apps, shortcuts, contacts...", color = theme.textSecondary) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = theme.accent) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("drawer_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedLabelColor = theme.accent,
                    focusedBorderColor = theme.accent,
                    unfocusedBorderColor = theme.textSecondary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Auto-categories section
            val categories = listOf("Social", "Work", "Study", "Games", "Tools")
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                categories.forEach { cat ->
                    val catApps = uiState.installedApps.filter { it.category == cat }
                    if (catApps.isNotEmpty()) {
                        item {
                            Column {
                                Text(
                                    text = "$cat.log // ${catApps.size} apps",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = theme.accent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    catApps.forEach { app ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(theme.cardBackground)
                                                .clickable {
                                                    viewModel.launchApplication(context, app)
                                                    onClose()
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(theme.accent)
                                                )
                                                Text(
                                                    text = app.appName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = theme.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ⚡ Dynamic Control Center Sheet (Swipe Down)
@Composable
fun ControlCenterSheet(
    uiState: LauncherUiState,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE50B0B0D))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONTROL_CENTER.sys",
                        style = MaterialTheme.typography.headlineMedium,
                        color = theme.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Custom sliders representing dynamic Nothing aesthetic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Brightness slider mockup
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.cardBackground)
                            .padding(12.dp)
                    ) {
                        var brightnessVal by remember { mutableStateOf(0.75f) }
                        Column {
                            Text("BRIGHTNESS // ${(brightnessVal * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = brightnessVal,
                                onValueChange = { brightnessVal = it },
                                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                            )
                        }
                    }

                    // Audio slider mockup
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(theme.cardBackground)
                            .padding(12.dp)
                    ) {
                        var audioVal by remember { mutableStateOf(0.5f) }
                        Column {
                            Text("AUDIO_VOL // ${(audioVal * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Slider(
                                value = audioVal,
                                onValueChange = { audioVal = it },
                                colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 🧭 Theme Switcher (Personal OS Identity Theme)
                Column {
                    Text(
                        text = "PERSONAL OS IDENTITY ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val themes = listOf("Nothing Monopix", "iOS Blue Frost", "AMOLED Orange", "Mossy Autumn")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        themes.forEachIndexed { index, name ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (uiState.themeIndex == index) theme.accent else theme.cardBackground)
                                    .border(1.dp, if (uiState.themeIndex == index) Color.Transparent else theme.borderAccent, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.setThemeIndex(index) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = if (uiState.themeIndex == index) Color.Black else theme.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Power control buttons at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleFocusMode() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (uiState.focusModeActive) Color.Red else theme.cardBackground)
                ) {
                    Text(text = if (uiState.focusModeActive) "FOCUS ACTIVE" else "TOGGLE FOCUS", color = theme.textPrimary)
                }

                Button(
                    onClick = { viewModel.unlockVault() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBackground)
                ) {
                    Text(text = "VAULT STORAGE", color = theme.textPrimary)
                }
            }
        }
    }
}

// 🧩 Widgets Board Drawer (Left side swipe)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsBoardSheet(
    notes: List<LauncherNote>,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    var notepadText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE808080A))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AESTHETIC_BOARD.db",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sticky Notepad integration
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.cardBackground)
                    .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "AESTHETIC_STICKY_PAD.notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = theme.accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                OutlinedTextField(
                    value = notepadText,
                    onValueChange = { notepadText = it },
                    placeholder = { Text("Write down instant thoughts...", color = theme.textSecondary) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.borderAccent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (notepadText.isNotBlank()) {
                            viewModel.addNote("Nova Scribble", notepadText)
                            notepadText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) {
                    Text("ADD SCRIP", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Existing Notes list
            Text(
                "SAVED_OS_RECORDS //",
                style = MaterialTheme.typography.labelSmall,
                color = theme.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (notes.isEmpty()) {
                    item {
                        Text(
                            text = "No saved notes. Write ideas above to store locally in Room Database.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                        )
                    }
                } else {
                    items(notes) { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(theme.cardBackground)
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(note.title.uppercase(), style = MaterialTheme.typography.labelSmall, color = theme.accent, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(note.content, style = MaterialTheme.typography.bodyMedium, color = theme.textPrimary)
                            }
                            IconButton(onClick = { viewModel.deleteNoteById(note.id) }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete note", tint = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 🌟 Life Layer Dashboard (Main USP - Swipe Left)
@Composable
fun LifeDashboardSheet(
    uiState: LauncherUiState,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF9070709))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIFE_LAYER.log",
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mood Tracker Visual block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.cardBackground)
                    .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "BIOMEDICAL_MOOD_SECTOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = theme.accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "How is your cognitive state feeling today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = theme.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val moods = listOf("Chill", "Tired", "Focus", "Energetic", "Unstoppable")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moods.forEach { mood ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (uiState.moodToday == mood) theme.accent else Color(0x10FFFFFF))
                                    .clickable { viewModel.setMood(mood) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = mood.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 7.sp,
                                    color = if (uiState.moodToday == mood) Color.Black else theme.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 📊 Phone Health Circular Canvas Gauges (Battery level, storage level, Phone Performance Score)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.cardBackground)
                        .border(1.dp, theme.borderAccent, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PERF_SCORE", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                            CircularProgressIndicator(
                                progress = { uiState.phonePerformanceScore / 100f },
                                color = theme.accent,
                                strokeWidth = 5.dp,
                                trackColor = Color(0x11FFFFFF)
                            )
                            Text("${uiState.phonePerformanceScore}", style = MaterialTheme.typography.headlineMedium, fontSize = 16.sp, color = theme.textPrimary)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(theme.cardBackground)
                        .border(1.dp, theme.borderAccent, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("STORAGE_USED", style = MaterialTheme.typography.labelSmall, color = theme.textSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(60.dp)) {
                            CircularProgressIndicator(
                                progress = { uiState.storagePercentUsed / 100f },
                                color = IOSBlue,
                                strokeWidth = 5.dp,
                                trackColor = Color(0x11FFFFFF)
                            )
                            Text("${uiState.storagePercentUsed}%", style = MaterialTheme.typography.headlineMedium, fontSize = 16.sp, color = theme.textPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Usage Rank of applications
            Text(
                "LAUNCH_COUNTS_STATISTICS //",
                style = MaterialTheme.typography.labelSmall,
                color = theme.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (uiState.recentApps.isEmpty()) {
                    item {
                        Text(
                            text = "Launch applications on the main grid to record historical usage and build local suggestions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = theme.textSecondary,
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                        )
                    }
                } else {
                    items(uiState.recentApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(theme.cardBackground)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.appName, style = MaterialTheme.typography.bodyMedium, color = theme.textPrimary)
                            Text("LAUNCH_COUNT: ${app.launchCount} times", style = MaterialTheme.typography.labelSmall, color = theme.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 🔒 Private Vault Secure Folder dialog
@Composable
fun PrivateVaultSheet(
    uiState: LauncherUiState,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel,
    onClose: () -> Unit
) {
    var codeText by remember { mutableStateOf("") }
    var setupPasswordText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFA050505))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(20.dp))
                .background(theme.cardBackground)
                .border(2.dp, theme.accent, RoundedCornerShape(20.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("SECURE_VAULT.sys", style = MaterialTheme.typography.headlineMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(10.dp))

            if (!uiState.privateVaultPasswordSet) {
                // Setup Mode
                Text("Setup security code to lock hidden records", style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = setupPasswordText,
                    onValueChange = { setupPasswordText = it },
                    label = { Text("Set PIN Code", color = theme.textSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (setupPasswordText.isNotBlank()) {
                            viewModel.setVaultPassword(setupPasswordText)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) {
                    Text("SAVE PASSCODE", color = Color.White)
                }
            } else {
                // Unlock mode
                Text("Enter security PIN to reveal hidden applications folder", style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = codeText,
                    onValueChange = { codeText = it },
                    label = { Text("Code PIN", color = theme.textSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = theme.accent)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.verifyVaultPassword(codeText)
                            codeText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                    ) {
                        Text("UNLOCK", color = Color.White)
                    }

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.3f))
                    ) {
                        Text("CANCEL", color = theme.textPrimary)
                    }
                }
            }
        }
    }
}

// 🔥 Quick Action floating bubble with Calculator + Instant Flashlight simulator
@Composable
fun QuickActionBubbleView(
    uiState: LauncherUiState,
    theme: CustomThemeConfig,
    viewModel: LauncherViewModel
) {
    var showMiniCalculator by remember { mutableStateOf(false) }
    var calcExpr by remember { mutableStateOf("") }
    var flashlightOn by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Floating expanded drawer controls
        if (uiState.bubbleExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 80.dp, bottom = 40.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(theme.cardBackground)
                    .border(1.dp, theme.borderAccent, RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .width(180.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("QUICK_OPERATIONS //", style = MaterialTheme.typography.labelSmall, color = theme.accent, fontWeight = FontWeight.Bold)

                    // Flashlight simulate
                    Button(
                        onClick = { flashlightOn = !flashlightOn },
                        colors = ButtonDefaults.buttonColors(containerColor = if (flashlightOn) ActiveGreen else Color(0x10FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Flashlight", modifier = Modifier.size(14.dp), tint = if (flashlightOn) Color.Black else theme.textPrimary)
                            Text(text = if (flashlightOn) "BEAM: INTENSE" else "FLASHLIGHT", color = if (flashlightOn) Color.Black else theme.textPrimary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }

                    // Mini calculator toggle
                    Button(
                        onClick = { showMiniCalculator = !showMiniCalculator },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x10FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Build, contentDescription = "Calculator", modifier = Modifier.size(14.dp), tint = theme.textPrimary)
                            Text(text = "CALCULATOR_SYS", color = theme.textPrimary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }

                    // Toggle Widget Panel
                    Button(
                        onClick = {
                            viewModel.toggleWidgetsPanel(true)
                            viewModel.toggleQuickBubbleExpanded()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x10FFFFFF)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "AESTHETIC SCRIBER", color = theme.textPrimary, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                    }
                }
            }
        }

        // Mini Calculator Panel dialog
        if (showMiniCalculator) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .background(Color(0xAA000000))
                    .clickable { showMiniCalculator = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(theme.cardBackground)
                        .border(1.dp, theme.accent, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                        .width(260.dp)
                        .clickable(enabled = false) { }
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MINI_CALC.bin", style = MaterialTheme.typography.labelSmall, color = theme.accent, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { showMiniCalculator = false }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = theme.textPrimary, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Display output screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF000000))
                                .border(1.dp, theme.borderAccent, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = calcExpr.ifEmpty { "0" }, style = MaterialTheme.typography.bodyMedium, color = theme.textSecondary)
                                Text(text = uiState.quickCalculatorResult, style = MaterialTheme.typography.headlineMedium, fontSize = 18.sp, color = theme.textPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid keys
                        val keys = listOf(
                            "7", "8", "9", "/",
                            "4", "5", "6", "*",
                            "1", "2", "3", "-",
                            "C", "0", "=", "+"
                        )

                        val keyChunks = keys.chunked(4)
                        keyChunks.forEach { chunk ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                chunk.forEach { key ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1.3f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (key == "=") theme.accent else Color(0x10FFFFFF))
                                            .clickable {
                                                when (key) {
                                                    "C" -> {
                                                        calcExpr = ""
                                                        viewModel.clearCalculator()
                                                    }
                                                    "=" -> {
                                                        viewModel.evaluateCalculator(calcExpr)
                                                    }
                                                    else -> {
                                                        calcExpr += key
                                                    }
                                                }
                                            }
                                            .padding(6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = key, style = MaterialTheme.typography.headlineMedium, fontSize = 16.sp, color = if (key == "=") Color.Black else theme.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Custom Trigger bubble floating on top
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 40.dp)
                .size(52.dp)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(theme.accent)
                .clickable { viewModel.toggleQuickBubbleExpanded() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (uiState.bubbleExpanded) Icons.Default.Close else Icons.Default.Menu,
                contentDescription = "Quick Options",
                tint = Color.Black,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ================= THEME ENGINE UTILITIES =================

data class CustomThemeConfig(
    val gradientColors: List<Color>,
    val cardBackground: Color,
    val borderAccent: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

fun getAestheticTheme(index: Int): CustomThemeConfig {
    return when (index) {
        0 -> CustomThemeConfig(
            gradientColors = listOf(Color(0xFF050505), Color(0xFF0A0A0F), Color(0xFF050505)),
            cardBackground = Color(0x0EFFFFFF),
            borderAccent = Color(0x13FFFFFF),
            accent = Color(0xFFF97316), // Glowing Minimalist Orange
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0x99F8FAFC)
        )
        1 -> CustomThemeConfig(
            gradientColors = listOf(Color(0xFF030712), Color(0xFF1E293B), Color(0xFF0A0A0E)),
            cardBackground = Color(0x14FFFFFF),
            borderAccent = Color(0x1BFFFFFF),
            accent = Color(0xFF38BDF8),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0x99F8FAFC)
        )
        2 -> CustomThemeConfig(
            gradientColors = listOf(Color(0xFF050505), Color(0xFF1C0D02), Color(0xFF050505)),
            cardBackground = Color(0x0FFFFFFF),
            borderAccent = Color(0x22F97316),
            accent = Color(0xFFFF7A00),
            textPrimary = Color(0xFFFFF2E6),
            textSecondary = Color(0xFFD4C2B3)
        )
        else -> CustomThemeConfig(
            gradientColors = listOf(Color(0xFF030712), Color(0xFF064E3B), Color(0xFF020617)),
            cardBackground = Color(0x0FFFFFFF),
            borderAccent = Color(0x1B34D399),
            accent = Color(0xFF34D399),
            textPrimary = Color(0xFFECFDF5),
            textSecondary = Color(0x99ECFDF5)
        )
    }
}

// Minimal placeholder FlowRow layout since original is missing standard library import
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        val layoutWidth = constraints.maxWidth
        
        var currentX = 0
        var currentY = 0
        var rowHeight = 0
        
        val placements = mutableListOf<Pair<androidx.compose.ui.layout.Placeable, IntOffset>>()
        
        placeables.forEach { placeable ->
            if (currentX + placeable.width > layoutWidth) {
                currentX = 0
                currentY += rowHeight + 16 // hardcoded vertical gap
                rowHeight = 0
            }
            
            placements.add(placeable to IntOffset(currentX, currentY))
            
            currentX += placeable.width + 16 // hardcoded horizontal gap
            rowHeight = maxOf(rowHeight, placeable.height)
        }
        
        layout(layoutWidth, currentY + rowHeight) {
            placements.forEach { (placeable, offset) ->
                placeable.placeRelative(offset.x, offset.y)
            }
        }
    }
}


