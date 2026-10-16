package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

    private fun evalFormatted(expr: String, isDegrees: Boolean = true): String {
        val res = CalculatorEngine.evaluate(expr, isDegrees = isDegrees)
        assertTrue("Expected success for $expr but got $res", res is CalculatorEngine.EvalResult.Success)
        return (res as CalculatorEngine.EvalResult.Success).formatted
    }

    @Test
    fun testBasicArithmetic() {
        assertEquals("5", evalFormatted("2+3"))
        assertEquals("400", evalFormatted("125+375−100"))
        assertEquals("14", evalFormatted("2+3×4"))
        assertEquals("20", evalFormatted("(2+3)×4"))
        assertEquals("2.5", evalFormatted("5÷2"))
    }

    @Test
    fun testFloatingPointPrecision() {
        assertEquals("0.3", evalFormatted("0.1+0.2"))
        assertEquals("1", evalFormatted("0.1×10"))
        assertEquals("0", evalFormatted("0.3−0.1−0.2"))
    }

    @Test
    fun testSmartPercentages() {
        assertEquals("220", evalFormatted("200+10%"))
        assertEquals("170", evalFormatted("200−15%"))
        assertEquals("20", evalFormatted("200×10%"))
        assertEquals("0.5", evalFormatted("50%"))
    }

    @Test
    fun testImplicitMultiplicationAndParentheses() {
        assertEquals("14", evalFormatted("2(3+4)"))
        assertEquals("45", evalFormatted("(2+3)(4+5)"))
        assertEquals("14", evalFormatted("2×(3+4"))
    }

    @Test
    fun testUnaryMinus() {
        assertEquals("-2", evalFormatted("−5+3"))
        assertEquals("-10", evalFormatted("5×(−2)"))
        assertEquals("0", evalFormatted("−0"))
    }

    @Test
    fun testDivisionByZero() {
        val res = CalculatorEngine.evaluate("10÷0")
        assertTrue(res is CalculatorEngine.EvalResult.Error)
        assertEquals("Деление на ноль", (res as CalculatorEngine.EvalResult.Error).message)
    }

    @Test
    fun testScientificFunctionsAndConstants() {
        assertEquals("41", evalFormatted("2^5+√(81)"))
        assertEquals("2", evalFormatted("sin(30)+cos(60)+tan(45)", isDegrees = true))
        assertEquals("30", evalFormatted("asin(0.5)", isDegrees = true))
        assertEquals("45", evalFormatted("atan(1)", isDegrees = true))
        assertEquals("120", evalFormatted("5!"))
        assertEquals("1", evalFormatted("0!"))
        assertEquals("2", evalFormatted("log(100)"))
        assertEquals("1", evalFormatted("ln(e)"))
        assertEquals("1", evalFormatted("cos(π)", isDegrees = false).let { if (it == "-1") "1" else it })
        assertEquals("-1", evalFormatted("cos(π)", isDegrees = false))
    }

    @Test
    fun testBuiltInSelfTestSuite() {
        val results = CalculatorEngine.runSelfTests()
        assertEquals(15, results.size)
        for (r in results) {
            assertTrue("Self-test failed: ${r.testCase.title} (actual=${r.actual})", r.passed)
        }
    }
}
