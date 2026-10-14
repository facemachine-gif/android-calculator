package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object CalculatorEngine {

    private val mc = MathContext(16, RoundingMode.HALF_UP)

    sealed class EvalResult {
        data class Success(val value: BigDecimal, val formatted: String) : EvalResult()
        data class Error(val message: String) : EvalResult()
    }

    data class SelfTestCase(
        val id: Int,
        val title: String,
        val expression: String,
        val expected: String,
        val expectError: Boolean = false
    )

    data class SelfTestResult(
        val testCase: SelfTestCase,
        val actual: String,
        val passed: Boolean
    )

    val defaultTestSuite: List<SelfTestCase> = listOf(
        SelfTestCase(1, "Сложение и вычитание", "125+375−100", "400"),
        SelfTestCase(2, "Приоритет операций", "2+3×4", "14"),
        SelfTestCase(3, "Вычисление со скобками", "(2+3)×4", "20"),
        SelfTestCase(4, "Дробное деление", "5÷2", "2.5"),
        SelfTestCase(5, "Точность BigDecimal (0.1 + 0.2)", "0.1+0.2", "0.3"),
        SelfTestCase(6, "Прибавление процента (+10%)", "200+10%", "220"),
        SelfTestCase(7, "Скидка в процентах (−15%)", "200−15%", "170"),
        SelfTestCase(8, "Умножение на процент", "200×10%", "20"),
        SelfTestCase(9, "Перевод процента в долю", "50%", "0.5"),
        SelfTestCase(10, "Неявное умножение и автоскобки", "2(3+4", "14"),
        SelfTestCase(11, "Отрицательные числа (унарный минус)", "5×(−2)", "-10"),
        SelfTestCase(12, "Защита от деления на ноль", "10÷0", "Деление на ноль", expectError = true)
    )

    fun runSelfTests(suite: List<SelfTestCase> = defaultTestSuite): List<SelfTestResult> {
        return suite.map { tc ->
            when (val res = evaluate(tc.expression)) {
                is EvalResult.Success -> {
                    val passed = !tc.expectError && res.formatted == tc.expected
                    SelfTestResult(tc, res.formatted, passed)
                }
                is EvalResult.Error -> {
                    val passed = tc.expectError && res.message == tc.expected
                    SelfTestResult(tc, res.message, passed)
                }
            }
        }
    }

    fun evaluate(rawExpression: String): EvalResult {
        val cleaned = rawExpression
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(",", ".")
            .replace(" ", "")
            .trim()

        if (cleaned.isEmpty()) {
            return EvalResult.Success(BigDecimal.ZERO, "0")
        }

        // Balance unclosed opening parentheses automatically for convenience
        val openCount = cleaned.count { it == '(' }
        val closeCount = cleaned.count { it == ')' }
        val balanced = if (openCount > closeCount) {
            cleaned + ")".repeat(openCount - closeCount)
        } else {
            cleaned
        }

        return try {
            val tokens = tokenize(balanced)
            if (tokens.isEmpty()) {
                return EvalResult.Success(BigDecimal.ZERO, "0")
            }
            val rpn = toRpn(tokens)
            val result = evalRpn(rpn)
            EvalResult.Success(result, formatNumber(result))
        } catch (e: ArithmeticException) {
            EvalResult.Error(e.message ?: "Деление на ноль")
        } catch (e: IllegalArgumentException) {
            EvalResult.Error(e.message ?: "Ошибка выражения")
        } catch (e: Exception) {
            EvalResult.Error("Ошибка выражения")
        }
    }

    fun formatNumber(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val stripped = value.stripTrailingZeros()
        val scale = stripped.scale()
        // Use plain string for typical numbers, scientific notation for very large/small
        val absVal = stripped.abs()
        if (absVal != BigDecimal.ZERO && (absVal >= BigDecimal("1E12") || absVal < BigDecimal("1E-9"))) {
            return stripped.round(MathContext(10, RoundingMode.HALF_UP)).toString()
        }
        val rounded = if (scale > 10) {
            stripped.setScale(10, RoundingMode.HALF_UP).stripTrailingZeros()
        } else {
            stripped
        }
        return if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
    }

    sealed class Token {
        data class NumberToken(val value: BigDecimal) : Token()
        data class OperatorToken(val op: Char) : Token()
        object LeftParen : Token()
        object RightParen : Token()
        object Percent : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val rawTokens = mutableListOf<Token>()
        var i = 0
        while (i < expr.length) {
            val c = expr[i]
            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    var dotCount = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') dotCount++
                        if (dotCount > 1) throw IllegalArgumentException("Некорректное число")
                        sb.append(expr[i])
                        i++
                    }
                    val numStr = sb.toString()
                    if (numStr == ".") throw IllegalArgumentException("Некорректное число")
                    rawTokens.add(Token.NumberToken(BigDecimal(numStr)))
                    continue
                }
                c == '(' -> rawTokens.add(Token.LeftParen)
                c == ')' -> rawTokens.add(Token.RightParen)
                c == '%' -> rawTokens.add(Token.Percent)
                c in listOf('+', '-', '*', '/') -> {
                    val isUnary = rawTokens.isEmpty() ||
                            rawTokens.last() is Token.LeftParen ||
                            rawTokens.last() is Token.OperatorToken
                    if (isUnary && (c == '-' || c == '+')) {
                        if (c == '-') {
                            // Represent unary minus as '~' (high-precedence unary negation)
                            rawTokens.add(Token.OperatorToken('~'))
                        }
                    } else {
                        rawTokens.add(Token.OperatorToken(c))
                    }
                }
                else -> throw IllegalArgumentException("Неизвестный символ: $c")
            }
            i++
        }

        // Insert implicit multiplication: e.g., 2(3) -> 2*(3), (2)(3) -> (2)*(3), 50%2 -> 50%*2
        val result = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            if (idx > 0) {
                val prev = rawTokens[idx - 1]
                val prevIsValue = prev is Token.NumberToken || prev is Token.RightParen || prev is Token.Percent
                val currStartsValue = curr is Token.LeftParen || curr is Token.NumberToken
                if (prevIsValue && currStartsValue) {
                    result.add(Token.OperatorToken('*'))
                }
            }
            result.add(curr)
        }
        return result
    }

    private fun precedence(op: Char): Int = when (op) {
        '+', '-' -> 1
        '*', '/' -> 2
        '~' -> 3
        else -> 0
    }

    private fun isRightAssociative(op: Char): Boolean = (op == '~')

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.NumberToken -> output.add(token)
                is Token.Percent -> output.add(token)
                is Token.OperatorToken -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.OperatorToken) {
                            val p1 = precedence(token.op)
                            val p2 = precedence(top.op)
                            if (p2 > p1 || (p1 == p2 && !isRightAssociative(token.op))) {
                                output.add(stack.removeLast())
                                continue
                            }
                        }
                        break
                    }
                    stack.addLast(token)
                }
                is Token.LeftParen -> stack.addLast(token)
                is Token.RightParen -> {
                    var foundLeft = false
                    while (stack.isNotEmpty()) {
                        val top = stack.removeLast()
                        if (top is Token.LeftParen) {
                            foundLeft = true
                            break
                        }
                        output.add(top)
                    }
                    if (!foundLeft) throw IllegalArgumentException("Непарные скобки")
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LeftParen || top is Token.RightParen) {
                throw IllegalArgumentException("Непарные скобки")
            }
            output.add(top)
        }
        return output
    }

    private fun evalRpn(rpn: List<Token>): BigDecimal {
        data class StackItem(
            val value: BigDecimal,
            val isPercent: Boolean = false,
            val rawPercent: BigDecimal = BigDecimal.ZERO
        )

        val stack = ArrayDeque<StackItem>()
        val hundred = BigDecimal(100)

        for (token in rpn) {
            when (token) {
                is Token.NumberToken -> stack.addLast(StackItem(token.value))
                is Token.Percent -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Ошибка процента")
                    val item = stack.removeLast()
                    val pctVal = item.value.divide(hundred, mc)
                    stack.addLast(StackItem(pctVal, isPercent = true, rawPercent = item.value))
                }
                is Token.OperatorToken -> {
                    if (token.op == '~') {
                        if (stack.isEmpty()) throw IllegalArgumentException("Неполное выражение")
                        val a = stack.removeLast()
                        stack.addLast(StackItem(a.value.negate(), a.isPercent, a.rawPercent.negate()))
                    } else {
                        if (stack.size < 2) throw IllegalArgumentException("Неполное выражение")
                        val b = stack.removeLast()
                        val a = stack.removeLast()
                        val res = when (token.op) {
                            '+' -> {
                                val addend = if (b.isPercent) a.value.multiply(b.value, mc) else b.value
                                a.value.add(addend, mc)
                            }
                            '-' -> {
                                val subtrahend = if (b.isPercent) a.value.multiply(b.value, mc) else b.value
                                a.value.subtract(subtrahend, mc)
                            }
                            '*' -> a.value.multiply(b.value, mc)
                            '/' -> {
                                if (b.value.compareTo(BigDecimal.ZERO) == 0) {
                                    throw ArithmeticException("Деление на ноль")
                                }
                                a.value.divide(b.value, mc)
                            }
                            else -> throw IllegalArgumentException("Неизвестный оператор")
                        }
                        stack.addLast(StackItem(res))
                    }
                }
                else -> throw IllegalArgumentException("Ошибка вычисления")
            }
        }

        if (stack.size != 1) throw IllegalArgumentException("Неполное выражение")
        return stack.last().value
    }
}
