package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorViewModelTest {

    @Test
    fun testTypingAndLivePreview() {
        val vm = CalculatorViewModel()
        vm.onDigit("2")
        vm.onOperator("+")
        vm.onDigit("3")
        vm.onOperator("×")
        vm.onDigit("4")
        assertEquals("2+3×4", vm.expression)
        assertEquals("14", vm.preview)
        assertFalse(vm.isError)

        vm.onEquals()
        assertEquals("14", vm.expression)
        assertEquals("", vm.preview)
        assertEquals(1, vm.history.size)
        assertEquals("2+3×4", vm.history[0].expression)
        assertEquals("14", vm.history[0].result)
    }

    @Test
    fun testOperatorReplacementAndDecimalRules() {
        val vm = CalculatorViewModel()
        vm.onDecimalPoint()
        assertEquals("0.", vm.expression)
        vm.onDigit("5")
        vm.onDecimalPoint()
        assertEquals("0.5", vm.expression)

        vm.onOperator("+")
        vm.onOperator("×")
        assertEquals("0.5×", vm.expression)
    }

    @Test
    fun testScientificModeLanguageAndSettingsState() {
        val vm = CalculatorViewModel()
        assertFalse(vm.isScientificMode)
        vm.toggleScientificMode()
        assertTrue(vm.isScientificMode)

        assertEquals(AppLanguage.RU, vm.language)
        vm.updateLanguage(AppLanguage.EN)
        assertEquals(AppLanguage.EN, vm.language)

        vm.updateThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, vm.themeMode)

        vm.updateThemePalette(ThemePalette.EMERALD)
        assertEquals(ThemePalette.EMERALD, vm.themePalette)

        vm.updateButtonShapeMode(ButtonShapeMode.ROUND)
        assertEquals(ButtonShapeMode.ROUND, vm.buttonShapeMode)

        vm.onFunction("sin")
        vm.onDigit("3")
        vm.onDigit("0")
        assertEquals("sin(30", vm.expression)
        assertEquals("0.5", vm.preview)

        vm.onBackspace()
        vm.onBackspace()
        vm.onBackspace()
        assertEquals("", vm.expression)
    }

    @Test
    fun testDiagnosticsAndLoadTestExpression() {
        val vm = CalculatorViewModel()
        vm.runDiagnostics()
        assertTrue(vm.testResults.all { it.passed })

        val sampleCase = vm.testResults.first { it.testCase.id == 6 }.testCase
        vm.loadTestExpression(sampleCase)
        assertEquals("200+10%", vm.expression)
        assertEquals("220", vm.preview)
    }
}
