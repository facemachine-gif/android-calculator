package com.example.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        vm.toggleLanguage()
        assertEquals(AppLanguage.EN, vm.language)
        vm.toggleLanguage()
        assertEquals(AppLanguage.RU, vm.language)

        assertEquals(ThemeMode.DARK, vm.themeMode)
        vm.toggleThemeMode()
        assertEquals(ThemeMode.LIGHT, vm.themeMode)
        vm.toggleThemeMode()
        assertEquals(ThemeMode.DARK, vm.themeMode)

        assertTrue(vm.autoScientificOnLandscape)
        vm.toggleAutoScientificOnLandscape()
        assertFalse(vm.autoScientificOnLandscape)
        vm.toggleAutoScientificOnLandscape()
        assertTrue(vm.autoScientificOnLandscape)

        // Background enabled toggle and default CROP mode
        assertTrue(vm.isBackgroundEnabled)
        vm.toggleBackgroundEnabled()
        assertFalse(vm.isBackgroundEnabled)
        vm.toggleBackgroundEnabled()
        assertTrue(vm.isBackgroundEnabled)

        assertEquals(0.85f, vm.uiAlpha, 0.001f)
        vm.updateUiAlpha(1.0f)
        assertEquals(1.0f, vm.uiAlpha, 0.001f)

        assertEquals(LandscapeBgMode.CROP, vm.landscapeBgMode)
        vm.toggleLandscapeBgMode()
        assertEquals(LandscapeBgMode.TRIPLE, vm.landscapeBgMode)
        vm.toggleLandscapeBgMode()
        assertEquals(LandscapeBgMode.BLUR_WINGS, vm.landscapeBgMode)
        vm.toggleLandscapeBgMode()
        assertEquals(LandscapeBgMode.CROP, vm.landscapeBgMode)

        vm.onCustomBackgroundSaved()
        assertTrue(vm.hasCustomBackground)
        assertTrue(vm.isBackgroundEnabled)
        vm.onCustomBackgroundCleared()
        assertFalse(vm.hasCustomBackground)

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
    fun testOrientationAutoScientificAndPersistenceRestore() {
        val vm = CalculatorViewModel()
        vm.loadSavedSettings(
            savedThemeMode = ThemeMode.DARK,
            savedPalette = ThemePalette.AMETHYST,
            savedShape = ButtonShapeMode.ROUND,
            savedLang = AppLanguage.EN,
            savedDegreeMode = true,
            savedAutoSciOnLandscape = true,
            savedUiAlpha = 1.0f,
            savedLandscapeBgMode = LandscapeBgMode.CROP,
            savedHasCustomBg = false,
            savedBgEnabled = true
        )
        assertEquals(ThemeMode.DARK, vm.themeMode)
        assertEquals(ThemePalette.AMETHYST, vm.themePalette)
        assertEquals(ButtonShapeMode.ROUND, vm.buttonShapeMode)
        assertEquals(AppLanguage.EN, vm.language)
        assertTrue(vm.autoScientificOnLandscape)
        assertEquals(1.0f, vm.uiAlpha, 0.001f)
        assertEquals(LandscapeBgMode.CROP, vm.landscapeBgMode)
        assertFalse(vm.hasCustomBackground)
        assertTrue(vm.isBackgroundEnabled)

        vm.onOrientationChanged(isLandscape = false)
        assertFalse(vm.isScientificMode)

        vm.onOrientationChanged(isLandscape = true)
        assertTrue(vm.isScientificMode)

        vm.onOrientationChanged(isLandscape = false)
        assertFalse(vm.isScientificMode)

        vm.updateAutoScientificOnLandscape(false)
        vm.onOrientationChanged(isLandscape = true)
        assertFalse(vm.isScientificMode)
    }

    @Test
    fun testCustomizationSwapDeleteAndDynamicBounds() {
        val vm = CalculatorViewModel()
        vm.toggleCustomizationMode()
        assertTrue(vm.isCustomizationMode)

        vm.onTopBarChipTappedInCustomMode(0)
        vm.onTopBarChipTappedInCustomMode(2)
        assertEquals(listOf(TopBarItem.SETTINGS, TopBarItem.MODE, TopBarItem.HISTORY), vm.topBarOrder)

        val firstId = vm.gridLayout[6]
        val secondId = vm.gridLayout[7]
        vm.onGridCellTappedInCustomMode(6)
        vm.onGridCellTappedInCustomMode(7)
        assertEquals(secondId, vm.gridLayout[6])
        assertEquals(firstId, vm.gridLayout[7])

        for (c in 0 until 5) {
            vm.deleteGridButtonAt(c)
            assertNull(vm.gridLayout[c])
        }
        assertEquals(Pair(1, 0), vm.computeActiveGridBounds(forScientificMode = true))

        for (r in 1 until 6) {
            vm.deleteGridButtonAt(r * 5)
        }
        assertEquals(Pair(1, 1), vm.computeActiveGridBounds(forScientificMode = true))

        val serializedTop = vm.serializeTopBarOrder()
        val serializedGrid = vm.serializeGridLayout()
        assertEquals(vm.topBarOrder, vm.parseTopBarOrder(serializedTop))
        assertEquals(vm.gridLayout, vm.parseGridLayout(serializedGrid))

        vm.resetCustomizations()
        assertEquals(CalculatorViewModel.DEFAULT_TOP_BAR, vm.topBarOrder)
        assertEquals(CalculatorViewModel.DEFAULT_GRID_IDS, vm.gridLayout)
        assertEquals(Pair(0, 0), vm.computeActiveGridBounds(forScientificMode = true))
        assertEquals(Pair(1, 1), vm.computeActiveGridBounds(forScientificMode = false))
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
