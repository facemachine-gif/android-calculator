package com.example.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CalculatorViewModel = viewModel()
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
    val label: String,
    val kind: CalcButtonKind,
    val onClick: () -> Unit
)

@Composable
fun CalculatorScreen(vm: CalculatorViewModel, isDarkTheme: Boolean) {
    var showHistory by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isRu = vm.language == AppLanguage.RU

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Constant keypad area height — never changes when switching between standard and scientific modes
        val fixedKeypadHeight: Dp = minOf(maxHeight * 0.58f, maxWidth * 1.20f)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: [History] — [Engineering Mode] — [Gear Settings]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = {
                        showHistory = !showHistory
                        if (showHistory) showSettings = false
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val histText = if (isRu) {
                        if (showHistory) "Скрыть" else "История (${vm.history.size})"
                    } else {
                        if (showHistory) "Hide" else "History (${vm.history.size})"
                    }
                    Text(
                        text = histText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        vm.toggleScientificMode()
                    },
                    modifier = Modifier.weight(1f),
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val sciText = if (isRu) {
                        if (vm.isScientificMode) "f(x) Обычн." else "f(x) Инж."
                    } else {
                        if (vm.isScientificMode) "f(x) Basic" else "f(x) Sci"
                    }
                    Text(
                        text = sciText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalButton(
                    onClick = {
                        showSettings = !showSettings
                        if (showSettings) showHistory = false
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    val settingsText = if (isRu) {
                        if (showSettings) "⚙ Скрыть" else "⚙ Настройки"
                    } else {
                        if (showSettings) "⚙ Hide" else "⚙ Settings"
                    }
                    Text(
                        text = settingsText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Expandable History Panel
            AnimatedVisibility(visible = showHistory) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 190.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(vm.history) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                vm.onHistoryItemSelected(item)
                                                showHistory = false
                                            }
                                            .padding(vertical = 6.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.expression,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 15.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "= ${item.result}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Expandable Settings Panel
            AnimatedVisibility(visible = showSettings) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 250.dp)
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                modifier = Modifier.width(130.dp)
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

                        Text(
                            text = if (isRu) "Режим темы" else "Theme Mode",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                        Text(
                            text = if (isRu) "Форма кнопок" else "Button Shape",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                    }
                }
            }

            // Display Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.End
            ) {
                val exprText = if (vm.expression.isEmpty()) "0" else vm.expression
                val exprFontSize = when {
                    exprText.length > 22 -> 24.sp
                    exprText.length > 14 -> 34.sp
                    else -> 44.sp
                }
                Text(
                    text = exprText,
                    fontSize = exprFontSize,
                    lineHeight = exprFontSize * 1.15f,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                val previewStr = vm.localizedPreview()
                Text(
                    text = if (previewStr.isNotEmpty()) "= $previewStr" else "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (vm.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRu) "+/− Знак" else "+/− Sign",
                            fontSize = 14.sp
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
                            fontSize = 24.sp,
                            color = if (vm.expression.isNotEmpty() || vm.isError)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // State holder for smooth transition — read ONLY inside Layout/Placement, not in Composition!
            val sciProgressState = animateFloatAsState(
                targetValue = if (vm.isScientificMode) 1f else 0f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "scientificLayoutProgress"
            )

            AnimatedKeypadGrid(
                vm = vm,
                sciProgressState = sciProgressState,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fixedKeypadHeight)
            )
        }
    }
}

@Composable
fun AnimatedKeypadGrid(
    vm: CalculatorViewModel,
    sciProgressState: State<Float>,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shapeMode = vm.buttonShapeMode
    val buttonShape: Shape = remember(shapeMode) {
        when (shapeMode) {
            ButtonShapeMode.ROUND -> CircleShape
            ButtonShapeMode.ROUNDED -> RoundedCornerShape(18.dp)
            ButtonShapeMode.SQUARE -> RoundedCornerShape(5.dp)
        }
    }

    val topSciButtons = remember(vm.isDegreeMode) {
        listOf(
            CalcButtonSpec(if (vm.isDegreeMode) "DEG" else "RAD", CalcButtonKind.SCIENTIFIC) { vm.toggleDegreeMode() },
            CalcButtonSpec("sin", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sin") },
            CalcButtonSpec("cos", CalcButtonKind.SCIENTIFIC) { vm.onFunction("cos") },
            CalcButtonSpec("tan", CalcButtonKind.SCIENTIFIC) { vm.onFunction("tan") },
            CalcButtonSpec("ln", CalcButtonKind.SCIENTIFIC) { vm.onFunction("ln") }
        )
    }

    val leftSciButtons = remember {
        listOf(
            CalcButtonSpec("√x", CalcButtonKind.SCIENTIFIC) { vm.onFunction("sqrt") },
            CalcButtonSpec("xʸ", CalcButtonKind.SCIENTIFIC) { vm.onPower() },
            CalcButtonSpec("log", CalcButtonKind.SCIENTIFIC) { vm.onFunction("log") },
            CalcButtonSpec("n!", CalcButtonKind.SCIENTIFIC) { vm.onFactorial() },
            CalcButtonSpec("π", CalcButtonKind.SCIENTIFIC) { vm.onConstant("π") }
        )
    }

    val mainButtons = remember {
        listOf(
            CalcButtonSpec("AC", CalcButtonKind.CLEAR) { vm.onClear() },
            CalcButtonSpec("( )", CalcButtonKind.ACTION) { vm.onParentheses() },
            CalcButtonSpec("%", CalcButtonKind.ACTION) { vm.onPercent() },
            CalcButtonSpec("÷", CalcButtonKind.OPERATOR) { vm.onOperator("÷") },
            CalcButtonSpec("7", CalcButtonKind.NUMBER) { vm.onDigit("7") },
            CalcButtonSpec("8", CalcButtonKind.NUMBER) { vm.onDigit("8") },
            CalcButtonSpec("9", CalcButtonKind.NUMBER) { vm.onDigit("9") },
            CalcButtonSpec("×", CalcButtonKind.OPERATOR) { vm.onOperator("×") },
            CalcButtonSpec("4", CalcButtonKind.NUMBER) { vm.onDigit("4") },
            CalcButtonSpec("5", CalcButtonKind.NUMBER) { vm.onDigit("5") },
            CalcButtonSpec("6", CalcButtonKind.NUMBER) { vm.onDigit("6") },
            CalcButtonSpec("−", CalcButtonKind.OPERATOR) { vm.onOperator("−") },
            CalcButtonSpec("1", CalcButtonKind.NUMBER) { vm.onDigit("1") },
            CalcButtonSpec("2", CalcButtonKind.NUMBER) { vm.onDigit("2") },
            CalcButtonSpec("3", CalcButtonKind.NUMBER) { vm.onDigit("3") },
            CalcButtonSpec("+", CalcButtonKind.OPERATOR) { vm.onOperator("+") },
            CalcButtonSpec("00", CalcButtonKind.NUMBER) { vm.onDigit("00") },
            CalcButtonSpec("0", CalcButtonKind.NUMBER) { vm.onDigit("0") },
            CalcButtonSpec(".", CalcButtonKind.NUMBER) { vm.onDecimalPoint() },
            CalcButtonSpec("=", CalcButtonKind.EQUALS) { vm.onEquals() }
        )
    }

    // All 30 buttons remain permanently composed in the tree so toggling modes never triggers composition spikes
    Layout(
        modifier = modifier,
        content = {
            for (btn in topSciButtons) {
                CalculatorKeyButton(
                    spec = btn,
                    shape = buttonShape,
                    shapeMode = shapeMode,
                    isDarkTheme = isDarkTheme,
                    enabled = vm.isScientificMode,
                    fontSizeOverride = 14.sp,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        btn.onClick()
                    }
                )
            }
            for (btn in leftSciButtons) {
                CalculatorKeyButton(
                    spec = btn,
                    shape = buttonShape,
                    shapeMode = shapeMode,
                    isDarkTheme = isDarkTheme,
                    enabled = vm.isScientificMode,
                    fontSizeOverride = 14.sp,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        btn.onClick()
                    }
                )
            }
            for (btn in mainButtons) {
                val baseFont = if (btn.kind == CalcButtonKind.OPERATOR || btn.kind == CalcButtonKind.EQUALS) {
                    24.sp
                } else {
                    20.sp
                }
                CalculatorKeyButton(
                    spec = btn,
                    shape = buttonShape,
                    shapeMode = shapeMode,
                    isDarkTheme = isDarkTheme,
                    enabled = true,
                    fontSizeOverride = baseFont,
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        btn.onClick()
                    }
                )
            }
        }
    ) { measurables, constraints ->
        val progress = sciProgressState.value
        val areaW = constraints.maxWidth.toFloat()
        val areaH = constraints.maxHeight.toFloat()
        val gapPx = (10.dp.toPx() * (1f - progress)) + (8.dp.toPx() * progress)

        val cols = 4f + progress
        val rows = 5f + progress

        val cellW = (areaW - gapPx * (cols - 1f)) / cols
        val cellH = (areaH - gapPx * (rows - 1f)) / rows
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
            val sciScale = 0.72f + 0.28f * progress

            // 1. Place Top Scientific Row (indices 0..4)
            if (progress > 0.005f) {
                for (idx in 0 until 5) {
                    val x = (if (idx == 0) 0f else stepX * ((idx - 1).toFloat() + progress)) + offsetInCellX
                    val y = offsetInCellY
                    placeables[idx].placeWithLayer(x.roundToInt(), y.roundToInt()) {
                        alpha = progress
                        scaleX = sciScale
                        scaleY = sciScale
                    }
                }

                // 2. Place Left Scientific Column (indices 5..9)
                for (rIdx in 0 until 5) {
                    val x = offsetInCellX
                    val y = stepY * (rIdx.toFloat() + progress) + offsetInCellY
                    placeables[5 + rIdx].placeWithLayer(x.roundToInt(), y.roundToInt()) {
                        alpha = progress
                        scaleX = sciScale
                        scaleY = sciScale
                    }
                }
            }

            // 3. Place Main 5x4 Buttons (indices 10..29)
            for (rIdx in 0 until 5) {
                for (cIdx in 0 until 4) {
                    val x = stepX * (cIdx.toFloat() + progress) + offsetInCellX
                    val y = stepY * (rIdx.toFloat() + progress) + offsetInCellY
                    placeables[10 + rIdx * 4 + cIdx].place(x.roundToInt(), y.roundToInt())
                }
            }
        }
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
            .size(36.dp)
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
                fontSize = 15.sp,
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
            .padding(vertical = 7.dp, horizontal = 4.dp),
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
    modifier: Modifier = Modifier,
    fontSizeOverride: TextUnit = 20.sp,
    onTap: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowState = animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPressed) 65 else 240),
        label = "buttonGlow"
    )
    val scaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
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

    val glowColor = if (isDarkTheme) Color(0xFFE8F4FF) else Color(0xFF0D2B6B)

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
                val borderStroke = Stroke(width = 1.3.dp.toPx())

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
                            alpha = gp * 0.75f
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
    }
}
