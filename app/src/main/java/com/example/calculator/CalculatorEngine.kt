package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorEngine {

    private val mc = MathContext(16, RoundingMode.HALF_UP)
    private val piValue = BigDecimal(Math.PI, mc)
    private val eValue = BigDecimal(Math.E, mc)

    sealed class EvalResult {
        data class Success(val value: BigDecimal, val formatted: String) : EvalResult()
        data class Error(val message: String) : EvalResult()
    }

    data class SelfTestCase(
        val id: Int,
        val title: String,
        val expression: String,
        val expected: String,
        val expectError: Boolean = false,
        val isDegrees: Boolean = true
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
        SelfTestCase(12, "Защита от деления на ноль", "10÷0", "Деление на ноль", expectError = true),
        SelfTestCase(13, "Степень и квадратный корень", "2^5+√(81)", "41"),
        SelfTestCase(14, "Тригонометрия в градусах", "sin(30)+cos(60)+tan(45)", "2"),
        SelfTestCase(15, "Факториал и логарифмы", "5!+log(100)+ln(e)", "123")
    )

    fun runSelfTests(suite: List<SelfTestCase> = defaultTestSuite): List<SelfTestResult> {
        return suite.map { tc ->
            when (val res = evaluate(tc.expression, isDegrees = tc.isDegrees)) {
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

    fun evaluate(rawExpression: String, isDegrees: Boolean = true): EvalResult {
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
            val result = evalRpn(rpn, isDegrees)
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
        data class FunctionToken(val name: String) : Token()
        object LeftParen : Token()
        object RightParen : Token()
        object Percent : Token()
        object Factorial : Token()
    }

    private val supportedFunctions = setOf(
        "sin", "cos", "tan",
        "asin", "acos", "atan",
        "ln", "log", "sqrt", "cbrt", "abs"
    )

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
                c == 'π' -> rawTokens.add(Token.NumberToken(piValue))
                c == '√' -> rawTokens.add(Token.FunctionToken("sqrt"))
                c == '∛' -> rawTokens.add(Token.FunctionToken("cbrt"))
                c.isLetter() -> {
                    val sb = StringBuilder()
                    while (i < expr.length && expr[i].isLetter()) {
                        sb.append(expr[i].lowercaseChar())
                        i++
                    }
                    when (val ident = sb.toString()) {
                        "pi" -> rawTokens.add(Token.NumberToken(piValue))
                        "e" -> rawTokens.add(Token.NumberToken(eValue))
                        in supportedFunctions -> rawTokens.add(Token.FunctionToken(ident))
                        else -> throw IllegalArgumentException("Неизвестная функция: $ident")
                    }
                    continue
                }
                c == '(' -> rawTokens.add(Token.LeftParen)
                c == ')' -> rawTokens.add(Token.RightParen)
                c == '%' -> rawTokens.add(Token.Percent)
                c == '!' -> rawTokens.add(Token.Factorial)
                c in listOf('+', '-', '*', '/', '^') -> {
                    val isUnary = rawTokens.isEmpty() ||
                            rawTokens.last() is Token.LeftParen ||
                            rawTokens.last() is Token.OperatorToken ||
                            rawTokens.last() is Token.FunctionToken
                    if (isUnary && (c == '-' || c == '+')) {
                        if (c == '-') {
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

        val result = mutableListOf<Token>()
        for (idx in rawTokens.indices) {
            val curr = rawTokens[idx]
            if (idx > 0) {
                val prev = rawTokens[idx - 1]
                val prevIsValue = prev is Token.NumberToken ||
                        prev is Token.RightParen ||
                        prev is Token.Percent ||
                        prev is Token.Factorial
                val currStartsValue = curr is Token.LeftParen ||
                        curr is Token.NumberToken ||
                        curr is Token.FunctionToken
                if (prevIsValue && currStartsValue) {
                    result.add(Token.OperatorToken('*'))
                }
            }
            result.add(curr)
        }
        return result
    }

    private fun precedence(token: Token): Int = when (token) {
        is Token.OperatorToken -> when (token.op) {
            '+', '-' -> 1
            '*', '/' -> 2
            '~' -> 3
            '^' -> 4
            else -> 0
        }
        is Token.FunctionToken -> 5
        else -> 0
    }

    private fun isRightAssociative(token: Token): Boolean = when (token) {
        is Token.OperatorToken -> token.op == '~' || token.op == '^'
        is Token.FunctionToken -> true
        else -> false
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.NumberToken -> output.add(token)
                is Token.Percent -> output.add(token)
                is Token.Factorial -> output.add(token)
                is Token.FunctionToken -> stack.addLast(token)
                is Token.OperatorToken -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.OperatorToken || top is Token.FunctionToken) {
                            if (token.op == '~' && (top is Token.FunctionToken || (top is Token.OperatorToken && top.op == '^'))) {
                                break
                            }
                            val p1 = precedence(token)
                            val p2 = precedence(top)
                            if (p2 > p1 || (p1 == p2 && !isRightAssociative(token))) {
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
                    if (stack.isNotEmpty() && stack.last() is Token.FunctionToken) {
                        output.add(stack.removeLast())
                    }
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

    private fun evalRpn(rpn: List<Token>, isDegrees: Boolean): BigDecimal {
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
                is Token.Factorial -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Ошибка факториала")
                    val item = stack.removeLast()
                    val factVal = computeFactorial(item.value)
                    stack.addLast(StackItem(factVal))
                }
                is Token.FunctionToken -> {
                    if (stack.isEmpty()) throw IllegalArgumentException("Неполное выражение")
                    val arg = stack.removeLast().value
                    val res = evalFunction(token.name, arg, isDegrees)
                    stack.addLast(StackItem(res))
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
                            '^' -> computePower(a.value, b.value)
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

    private fun computeFactorial(value: BigDecimal): BigDecimal {
        val stripped = value.stripTrailingZeros()
        if (stripped.scale() > 0 || stripped < BigDecimal.ZERO) {
            throw ArithmeticException("Факториал только для целых >= 0")
        }
        val n = stripped.toInt()
        if (n > 170) {
            throw ArithmeticException("Переполнение числа")
        }
        var acc = BigDecimal.ONE
        for (i in 2..n) {
            acc = acc.multiply(BigDecimal.valueOf(i.toLong()), mc)
        }
        return acc
    }

    private fun computePower(base: BigDecimal, exponent: BigDecimal): BigDecimal {
        val expStripped = exponent.stripTrailingZeros()
        if (expStripped.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ONE
        }
        if (expStripped.scale() <= 0 && abs(expStripped.toInt()) <= 1000) {
            val n = expStripped.toInt()
            if (n < 0 && base.compareTo(BigDecimal.ZERO) == 0) {
                throw ArithmeticException("Деление на ноль")
            }
            return base.pow(n, mc)
        }
        val bDouble = base.toDouble()
        val eDouble = exponent.toDouble()
        if (bDouble < 0.0) {
            throw ArithmeticException("Отрицательное основание дробной степени")
        }
        val p = bDouble.pow(eDouble)
        if (p.isNaN() || p.isInfinite()) {
            throw ArithmeticException("Переполнение числа")
        }
        return BigDecimal(p, mc)
    }

    private fun evalFunction(name: String, arg: BigDecimal, isDegrees: Boolean): BigDecimal {
        val x = arg.toDouble()
        val resultDouble: Double = when (name) {
            "sin" -> {
                if (isDegrees) {
                    val mod = ((x % 360.0) + 360.0) % 360.0
                    when (mod) {
                        0.0, 180.0 -> 0.0
                        30.0, 150.0 -> 0.5
                        210.0, 330.0 -> -0.5
                        90.0 -> 1.0
                        270.0 -> -1.0
                        else -> sin(Math.toRadians(x))
                    }
                } else {
                    sin(x)
                }
            }
            "cos" -> {
                if (isDegrees) {
                    val mod = ((x % 360.0) + 360.0) % 360.0
                    when (mod) {
                        90.0, 270.0 -> 0.0
                        60.0, 300.0 -> 0.5
                        120.0, 240.0 -> -0.5
                        0.0 -> 1.0
                        180.0 -> -1.0
                        else -> cos(Math.toRadians(x))
                    }
                } else {
                    cos(x)
                }
            }
            "tan" -> {
                if (isDegrees) {
                    val mod = ((x % 180.0) + 180.0) % 180.0
                    if (abs(mod - 90.0) < 1e-12) {
                        throw ArithmeticException("Не определено")
                    }
                    when (mod) {
                        0.0 -> 0.0
                        45.0 -> 1.0
                        135.0 -> -1.0
                        else -> tan(Math.toRadians(x))
                    }
                } else {
                    val c = cos(x)
                    if (abs(c) < 1e-15) throw ArithmeticException("Не определено")
                    tan(x)
                }
            }
            "asin" -> {
                if (x < -1.0 || x > 1.0) throw ArithmeticException("Вне области определения")
                val rad = asin(x)
                if (isDegrees) Math.toDegrees(rad) else rad
            }
            "acos" -> {
                if (x < -1.0 || x > 1.0) throw ArithmeticException("Вне области определения")
                val rad = acos(x)
                if (isDegrees) Math.toDegrees(rad) else rad
            }
            "atan" -> {
                val rad = atan(x)
                if (isDegrees) Math.toDegrees(rad) else rad
            }
            "ln" -> {
                if (x <= 0.0) throw ArithmeticException("Логарифм от <= 0")
                ln(x)
            }
            "log" -> {
                if (x <= 0.0) throw ArithmeticException("Логарифм от <= 0")
                log10(x)
            }
            "sqrt" -> {
                if (arg < BigDecimal.ZERO) throw ArithmeticException("Корень из отрицательного числа")
                sqrt(x)
            }
            "cbrt" -> Math.cbrt(x)
            "abs" -> return arg.abs()
            else -> throw IllegalArgumentException("Неизвестная функция: $name")
        }

        if (resultDouble.isNaN() || resultDouble.isInfinite()) {
            throw ArithmeticException("Не определено")
        }
        if (abs(resultDouble) < 1e-14) {
            return BigDecimal.ZERO
        }
        return BigDecimal(resultDouble, mc)
    }
}
