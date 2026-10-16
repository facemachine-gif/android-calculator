package com.example.calculator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class HistoryItem(
    val expression: String,
    val result: String
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class ThemePalette {
    OCEAN, EMERALD, AMETHYST, AMBER, ROSE
}

enum class ButtonShapeMode {
    ROUND, ROUNDED, SQUARE
}

enum class AppLanguage {
    RU, EN
}

class CalculatorViewModel : ViewModel() {

    var expression by mutableStateOf("")
        private set

    var preview by mutableStateOf("")
        private set

    var isError by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var themePalette by mutableStateOf(ThemePalette.OCEAN)
        private set

    var buttonShapeMode by mutableStateOf(ButtonShapeMode.ROUNDED)
        private set

    var language by mutableStateOf(AppLanguage.RU)
        private set

    var isScientificMode by mutableStateOf(false)
        private set

    var portraitScientificMode by mutableStateOf(false)
        private set

    var autoScientificOnLandscape by mutableStateOf(true)
        private set

    var isDegreeMode by mutableStateOf(true)
        private set

    var isInverseTrig by mutableStateOf(false)
        private set

    var testResults by mutableStateOf(CalculatorEngine.runSelfTests())
        private set

    val history = mutableStateListOf<HistoryItem>()

    private var lastKnownLandscape: Boolean? = null
    private var settingsLoaded: Boolean = false

    private val operators = setOf('+', '−', '×', '÷', '^')
    private val functionPrefixes = listOf(
        "asin(", "acos(", "atan(",
        "sqrt(", "cbrt(",
        "sin(", "cos(", "tan(", "log(", "abs(",
        "ln(", "√(", "∛("
    )

    fun loadSavedSettings(
        savedThemeMode: ThemeMode,
        savedPalette: ThemePalette,
        savedShape: ButtonShapeMode,
        savedLang: AppLanguage,
        savedDegreeMode: Boolean,
        savedAutoSciOnLandscape: Boolean
    ) {
        if (settingsLoaded) return
        settingsLoaded = true
        themeMode = savedThemeMode
        themePalette = savedPalette
        buttonShapeMode = savedShape
        language = savedLang
        isDegreeMode = savedDegreeMode
        autoScientificOnLandscape = savedAutoSciOnLandscape
    }

    fun onOrientationChanged(isLandscape: Boolean) {
        if (lastKnownLandscape == isLandscape) return
        val prevLandscape = lastKnownLandscape
        lastKnownLandscape = isLandscape

        if (autoScientificOnLandscape) {
            if (isLandscape) {
                if (prevLandscape == false) {
                    portraitScientificMode = isScientificMode
                }
                isScientificMode = true
            } else {
                isScientificMode = portraitScientificMode
            }
        }
    }

    fun runDiagnostics() {
        testResults = CalculatorEngine.runSelfTests()
    }

    fun loadTestExpression(testCase: CalculatorEngine.SelfTestCase) {
        expression = testCase.expression
        isDegreeMode = testCase.isDegrees
        isError = false
        when (val res = CalculatorEngine.evaluate(expression, isDegrees = isDegreeMode)) {
            is CalculatorEngine.EvalResult.Success -> {
                preview = res.formatted
                isError = false
            }
            is CalculatorEngine.EvalResult.Error -> {
                preview = res.message
                isError = true
            }
        }
    }

    fun cycleThemeMode() {
        themeMode = when (themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
    }

    fun updateThemePalette(palette: ThemePalette) {
        themePalette = palette
    }

    fun updateButtonShapeMode(shapeMode: ButtonShapeMode) {
        buttonShapeMode = shapeMode
    }

    fun updateLanguage(lang: AppLanguage) {
        language = lang
    }

    fun updateAutoScientificOnLandscape(enabled: Boolean) {
        autoScientificOnLandscape = enabled
        if (enabled) {
            if (lastKnownLandscape == true) {
                isScientificMode = true
            } else if (lastKnownLandscape == false) {
                isScientificMode = portraitScientificMode
            }
        }
    }

    fun toggleScientificMode() {
        isScientificMode = !isScientificMode
        if (lastKnownLandscape != true) {
            portraitScientificMode = isScientificMode
        }
    }

    fun toggleDegreeMode() {
        isDegreeMode = !isDegreeMode
        updatePreview()
    }

    fun toggleInverseTrig() {
        isInverseTrig = !isInverseTrig
    }

    fun localizedPreview(): String {
        if (preview.isEmpty()) return ""
        if (!isError || language == AppLanguage.RU) return preview
        return when (preview) {
            "Деление на ноль" -> "Division by zero"
            "Не определено" -> "Undefined"
            "Вне области определения" -> "Out of domain"
            "Логарифм от <= 0" -> "Logarithm of <= 0"
            "Корень из отрицательного числа" -> "Root of negative number"
            "Факториал только для целых >= 0" -> "Factorial requires int >= 0"
            "Переполнение числа" -> "Number overflow"
            else -> "Expression error"
        }
    }

    fun onDigit(digit: String) {
        if (isError) {
            expression = ""
            isError = false
        }
        expression += digit
        updatePreview()
    }

    fun onDecimalPoint() {
        if (isError) {
            expression = "0."
            isError = false
            updatePreview()
            return
        }
        val currentNumber = getLastNumberSegment(expression)
        if ('.' in currentNumber) return
        expression += if (currentNumber.isEmpty()) "0." else "."
        updatePreview()
    }

    fun onOperator(op: String) {
        if (isError) {
            expression = ""
            isError = false
        }
        val opChar = op.first()
        if (expression.isEmpty()) {
            if (opChar == '−') {
                expression = "−"
            }
            return
        }
        val last = expression.last()
        if (last in operators) {
            expression = expression.dropLast(1) + opChar
        } else if (last == '(' && opChar == '−') {
            expression += opChar
        } else if (last != '(' && last != '.') {
            expression += opChar
        }
        updatePreview()
    }

    fun onFunction(funcName: String) {
        if (isError) {
            expression = ""
            isError = false
        }
        val token = when (funcName) {
            "sqrt" -> "√("
            "cbrt" -> "∛("
            else -> "$funcName("
        }
        expression += token
        updatePreview()
    }

    fun onConstant(constantSymbol: String) {
        if (isError) {
            expression = ""
            isError = false
        }
        expression += constantSymbol
        updatePreview()
    }

    fun onPower() {
        onOperator("^")
    }

    fun onSquare() {
        if (isError || expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
            expression += "^2"
            updatePreview()
        }
    }

    fun onFactorial() {
        if (isError || expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
            expression += "!"
            updatePreview()
        }
    }

    fun onReciprocal() {
        if (isError) {
            expression = "1÷("
            isError = false
            return
        }
        if (expression.isEmpty()) {
            expression = "1÷("
        } else {
            expression = "1÷($expression)"
        }
        updatePreview()
    }

    fun onPercent() {
        if (isError || expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == ')' || last == 'π' || last == 'e') {
            expression += "%"
            updatePreview()
        }
    }

    fun onParentheses() {
        if (isError) {
            expression = "("
            isError = false
            return
        }
        val openCount = expression.count { it == '(' }
        val closeCount = expression.count { it == ')' }
        val last = expression.lastOrNull()

        if (last == null || last in operators || last == '(') {
            expression += "("
        } else if (openCount > closeCount && (last.isDigit() || last == ')' || last == '%' || last == '!' || last == 'π' || last == 'e')) {
            expression += ")"
        } else {
            expression += "×("
        }
        updatePreview()
    }

    fun onToggleSign() {
        if (isError) {
            expression = ""
            isError = false
            return
        }
        if (expression.isEmpty()) {
            expression = "−"
            return
        }
        if (expression.startsWith("−") && expression.drop(1).all { it.isDigit() || it == '.' }) {
            expression = expression.drop(1)
        } else if (expression.all { it.isDigit() || it == '.' }) {
            expression = "−$expression"
        } else {
            expression = "−($expression)"
        }
        updatePreview()
    }

    fun onBackspace() {
        if (isError) {
            onClear()
            return
        }
        if (expression.isNotEmpty()) {
            val matchedPrefix = functionPrefixes.firstOrNull { expression.endsWith(it) }
            expression = if (matchedPrefix != null) {
                expression.dropLast(matchedPrefix.length)
            } else {
                expression.dropLast(1)
            }
            updatePreview()
        }
    }

    fun onClear() {
        expression = ""
        preview = ""
        isError = false
    }

    fun onEquals() {
        if (expression.isEmpty() || isError) return
        when (val res = CalculatorEngine.evaluate(expression, isDegrees = isDegreeMode)) {
            is CalculatorEngine.EvalResult.Success -> {
                val formatted = res.formatted
                if (formatted != expression) {
                    history.add(0, HistoryItem(expression = expression, result = formatted))
                }
                expression = formatted
                preview = ""
                isError = false
            }
            is CalculatorEngine.EvalResult.Error -> {
                preview = res.message
                isError = true
            }
        }
    }

    fun onHistoryItemSelected(item: HistoryItem) {
        expression = item.result
        preview = ""
        isError = false
    }

    fun clearHistory() {
        history.clear()
    }

    private fun updatePreview() {
        if (expression.isEmpty()) {
            preview = ""
            isError = false
            return
        }
        val hasComplexOrOperator = expression.any {
            it in operators || it == '(' || it == ')' || it == '%' || it == '!' ||
                    it == '√' || it == '∛' || it == 'π' || it == 'e' || it.isLetter()
        }
        if (!hasComplexOrOperator) {
            preview = ""
            isError = false
            return
        }
        when (val res = CalculatorEngine.evaluate(expression, isDegrees = isDegreeMode)) {
            is CalculatorEngine.EvalResult.Success -> {
                preview = if (res.formatted != expression) res.formatted else ""
                isError = false
            }
            is CalculatorEngine.EvalResult.Error -> {
                preview = ""
                isError = false
            }
        }
    }

    private fun getLastNumberSegment(expr: String): String {
        val sb = StringBuilder()
        for (i in expr.length - 1 downTo 0) {
            val c = expr[i]
            if (c.isDigit() || c == '.') {
                sb.append(c)
            } else {
                break
            }
        }
        return sb.reverse().toString()
    }
}
