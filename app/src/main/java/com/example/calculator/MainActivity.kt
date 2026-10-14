package com.example.calculator

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: CalculatorViewModel = viewModel()
            CalculatorAppTheme(themeMode = vm.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalculatorScreen(vm = vm)
                }
            }
        }
    }
}

@Composable
fun CalculatorAppTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF00263A),
            primaryContainer = Color(0xFF0284C7),
            onPrimaryContainer = Color(0xFFE0F2FE),
            secondaryContainer = Color(0xFF1E293B),
            onSecondaryContainer = Color(0xFFE2E8F0),
            tertiaryContainer = Color(0xFF334155),
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
        else -> lightColorScheme(
            primary = Color(0xFF0284C7),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF0284C7),
            onPrimaryContainer = Color.White,
            secondaryContainer = Color(0xFFF1F5F9),
            onSecondaryContainer = Color(0xFF0F172A),
            tertiaryContainer = Color(0xFFE0F2FE),
            onTertiaryContainer = Color(0xFF0369A1),
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFFB91C1C),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

enum class CalcButtonKind {
    NUMBER, OPERATOR, ACTION, CLEAR, EQUALS
}

data class CalcButtonSpec(
    val label: String,
    val kind: CalcButtonKind,
    val onClick: () -> Unit
)

@Composable
fun CalculatorScreen(vm: CalculatorViewModel) {
    var showHistory by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar: History toggle and Theme selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { showHistory = !showHistory },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (showHistory) "Скрыть историю" else "История (${vm.history.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            FilledTonalButton(
                onClick = { vm.cycleThemeMode() },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                val modeLabel = when (vm.themeMode) {
                    ThemeMode.SYSTEM -> "Тема: Авто"
                    ThemeMode.DARK -> "Тема: Тёмная"
                    ThemeMode.LIGHT -> "Тема: Светлая"
                }
                Text(text = modeLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Expandable History Panel
        AnimatedVisibility(visible = showHistory) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 210.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Журнал вычислений",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (vm.history.isNotEmpty()) {
                            Text(
                                text = "Очистить",
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
                                text = "Пока нет вычислений",
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

        // Display Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            val exprText = if (vm.expression.isEmpty()) "0" else vm.expression
            val exprFontSize = when {
                exprText.length > 18 -> 28.sp
                exprText.length > 12 -> 36.sp
                else -> 48.sp
            }
            Text(
                text = exprText,
                fontSize = exprFontSize,
                lineHeight = exprFontSize * 1.15f,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.End,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (vm.preview.isNotEmpty()) "= ${vm.preview}" else "",
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = if (vm.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            )

            // Backspace Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        vm.onToggleSign()
                    }
                ) {
                    Text(text = "+/− Знак", fontSize = 15.sp)
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

        // Keypad Grid (5 rows x 4 columns)
        val rows = listOf(
            listOf(
                CalcButtonSpec("AC", CalcButtonKind.CLEAR) { vm.onClear() },
                CalcButtonSpec("( )", CalcButtonKind.ACTION) { vm.onParentheses() },
                CalcButtonSpec("%", CalcButtonKind.ACTION) { vm.onPercent() },
                CalcButtonSpec("÷", CalcButtonKind.OPERATOR) { vm.onOperator("÷") }
            ),
            listOf(
                CalcButtonSpec("7", CalcButtonKind.NUMBER) { vm.onDigit("7") },
                CalcButtonSpec("8", CalcButtonKind.NUMBER) { vm.onDigit("8") },
                CalcButtonSpec("9", CalcButtonKind.NUMBER) { vm.onDigit("9") },
                CalcButtonSpec("×", CalcButtonKind.OPERATOR) { vm.onOperator("×") }
            ),
            listOf(
                CalcButtonSpec("4", CalcButtonKind.NUMBER) { vm.onDigit("4") },
                CalcButtonSpec("5", CalcButtonKind.NUMBER) { vm.onDigit("5") },
                CalcButtonSpec("6", CalcButtonKind.NUMBER) { vm.onDigit("6") },
                CalcButtonSpec("−", CalcButtonKind.OPERATOR) { vm.onOperator("−") }
            ),
            listOf(
                CalcButtonSpec("1", CalcButtonKind.NUMBER) { vm.onDigit("1") },
                CalcButtonSpec("2", CalcButtonKind.NUMBER) { vm.onDigit("2") },
                CalcButtonSpec("3", CalcButtonKind.NUMBER) { vm.onDigit("3") },
                CalcButtonSpec("+", CalcButtonKind.OPERATOR) { vm.onOperator("+") }
            ),
            listOf(
                CalcButtonSpec("00", CalcButtonKind.NUMBER) { vm.onDigit("00") },
                CalcButtonSpec("0", CalcButtonKind.NUMBER) { vm.onDigit("0") },
                CalcButtonSpec(".", CalcButtonKind.NUMBER) { vm.onDecimalPoint() },
                CalcButtonSpec("=", CalcButtonKind.EQUALS) { vm.onEquals() }
            )
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (btn in row) {
                        CalculatorKeyButton(
                            spec = btn,
                            modifier = Modifier
                                .weight(1f)
                                .height(68.dp),
                            onTap = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                btn.onClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorKeyButton(
    spec: CalcButtonSpec,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    val (bgColor, contentColor) = when (spec.kind) {
        CalcButtonKind.NUMBER -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalcButtonKind.ACTION -> Pair(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer
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

    Button(
        onClick = onTap,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = spec.label,
            fontSize = if (spec.kind == CalcButtonKind.OPERATOR || spec.kind == CalcButtonKind.EQUALS) 28.sp else 24.sp,
            fontWeight = if (spec.kind == CalcButtonKind.NUMBER) FontWeight.Normal else FontWeight.SemiBold
        )
    }
}
