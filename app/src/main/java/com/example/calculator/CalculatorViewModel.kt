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

class CalculatorViewModel : ViewModel() {

    var expression by mutableStateOf("")
        private set

    var preview by mutableStateOf("")
        private set

    var isError by mutableStateOf(false)
        private set

    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    var testResults by mutableStateOf(CalculatorEngine.runSelfTests())
        private set

    val history = mutableStateListOf<HistoryItem>()

    private val operators = setOf('+', '−', '×', '÷')

    fun runDiagnostics() {
        testResults = CalculatorEngine.runSelfTests()
    }

    fun loadTestExpression(testCase: CalculatorEngine.SelfTestCase) {
        expression = testCase.expression
        isError = false
        when (val res = CalculatorEngine.evaluate(expression)) {
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

    fun onPercent() {
        if (isError || expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last == ')') {
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
        } else if (openCount > closeCount && (last.isDigit() || last == ')' || last == '%')) {
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
            expression = expression.dropLast(1)
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
        when (val res = CalculatorEngine.evaluate(expression)) {
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
        val hasOperatorOrParen = expression.any { it in operators || it == '(' || it == ')' || it == '%' }
        if (!hasOperatorOrParen) {
            preview = ""
            isError = false
            return
        }
        when (val res = CalculatorEngine.evaluate(expression)) {
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
