package com.example.calculator

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.min
import kotlin.math.roundToInt

private const val PREFS_NAME = "calculator_user_prefs"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_THEME_PALETTE = "theme_palette"
private const val KEY_BUTTON_SHAPE = "button_shape"
private const val KEY_LANGUAGE = "app_language"
private const val KEY_DEGREE_MODE = "degree_mode"
private const val KEY_AUTO_SCI_LANDSCAPE = "auto_sci_landscape"
private const val KEY_TOP_BAR_ORDER = "top_bar_order"
private const val KEY_GRID_LAYOUT = "grid_layout_v1"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CalculatorViewModel = viewModel()
            val context = LocalContext.current
            val prefs = remember(context) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }

            // Load persisted settings synchronously before first composition
            remember(vm, prefs) {
                val savedMode = runCatching {
                    ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
                }.getOrDefault(ThemeMode.SYSTEM)

                val savedPalette = runCatching {
                    ThemePalette.valueOf(prefs.getString(KEY_THEME_PALETTE, ThemePalette.OCEAN.name) ?: ThemePalette.OCEAN.name)
                }.getOrDefault(ThemePalette.OCEAN)

                val savedShape = runCatching {
                    ButtonShapeMode.valueOf(prefs.getString(KEY_BUTTON_SHAPE, ButtonShapeMode.ROUNDED.name) ?: ButtonShapeMode.ROUNDED.name)
                }.getOrDefault(ButtonShapeMode.ROUNDED)

                val savedLang = runCatching {
                    AppLanguage.valueOf(prefs.getString(KEY_LANGUAGE, AppLanguage.RU.name) ?: AppLanguage.RU.name)
                }.getOrDefault(AppLanguage.RU)

                val savedDeg = prefs.getBoolean(KEY_DEGREE_MODE, true)
                val savedAutoSci = prefs.getBoolean(KEY_AUTO_SCI_LANDSCAPE, true)
                val savedTopBar = vm.parseTopBarOrder(prefs.getString(KEY_TOP_BAR_ORDER, null))
                val savedGrid = vm.parseGridLayout(prefs.getString(KEY_GRID_LAYOUT, null))

                vm.loadSavedSettings(
                    savedThemeMode = savedMode,
                    savedPalette = savedPalette,
                    savedShape = savedShape,
                    savedLang = savedLang,
                    savedDegreeMode = savedDeg,
                    savedAutoSciOnLandscape = savedAutoSci,
                    savedTopBarOrder = savedTopBar,
                    savedGridLayout = savedGrid
                )
                true
            }

            // Persist settings whenever any user preference or layout customization changes
            LaunchedEffect(
                vm.themeMode,
                vm.themePalette,
                vm.buttonShapeMode,
                vm.language,
                vm.isDegreeMode,
                vm.autoScientificOnLandscape,
                vm.topBarOrder,
                vm.gridLayout
            ) {
                prefs.edit()
                    .putString(KEY_THEME_MODE, vm.themeMode.name)
                    .putString(KEY_THEME_PALETTE, vm.themePalette.name)
                    .putString(KEY_BUTTON_SHAPE, vm.buttonShapeMode.name)
                    .putString(KEY_LANGUAGE, vm.language.name)
                    .putBoolean(KEY_DEGREE_MODE, vm.isDegreeMode)
                    .putBoolean(KEY_AUTO_SCI_LANDSCAPE, vm.autoScientificOnLandscape)
                    .putString(KEY_TOP_BAR_ORDER, vm.serializeTopBarOrder())
                    .putString(KEY_GRID_LAYOUT, vm.serializeGridLayout())
                    .apply()
            }

            val systemDark = isSystemInDarkTheme()
            val isDark = when (vm.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }
            CalculatorAppTheme(
                darkTheme = isDark,
                palette = vm.themePalette
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalculatorScreen(vm = vm, isDarkTheme = isDark)
                }
            }
        }
    }
}

@Composable
fun CalculatorAppTheme(
    darkTheme: Boolean,
    palette: ThemePalette,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        when (palette) {
            ThemePalette.OCEAN -> darkColorScheme(
                primary = Color(0xFF38BDF8),
                onPrimary = Color(0xFF00263A),
                primaryContainer = Color(0xFF0284C7),
                onPrimaryContainer = Color(0xFFE0F2FE),
                secondaryContainer = Color(0xFF1E293B),
                onSecondaryContainer = Color(0xFFE2E8F0),
                tertiaryContainer = Color(0xFF283548),
                onTertiaryContainer = Color(0xFF7DD3FC),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF0B1120),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF111827),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1E293B),
                onSurfaceVariant = Color(0xFF94A3B8)
            )
            ThemePalette.EMERALD -> darkColorScheme(
                primary = Color(0xFF34D399),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF059669),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondaryContainer = Color(0xFF142924),
                onSecondaryContainer = Color(0xFFECFDF5),
                tertiaryContainer = Color(0xFF1F3A33),
                onTertiaryContainer = Color(0xFF6EE7B7),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF071512),
                onBackground = Color(0xFFF0FDF4),
                surface = Color(0xFF0D1F1B),
                onSurface = Color(0xFFF0FDF4),
                surfaceVariant = Color(0xFF162E28),
                onSurfaceVariant = Color(0xFF99B8AF)
            )
            ThemePalette.AMETHYST -> darkColorScheme(
                primary = Color(0xFFC084FC),
                onPrimary = Color(0xFF2E1065),
                primaryContainer = Color(0xFF7C3AED),
                onPrimaryContainer = Color(0xFFF3E8FF),
                secondaryContainer = Color(0xFF231B38),
                onSecondaryContainer = Color(0xFFF5F3FF),
                tertiaryContainer = Color(0xFF342654),
                onTertiaryContainer = Color(0xFFD8B4FE),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF0F0A1C),
                onBackground = Color(0xFFFAF5FF),
                surface = Color(0xFF17112B),
                onSurface = Color(0xFFFAF5FF),
                surfaceVariant = Color(0xFF251C42),
                onSurfaceVariant = Color(0xFFB5A8D1)
            )
            ThemePalette.AMBER -> darkColorScheme(
                primary = Color(0xFFFBBF24),
                onPrimary = Color(0xFF3F2305),
                primaryContainer = Color(0xFFD97706),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondaryContainer = Color(0xFF292116),
                onSecondaryContainer = Color(0xFFFFFBEB),
                tertiaryContainer = Color(0xFF3D301D),
                onTertiaryContainer = Color(0xFFFCD34D),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF14100A),
                onBackground = Color(0xFFFFFBEB),
                surface = Color(0xFF1F1910),
                onSurface = Color(0xFFFFFBEB),
                surfaceVariant = Color(0xFF2E2418),
                onSurfaceVariant = Color(0xFFC4B59B)
            )
            ThemePalette.ROSE -> darkColorScheme(
                primary = Color(0xFFFB7185),
                onPrimary = Color(0xFF4C0519),
                primaryContainer = Color(0xFFE11D48),
                onPrimaryContainer = Color(0xFFFFE4E6),
                secondaryContainer = Color(0xFF2D1922),
                onSecondaryContainer = Color(0xFFFFF1F2),
                tertiaryContainer = Color(0xFF422230),
                onTertiaryContainer = Color(0xFFFDA4AF),
                errorContainer = Color(0xFF7F1D1D),
                onErrorContainer = Color(0xFFFECACA),
                background = Color(0xFF170B10),
                onBackground = Color(0xFFFFF1F2),
                surface = Color(0xFF221118),
                onSurface = Color(0xFFFFF1F2),
                surfaceVariant = Color(0xFF331B26),
                onSurfaceVariant = Color(0xFFC9A3B0)
            )
        }
    } else {
        when (palette) {
            ThemePalette.OCEAN -> lightColorScheme(
                primary = Color(0xFF0284C7),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF0284C7),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFE2E8F0),
                onSecondaryContainer = Color(0xFF0F172A),
                tertiaryContainer = Color(0xFFBAE6FD),
                onTertiaryContainer = Color(0xFF0369A1),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFF1F5F9),
                onBackground = Color(0xFF0F172A),
                surface = Color.White,
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = Color(0xFF475569)
            )
            ThemePalette.EMERALD -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF059669),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFD1FAE5),
                onSecondaryContainer = Color(0xFF064E3B),
                tertiaryContainer = Color(0xFFA7F3D0),
                onTertiaryContainer = Color(0xFF047857),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFECFDF5),
                onBackground = Color(0xFF064E3B),
                surface = Color.White,
                onSurface = Color(0xFF064E3B),
                surfaceVariant = Color(0xFFD1FAE5),
                onSurfaceVariant = Color(0xFF047857)
            )
            ThemePalette.AMETHYST -> lightColorScheme(
                primary = Color(0xFF7C3AED),
                onPrimary = Color.White,
                primaryContainer = Color(0xFF7C3AED),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFEDE9FE),
                onSecondaryContainer = Color(0xFF2E1065),
                tertiaryContainer = Color(0xFFDDD6FE),
                onTertiaryContainer = Color(0xFF5B21B6),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFF5F3FF),
                onBackground = Color(0xFF2E1065),
                surface = Color.White,
                onSurface = Color(0xFF2E1065),
                surfaceVariant = Color(0xFFEDE9FE),
                onSurfaceVariant = Color(0xFF5B21B6)
            )
            ThemePalette.AMBER -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD97706),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFFEF3C7),
                onSecondaryContainer = Color(0xFF451A03),
                tertiaryContainer = Color(0xFFFDE68A),
                onTertiaryContainer = Color(0xFFB45309),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFFFFBEB),
                onBackground = Color(0xFF451A03),
                surface = Color.White,
                onSurface = Color(0xFF451A03),
                surfaceVariant = Color(0xFFFEF3C7),
                onSurfaceVariant = Color(0xFF92400E)
            )
            ThemePalette.ROSE -> lightColorScheme(
                primary = Color(0xFFE11D48),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE11D48),
                onPrimaryContainer = Color.White,
                secondaryContainer = Color(0xFFFFE4E6),
                onSecondaryContainer = Color(0xFF4C0519),
                tertiaryContainer = Color(0xFFFECDD3),
                onTertiaryContainer = Color(0xFFBE123C),
                errorContainer = Color(0xFFFEE2E2),
                onErrorContainer = Color(0xFFB91C1C),
                background = Color(0xFFFFF1F2),
                onBackground = Color(0xFF4C0519),
                surface = Color.White,
                onSurface = Color(0xFF4C0519),
                surfaceVariant = Color(0xFFFFE4E6),
                onSurfaceVariant = Color(0xFF9F1239)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

enum class CalcButtonKind {
    NUMBER, OPERATOR, ACTION, SCIENTIFIC, CLEAR, EQUALS
}

data class CalcButtonSpec(
    val id: String,
    val label: String,
    val kind: CalcButtonKind,
    val onClick: () -> Unit
)

fun resolveButtonSpec(id: String, vm: CalculatorViewModel): CalcButtonSpec {
    return when (id) {
        "DEG" -> CalcButtonSpec(id, if (vm.isDegreeMode) "DEG" else "RAD", CalcButtonKind.SCIENTIFIC) { vm.toggleDegreeMode() }
        "sin" -> CalcButtonSpec(id, "sin", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sin") }
        "cos" -> CalcButtonSpec(id, "cos", CalcButtonKind.SCIENTIFIC) { vm.onFunction("cos") }
        "tan" -> CalcButtonSpec(id, "tan", CalcButtonKind.SCIENTIFIC) { vm.onFunction("tan") }
        "ln" -> CalcButtonSpec(id, "ln", CalcButtonKind.SCIENTIFIC) { vm.onFunction("ln") }
        "sqrt" -> CalcButtonSpec(id, "√x", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sqrt") }
        "pow" -> CalcButtonSpec(id, "xʸ", CalcButtonKind.SCIENTIFIC) { vm.onPower() }
        "log" -> CalcButtonSpec(id, "log", CalcButtonKind.SCIENTIFIC) { vm.onFunction("log") }
        "fact" -> CalcButtonSpec(id, "n!", CalcButtonKind.SCIENTIFIC) { vm.onFactorial() }
        "pi" -> CalcButtonSpec(id, "π", CalcButtonKind.SCIENTIFIC) { vm.onConstant("π") }
        "AC" -> CalcButtonSpec(id, "AC", CalcButtonKind.CLEAR) { vm.onClear() }
        "paren" -> CalcButtonSpec(id, "( )", CalcButtonKind.ACTION) { vm.onParentheses() }
        "percent" -> CalcButtonSpec(id, "%", CalcButtonKind.ACTION) { vm.onPercent() }
        "div" -> CalcButtonSpec(id, "÷", CalcButtonKind.OPERATOR) { vm.onOperator("÷") }
        "mul" -> CalcButtonSpec(id, "×", CalcButtonKind.OPERATOR) { vm.onOperator("×") }
        "sub" -> CalcButtonSpec(id, "−", CalcButtonKind.OPERATOR) { vm.onOperator("−") }
        "add" -> CalcButtonSpec(id, "+", CalcButtonKind.OPERATOR) { vm.onOperator("+") }
        "dot" -> CalcButtonSpec(id, ".", CalcButtonKind.NUMBER) { vm.onDecimalPoint() }
        "eq" -> CalcButtonSpec(id, "=", CalcButtonKind.EQUALS) { vm.onEquals() }
        else -> CalcButtonSpec(id, id, CalcButtonKind.NUMBER) { vm.onDigit(id) }
    }
}

@Composable
fun CalculatorScreen(vm: CalculatorViewModel, isDarkTheme: Boolean) {
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val isRu = vm.language == AppLanguage.RU

    val historyChipIndex = vm.topBarOrder.indexOf(TopBarItem.HISTORY).coerceIn(0, 2)
    val settingsChipIndex = vm.topBarOrder.indexOf(TopBarItem.SETTINGS).coerceIn(0, 2)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val isLandscape = screenW > screenH

        SideEffect {
            vm.onOrientationChanged(isLandscape)
        }

        val (targetStartRow, targetStartCol) = vm.computeActiveGridBounds(vm.isScientificMode)

        val startRowState = animateFloatAsState(
            targetValue = targetStartRow.toFloat(),
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "animatedStartRow"
        )
        val startColState = animateFloatAsState(
            targetValue = targetStartCol.toFloat(),
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "animatedStartCol"
        )

        if (!isLandscape) {
            val fixedKeypadHeight: Dp = minOf(screenH * 0.58f, screenW * 1.20f)

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                CalculatorTopBar(
                    vm = vm,
                    isRu = isRu,
                    showHistory = showHistory,
                    showSettings = showSettings,
                    onToggleHistory = {
                        showHistory = !showHistory
                        if (showHistory) showSettings = false
                    },
                    onToggleSettings = {
                        showSettings = !showSettings
                        if (showSettings) showHistory = false
                    }
                )

                CustomizationBanner(
                    vm = vm,
                    isRu = isRu
                )

                HistoryDropdownPanel(
                    vm = vm,
                    isRu = isRu,
                    visible = showHistory && !vm.isCustomizationMode,
                    chipSlotIndex = historyChipIndex,
                    maxCardHeight = 190.dp,
                    onSelectHistory = { item ->
                        vm.onHistoryItemSelected(item)
                        showHistory = false
                    }
                )

                SettingsDropdownPanel(
                    vm = vm,
                    isRu = isRu,
                    visible = showSettings && !vm.isCustomizationMode,
                    chipSlotIndex = settingsChipIndex,
                    maxCardHeight = 290.dp,
                    onStartCustomization = {
                        showSettings = false
                        showHistory = false
                        vm.toggleCustomizationMode()
                    }
                )

                CalculatorDisplaySection(
                    vm = vm,
                    isRu = isRu,
                    isCompact = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp)
                )

                AnimatedKeypadGrid(
                    vm = vm,
                    startRowState = startRowState,
                    startColState = startColState,
                    isDarkTheme = isDarkTheme,
                    isLandscape = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fixedKeypadHeight)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    CalculatorTopBar(
                        vm = vm,
                        isRu = isRu,
                        showHistory = showHistory,
                        showSettings = showSettings,
                        onToggleHistory = {
                            showHistory = !showHistory
                            if (showHistory) showSettings = false
                        },
                        onToggleSettings = {
                            showSettings = !showSettings
                            if (showSettings) showHistory = false
                        }
                    )

                    CustomizationBanner(
                        vm = vm,
                        isRu = isRu
                    )

                    HistoryDropdownPanel(
                        vm = vm,
                        isRu = isRu,
                        visible = showHistory && !vm.isCustomizationMode,
                        chipSlotIndex = historyChipIndex,
                        maxCardHeight = screenH * 0.72f,
                        onSelectHistory = { item ->
                            vm.onHistoryItemSelected(item)
                            showHistory = false
                        }
                    )

                    SettingsDropdownPanel(
                        vm = vm,
                        isRu = isRu,
                        visible = showSettings && !vm.isCustomizationMode,
                        chipSlotIndex = settingsChipIndex,
                        maxCardHeight = screenH * 0.78f,
                        onStartCustomization = {
                            showSettings = false
                            showHistory = false
                            vm.toggleCustomizationMode()
                        }
                    )

                    CalculatorDisplaySection(
                        vm = vm,
                        isRu = isRu,
                        isCompact = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 2.dp)
                    )
                }

                AnimatedKeypadGrid(
                    vm = vm,
                    startRowState = startRowState,
                    startColState = startColState,
                    isDarkTheme = isDarkTheme,
                    isLandscape = true,
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
fun CalculatorTopBar(
    vm: CalculatorViewModel,
    isRu: Boolean,
    showHistory: Boolean,
    showSettings: Boolean,
    onToggleHistory: () -> Unit,
    onToggleSettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val customMode = vm.isCustomizationMode

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for ((index, item) in vm.topBarOrder.withIndex()) {
            val isSelectedForSwap = customMode && vm.selectedTopBarIndex == index
            val borderModifier = if (customMode) {
                Modifier.border(
                    width = if (isSelectedForSwap) 2.dp else 1.dp,
                    color = if (isSelectedForSwap)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(50)
                )
            } else {
                Modifier
            }

            when (item) {
                TopBarItem.HISTORY -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                onToggleHistory()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .then(borderModifier),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val histText = if (isRu) {
                            if (showHistory && !customMode) "Скрыть" else "${prefix}История (${vm.history.size})"
                        } else {
                            if (showHistory && !customMode) "Hide" else "${prefix}History (${vm.history.size})"
                        }
                        Text(
                            text = histText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TopBarItem.MODE -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                vm.toggleScientificMode()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .then(borderModifier),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (vm.isScientificMode)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (vm.isScientificMode)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val sciText = if (isRu) {
                            if (vm.isScientificMode) "${prefix}f(x) Обычн." else "${prefix}f(x) Инж."
                        } else {
                            if (vm.isScientificMode) "${prefix}f(x) Basic" else "${prefix}f(x) Sci"
                        }
                        Text(
                            text = sciText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                TopBarItem.SETTINGS -> {
                    FilledTonalButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onTopBarChipTappedInCustomMode(index)
                            } else {
                                onToggleSettings()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .then(borderModifier),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        val prefix = if (customMode) "⇄ " else ""
                        val settingsText = if (isRu) {
                            if (showSettings && !customMode) "⚙ Скрыть" else "${prefix}⚙ Настройки"
                        } else {
                            if (showSettings && !customMode) "⚙ Hide" else "${prefix}⚙ Settings"
                        }
                        Text(
                            text = settingsText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomizationBanner(
    vm: CalculatorViewModel,
    isRu: Boolean
) {
    AnimatedVisibility(visible = vm.isCustomizationMode) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
            )
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (isRu)
                        "Режим кастомизации: нажмите 2 элемента для обмена местами, × — удалить кнопку"
                    else
                        "Customization: tap 2 items to swap places, × to delete a button",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SettingsOptionChip(
                        label = if (isRu) {
                            if (vm.isScientificMode) "Вид: Инж." else "Вид: Обычн."
                        } else {
                            if (vm.isScientificMode) "View: Sci" else "View: Basic"
                        },
                        selected = vm.isScientificMode,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.toggleScientificMode() }
                    )
                    SettingsOptionChip(
                        label = if (isRu) "↺ Сбросить" else "↺ Reset",
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.resetCustomizations() }
                    )
                    SettingsOptionChip(
                        label = if (isRu) "✓ Готово" else "✓ Done",
                        selected = true,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.exitCustomizationMode() }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryDropdownPanel(
    vm: CalculatorViewModel,
    isRu: Boolean,
    visible: Boolean,
    chipSlotIndex: Int,
    maxCardHeight: Dp,
    onSelectHistory: (HistoryItem) -> Unit
) {
    val pivotX = when (chipSlotIndex) {
        0 -> 0.16f
        1 -> 0.50f
        else -> 0.84f
    }
    val enterAnim = remember(chipSlotIndex) {
        scaleIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 170)
        )
    }

    val exitAnim = remember(chipSlotIndex) {
        scaleOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp, max = maxCardHeight)
                .padding(top = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.80f)
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Журнал вычислений" else "Calculation History",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (vm.history.isNotEmpty()) {
                        Text(
                            text = if (isRu) "Очистить" else "Clear",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { vm.clearHistory() }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                if (vm.history.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Пока нет вычислений" else "No calculations yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(vm.history) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectHistory(item) }
                                    .padding(vertical = 5.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.expression,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "= ${item.result}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
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
fun SettingsDropdownPanel(
    vm: CalculatorViewModel,
    isRu: Boolean,
    visible: Boolean,
    chipSlotIndex: Int,
    maxCardHeight: Dp,
    onStartCustomization: () -> Unit
) {
    val pivotX = when (chipSlotIndex) {
        0 -> 0.16f
        1 -> 0.50f
        else -> 0.84f
    }
    val enterAnim = remember(chipSlotIndex) {
        scaleIn(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            initialScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + expandVertically(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            expandFrom = Alignment.Top
        ) + fadeIn(
            animationSpec = tween(durationMillis = 170)
        )
    }

    val exitAnim = remember(chipSlotIndex) {
        scaleOut(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            targetScale = 0.18f,
            transformOrigin = TransformOrigin(pivotX, 0f)
        ) + shrinkVertically(
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(
            animationSpec = tween(durationMillis = 150)
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterAnim,
        exit = exitAnim
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxCardHeight)
                .padding(top = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // 1. Language (RU / EN)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Язык" else "Language",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        SettingsOptionChip(
                            label = "RU",
                            selected = vm.language == AppLanguage.RU,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateLanguage(AppLanguage.RU) }
                        )
                        SettingsOptionChip(
                            label = "EN",
                            selected = vm.language == AppLanguage.EN,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateLanguage(AppLanguage.EN) }
                        )
                    }
                }

                // 2. Auto-switch to scientific mode on screen rotation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Инж. режим при повороте" else "Auto-Sci on rotate",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.width(120.dp)
                    ) {
                        SettingsOptionChip(
                            label = if (isRu) "Вкл" else "On",
                            selected = vm.autoScientificOnLandscape,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateAutoScientificOnLandscape(true) }
                        )
                        SettingsOptionChip(
                            label = if (isRu) "Выкл" else "Off",
                            selected = !vm.autoScientificOnLandscape,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateAutoScientificOnLandscape(false) }
                        )
                    }
                }

                // 3. Theme Mode
                Text(
                    text = if (isRu) "Режим темы" else "Theme Mode",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = if (isRu) {
                        listOf(
                            ThemeMode.SYSTEM to "Авто",
                            ThemeMode.DARK to "Тёмная",
                            ThemeMode.LIGHT to "Светлая"
                        )
                    } else {
                        listOf(
                            ThemeMode.SYSTEM to "Auto",
                            ThemeMode.DARK to "Dark",
                            ThemeMode.LIGHT to "Light"
                        )
                    }
                    for ((mode, label) in modes) {
                        SettingsOptionChip(
                            label = label,
                            selected = vm.themeMode == mode,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateThemeMode(mode) }
                        )
                    }
                }

                // 4. Visual Color Palette Swatches
                Text(
                    text = if (isRu) "Палитра темы" else "Theme Palette",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val swatchColors = listOf(
                        Triple(ThemePalette.OCEAN, Color(0xFF38BDF8), Color(0xFF0284C7)),
                        Triple(ThemePalette.EMERALD, Color(0xFF34D399), Color(0xFF059669)),
                        Triple(ThemePalette.AMETHYST, Color(0xFFC084FC), Color(0xFF7C3AED)),
                        Triple(ThemePalette.AMBER, Color(0xFFFBBF24), Color(0xFFD97706)),
                        Triple(ThemePalette.ROSE, Color(0xFFFB7185), Color(0xFFE11D48))
                    )
                    for ((palette, topCol, botCol) in swatchColors) {
                        ColorPaletteSwatch(
                            topColor = topCol,
                            bottomColor = botCol,
                            selected = vm.themePalette == palette,
                            onClick = { vm.updateThemePalette(palette) }
                        )
                    }
                }

                // 5. Button Shape
                Text(
                    text = if (isRu) "Форма кнопок" else "Button Shape",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val shapes = if (isRu) {
                        listOf(
                            ButtonShapeMode.ROUND to "Круглые",
                            ButtonShapeMode.ROUNDED to "Скруглённые",
                            ButtonShapeMode.SQUARE to "Квадратные"
                        )
                    } else {
                        listOf(
                            ButtonShapeMode.ROUND to "Round",
                            ButtonShapeMode.ROUNDED to "Rounded",
                            ButtonShapeMode.SQUARE to "Square"
                        )
                    }
                    for ((shapeOption, label) in shapes) {
                        SettingsOptionChip(
                            label = label,
                            selected = vm.buttonShapeMode == shapeOption,
                            modifier = Modifier.weight(1f),
                            onClick = { vm.updateButtonShapeMode(shapeOption) }
                        )
                    }
                }

                // 6. Customization Mode Entry & Reset
                Text(
                    text = if (isRu) "Кастомизация" else "Customization",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SettingsOptionChip(
                        label = if (isRu) "✎ Настроить кнопки и плашки" else "✎ Customize layout",
                        selected = vm.isCustomizationMode,
                        modifier = Modifier.weight(1.6f),
                        onClick = onStartCustomization
                    )
                    SettingsOptionChip(
                        label = if (isRu) "↺ Сброс" else "↺ Reset",
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = { vm.resetCustomizations() }
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorDisplaySection(
    vm: CalculatorViewModel,
    isRu: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.End
    ) {
        val exprText = if (vm.expression.isEmpty()) "0" else vm.expression
        val exprFontSize = when {
            exprText.length > 22 -> if (isCompact) 20.sp else 24.sp
            exprText.length > 14 -> if (isCompact) 26.sp else 34.sp
            else -> if (isCompact) 34.sp else 44.sp
        }
        Text(
            text = exprText,
            fontSize = exprFontSize,
            lineHeight = exprFontSize * 1.12f,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))

        val previewStr = vm.localizedPreview()
        Text(
            text = if (previewStr.isNotEmpty()) "= $previewStr" else "",
            fontSize = if (isCompact) 18.sp else 22.sp,
            fontWeight = FontWeight.Medium,
            color = if (vm.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 24.dp else 28.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.onToggleSign()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isRu) "+/− Знак" else "+/− Sign",
                    fontSize = 13.sp
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    vm.onBackspace()
                },
                enabled = vm.expression.isNotEmpty() || vm.isError
            ) {
                Text(
                    text = "⌫",
                    fontSize = if (isCompact) 22.sp else 24.sp,
                    color = if (vm.expression.isNotEmpty() || vm.isError)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun AnimatedKeypadGrid(
    vm: CalculatorViewModel,
    startRowState: State<Float>,
    startColState: State<Float>,
    isDarkTheme: Boolean,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shapeMode = vm.buttonShapeMode
    val customMode = vm.isCustomizationMode
    val selectedCell = vm.selectedGridIndex
    val gridIds = vm.gridLayout

    val buttonShape: Shape = remember(shapeMode) {
        when (shapeMode) {
            ButtonShapeMode.ROUND -> CircleShape
            ButtonShapeMode.ROUNDED -> RoundedCornerShape(18.dp)
            ButtonShapeMode.SQUARE -> RoundedCornerShape(5.dp)
        }
    }

    val sciFont = if (isLandscape) 13.sp else 14.sp
    val mainOpFont = if (isLandscape) 20.sp else 24.sp
    val mainNumFont = if (isLandscape) 17.sp else 20.sp

    val (activeStartRow, activeStartCol) = vm.computeActiveGridBounds(vm.isScientificMode)

    Layout(
        modifier = modifier,
        content = {
            for (cellIdx in 0 until CalculatorViewModel.GRID_SIZE) {
                val r = cellIdx / CalculatorViewModel.GRID_COLS
                val c = cellIdx % CalculatorViewModel.GRID_COLS
                val isCellVisibleInMode = r >= activeStartRow && c >= activeStartCol
                val btnId = gridIds[cellIdx]
                val isSelected = customMode && selectedCell == cellIdx

                if (btnId != null) {
                    val spec = resolveButtonSpec(btnId, vm)
                    val baseFont = when (spec.kind) {
                        CalcButtonKind.OPERATOR, CalcButtonKind.EQUALS -> mainOpFont
                        CalcButtonKind.SCIENTIFIC -> sciFont
                        else -> mainNumFont
                    }
                    CalculatorKeyButton(
                        spec = spec,
                        shape = buttonShape,
                        shapeMode = shapeMode,
                        isDarkTheme = isDarkTheme,
                        enabled = isCellVisibleInMode,
                        isCustomizationMode = customMode,
                        isSelectedForSwap = isSelected,
                        fontSizeOverride = baseFont,
                        onTap = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (customMode) {
                                vm.onGridCellTappedInCustomMode(cellIdx)
                            } else {
                                spec.onClick()
                            }
                        },
                        onDelete = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            vm.deleteGridButtonAt(cellIdx)
                        }
                    )
                } else {
                    EmptyGridSlot(
                        shape = buttonShape,
                        visible = customMode && isCellVisibleInMode,
                        isSelected = isSelected,
                        onClick = {
                            if (customMode && isCellVisibleInMode) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                vm.onGridCellTappedInCustomMode(cellIdx)
                            }
                        }
                    )
                }
            }
        }
    ) { measurables, constraints ->
        val startR = startRowState.value
        val startC = startColState.value
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()

        val gapPx = if (isLandscape) {
            6.dp.toPx()
        } else {
            val sciFactor = (1f - startR.coerceIn(0f, 1f))
            (10.dp.toPx() * (1f - sciFactor)) + (8.dp.toPx() * sciFactor)
        }

        val visibleCols = (CalculatorViewModel.GRID_COLS.toFloat() - startC).coerceAtLeast(1f)
        val visibleRows = (CalculatorViewModel.GRID_ROWS.toFloat() - startR).coerceAtLeast(1f)

        val cellW = (areaW - gapPx * (visibleCols - 1f)) / visibleCols
        val cellH = (areaH - gapPx * (visibleRows - 1f)) / visibleRows
        val stepX = cellW + gapPx
        val stepY = cellH + gapPx

        val isRound = shapeMode == ButtonShapeMode.ROUND
        val btnW = if (isRound) min(cellW, cellH).roundToInt().coerceAtLeast(1) else cellW.roundToInt().coerceAtLeast(1)
        val btnH = if (isRound) btnW else cellH.roundToInt().coerceAtLeast(1)
        val offsetInCellX = (cellW - btnW) * 0.5f
        val offsetInCellY = (cellH - btnH) * 0.5f

        val childConstraints = Constraints.fixed(btnW, btnH)
        val placeables = measurables.map { it.measure(childConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            for (cellIdx in 0 until CalculatorViewModel.GRID_SIZE) {
                val r = cellIdx / CalculatorViewModel.GRID_COLS
                val c = cellIdx % CalculatorViewModel.GRID_COLS

                val rowVis = (r.toFloat() - startR + 1f).coerceIn(0f, 1f)
                val colVis = (c.toFloat() - startC + 1f).coerceIn(0f, 1f)
                val visibility = min(rowVis, colVis)

                if (visibility > 0.005f) {
                    val x = stepX * (c.toFloat() - startC) + offsetInCellX
                    val y = stepY * (r.toFloat() - startR) + offsetInCellY
                    if (visibility < 0.999f) {
                        val s = 0.72f + 0.28f * visibility
                        placeables[cellIdx].placeWithLayer(x.roundToInt(), y.roundToInt()) {
                            alpha = visibility
                            scaleX = s
                            scaleY = s
                        }
                    } else {
                        placeables[cellIdx].place(x.roundToInt(), y.roundToInt())
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyGridSlot(
    shape: Shape,
    visible: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    if (!visible) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⇄",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun ColorPaletteSwatch(
    topColor: Color,
    bottomColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "swatchScale"
    )
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(34.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(topColor, bottomColor),
                    start = Offset.Zero,
                    end = Offset(100f, 100f)
                )
            )
            .border(
                width = if (selected) 2.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SettingsOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CalculatorKeyButton(
    spec: CalcButtonSpec,
    shape: Shape,
    shapeMode: ButtonShapeMode,
    isDarkTheme: Boolean,
    enabled: Boolean = true,
    isCustomizationMode: Boolean = false,
    isSelectedForSwap: Boolean = false,
    modifier: Modifier = Modifier,
    fontSizeOverride: TextUnit = 20.sp,
    onTap: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowState = animateFloatAsState(
        targetValue = if (isPressed || isSelectedForSwap) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 65 else 240),
        label = "buttonGlow"
    )
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (isSelectedForSwap) 0.93f else 1f,
        animationSpec = tween(durationMillis = 80),
        label = "buttonScale"
    )

    val (baseBgColor, contentColor) = when (spec.kind) {
        CalcButtonKind.NUMBER -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalcButtonKind.ACTION -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcButtonKind.SCIENTIFIC -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.primary
        )
        CalcButtonKind.OPERATOR -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcButtonKind.CLEAR -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        CalcButtonKind.EQUALS -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    val glowColor = if (isSelectedForSwap) {
        MaterialTheme.colorScheme.primary
    } else if (isDarkTheme) {
        Color(0xFFE8F4FF)
    } else {
        Color(0xFF0D2B6B)
    }

    val topAlpha = if (isDarkTheme) 0.68f else 0.78f
    val bottomAlpha = if (isDarkTheme) 0.46f else 0.56f
    val topHighlightTint = if (isDarkTheme) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.55f)
    val bottomShadeTint = if (isDarkTheme) Color.Black.copy(alpha = 0.35f) else Color(0xFF0F172A).copy(alpha = 0.14f)

    val topSurfaceColor = lerp(baseBgColor.copy(alpha = topAlpha), topHighlightTint, 0.22f)
    val bottomSurfaceColor = lerp(baseBgColor.copy(alpha = bottomAlpha), bottomShadeTint, 0.25f)

    val topRimBase = if (isDarkTheme) Color.White.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.85f)
    val bottomRimBase = if (isDarkTheme) Color.Black.copy(alpha = 0.60f) else Color(0xFF1E293B).copy(alpha = 0.24f)

    Box(
        modifier = modifier
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
            }
            .drawWithCache {
                val btnCenter = Offset(size.width * 0.5f, size.height * 0.5f)
                val shadowRadius = size.maxDimension * 0.58f
                val shadowCenter = Offset(btnCenter.x, btnCenter.y + 3.dp.toPx())
                val shadowBrush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (isDarkTheme) 0.34f else 0.13f),
                        Color.Transparent
                    ),
                    center = shadowCenter,
                    radius = shadowRadius
                )
                val glowRadius = size.maxDimension * 0.72f
                val outerGlowBrush = Brush.radialGradient(
                    colors = listOf(
                        glowColor.copy(alpha = if (isDarkTheme) 0.56f else 0.46f),
                        glowColor.copy(alpha = if (isDarkTheme) 0.22f else 0.18f),
                        Color.Transparent
                    ),
                    center = btnCenter,
                    radius = glowRadius
                )
                val cornerPx = when (shapeMode) {
                    ButtonShapeMode.ROUND -> size.minDimension * 0.5f
                    ButtonShapeMode.ROUNDED -> 18.dp.toPx()
                    ButtonShapeMode.SQUARE -> 5.dp.toPx()
                }
                val cornerRadius = CornerRadius(cornerPx, cornerPx)
                val surfaceBrush = Brush.verticalGradient(
                    colors = listOf(topSurfaceColor, bottomSurfaceColor)
                )
                val borderBrush = Brush.linearGradient(
                    colors = listOf(topRimBase, bottomRimBase),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val glowBorderBrush = Brush.linearGradient(
                    colors = listOf(glowColor.copy(alpha = 0.9f), glowColor.copy(alpha = 0.7f)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
                val borderStroke = Stroke(width = if (isSelectedForSwap) 2.4.dp.toPx() else 1.3.dp.toPx())

                onDrawBehind {
                    val gp = glowState.value
                    drawCircle(
                        brush = shadowBrush,
                        radius = shadowRadius,
                        center = shadowCenter
                    )
                    if (gp > 0.01f) {
                        drawCircle(
                            brush = outerGlowBrush,
                            radius = glowRadius,
                            center = center,
                            alpha = gp
                        )
                    }
                    drawRoundRect(
                        brush = surfaceBrush,
                        cornerRadius = cornerRadius
                    )
                    if (gp > 0.01f) {
                        drawRoundRect(
                            color = glowColor.copy(alpha = (if (isDarkTheme) 0.26f else 0.20f) * gp),
                            cornerRadius = cornerRadius
                        )
                    }
                    drawRoundRect(
                        brush = borderBrush,
                        cornerRadius = cornerRadius,
                        style = borderStroke
                    )
                    if (gp > 0.01f) {
                        drawRoundRect(
                            brush = glowBorderBrush,
                            cornerRadius = cornerRadius,
                            style = borderStroke,
                            alpha = gp * 0.85f
                        )
                    }
                }
            }
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onTap
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = spec.label,
            color = contentColor,
            fontSize = fontSizeOverride,
            fontWeight = if (spec.kind == CalcButtonKind.NUMBER) FontWeight.Normal else FontWeight.SemiBold,
            maxLines = 1
        )

        if (isCustomizationMode && enabled) {
            // Red delete circle at the top-center of the button with a centered vector cross filling the circle
            Canvas(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 3.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDC2626))
                    .border(1.2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    .clickable(onClick = onDelete)
            ) {
                val cx = size.width * 0.5f
                val cy = size.height * 0.5f
                val arm = size.minDimension * 0.28f
                val strokeW = 2.2.dp.toPx()
                drawLine(
                    color = Color.White,
                    start = Offset(cx - arm, cy - arm),
                    end = Offset(cx + arm, cy + arm),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(cx + arm, cy - arm),
                    end = Offset(cx - arm, cy + arm),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
