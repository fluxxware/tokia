package io.tokia.cmp.gradle

import io.tokia.cmp.gradle.core.config.AdaptiveStrategy
import io.tokia.cmp.gradle.core.config.TokiaConfig
import io.tokia.cmp.gradle.core.emit.AdaptiveMember
import io.tokia.cmp.gradle.core.emit.AdaptiveWriter
import io.tokia.cmp.gradle.core.emit.TokenClass
import io.tokia.cmp.gradle.core.emit.TokenWriter
import io.tokia.cmp.gradle.core.media.BreakpointReader
import io.tokia.cmp.gradle.core.media.ResolvedTier
import io.tokia.cmp.gradle.guard.StrayDimensionGuard
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Covers the `tokia { }` options and the fixed generated-code naming. */
class OptionsTest {

    private fun temp(name: String, content: String): File =
        File.createTempFile(name, ".tmp").apply { writeText(content); deleteOnExit() }

    @Test
    fun fixedGeneratedNames() {
        val cfg = TokiaConfig("a.b", "a.b.tokens", "a.b")
        assertEquals("GeneratedThemeTokens", cfg.classNameFor("variables.scss"))
        assertEquals("a.b", cfg.packageFor("variables.scss"))
        assertEquals("ButtonTokens", cfg.classNameFor("Button.scss"))
        assertEquals("a.b.tokens", cfg.packageFor("Button.scss"))
    }

    @Test
    fun customGlobalScssFileNames() {
        val cfg = TokiaConfig("a", "a.t", "a", globalScssFileNames = setOf("theme.scss"))
        assertTrue(cfg.isGlobal("theme.scss"))
        assertTrue(!cfg.isGlobal("variables.scss"))
    }

    @Test
    fun customBreakpointPrefix() {
        val f = temp("brk", "\$screen-sm: 600px;\n\$screen-lg: 1200px;\n\$other: 9px;\n")
        val bp = BreakpointReader().read(f, "screen-")
        assertEquals(listOf(600, 1200), bp.values)
        assertEquals(setOf("screen-sm", "screen-lg"), bp.substitutions.keys)
    }

    @Test
    fun fixedAdaptiveRuntimeNames() {
        val tiers = listOf(ResolvedTier("Regular", null, null, mapOf("x" to "1.dp")))
        val members = listOf(AdaptiveMember("C", "X", "Dp", mapOf("Regular" to "1.dp")))
        val code = AdaptiveWriter.generate("p", tiers, members)
        assertTrue(code.contains("internal data class TokiaTier("), code)
        assertTrue(code.contains("internal val LocalTokiaAdaptive"), code)
        assertTrue(code.contains("internal fun selectTokiaTier"), code)
        assertTrue(code.contains("fun TokiaAdaptiveScope("), code)
    }

    private fun adaptiveTokenClass(strategy: AdaptiveStrategy): TokenClass = TokenClass(
        packageName = "p",
        className = "CompTokens",
        colors = emptyList(),
        sizes = listOf("    val Width = 320.dp"),
        numbers = emptyList(),
        adaptive = setOf("Width"),
        adaptivePackage = "p",
        adaptiveStrategy = strategy,
        adaptiveMembers = listOf(
            AdaptiveMember("CompTokens", "Width", "Dp", mapOf("Regular" to "320.dp", "Tier1" to "560.dp")),
        ),
        tiers = listOf(
            ResolvedTier("Regular", null, 599, mapOf("Width" to "320.dp")),
            ResolvedTier("Tier1", 600, null, mapOf("Width" to "560.dp")),
        ),
    )

    @Test
    fun strategyCompositionLocal() {
        val code = TokenWriter.generate(adaptiveTokenClass(AdaptiveStrategy.COMPOSITION_LOCAL))
        assertTrue(code.contains("LocalTokiaAdaptive.current.CompTokens_Width"), code)
        assertFalse(code.contains("LocalWindowInfo"), code)
    }

    @Test
    fun strategyWindowInfo() {
        val code = TokenWriter.generate(adaptiveTokenClass(AdaptiveStrategy.WINDOW_INFO))
        assertTrue(code.contains("LocalWindowInfo.current.containerSize"), code)
        assertTrue(code.contains("width <= 599.dp -> 320.dp"), code)
        assertTrue(code.contains("else -> 560.dp"), code)
        assertFalse(code.contains("LocalTokiaAdaptive"), code)
    }

    @Test
    fun strategyWindowSizeClass() {
        val code = TokenWriter.generate(adaptiveTokenClass(AdaptiveStrategy.WINDOW_SIZE_CLASS))
        assertTrue(code.contains("currentWindowAdaptiveInfoV2().windowSizeClass.windowWidthSizeClass"), code)
        assertTrue(code.contains("WindowWidthSizeClass.COMPACT -> 320.dp"), code)
        assertTrue(code.contains("WindowWidthSizeClass.EXPANDED -> 560.dp"), code)
        assertFalse(code.contains("LocalTokiaAdaptive"), code)
    }

    @Test
    fun guardExcludesMatchingPaths() {
        val f = temp("ProfileTableState", "val x = 24.dp\n")
        assertTrue(StrayDimensionGuard.check(listOf(f), setOf("0.dp", "1.dp"), listOf("ProfileTableState")).isEmpty())
        assertTrue(StrayDimensionGuard.check(listOf(f), setOf("0.dp", "1.dp"), emptyList()).isNotEmpty())
    }
}
