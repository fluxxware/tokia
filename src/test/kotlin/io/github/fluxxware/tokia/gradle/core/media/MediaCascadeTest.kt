package io.github.fluxxware.tokia.gradle.core.media

import io.github.fluxxware.tokia.gradle.core.emit.ValueEmitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaCascadeTest {

    @Test
    fun unconditionalOnly() {
        val decls = MediaScssParser.parse("${'$'}modal-width: 560px;\n${'$'}space-sm: 9px;")
        val tiers = TierResolver.resolve(decls, emptyList())
        assertEquals(1, tiers.size)
        assertEquals("560px", tiers[0].values["modal-width"])
        assertEquals("9px", tiers[0].values["space-sm"])
    }

    @Test
    fun maxWidthOverrideWinsBelowBreakpoint() {
        val decls = MediaScssParser.parse(
            "${'$'}modal-width: 560px;\n@media (max-width: 600px) {\n  ${'$'}modal-width: 320px;\n}",
        )
        val tiers = TierResolver.resolve(decls, listOf(600))
        assertEquals(2, tiers.size)
        assertEquals("320px", tiers[0].values["modal-width"])
        assertEquals("560px", tiers[1].values["modal-width"])
    }

    @Test
    fun minWidthAppliesAbove() {
        val decls = MediaScssParser.parse(
            "${'$'}x: 10px;\n@media (min-width: 840px) {\n  ${'$'}x: 20px;\n}",
        )
        val tiers = TierResolver.resolve(decls, listOf(600, 840))
        assertEquals("10px", tiers[0].values["x"])
        assertEquals("10px", tiers[1].values["x"])
        assertEquals("20px", tiers[2].values["x"])
    }

    @Test
    fun laterRuleWinsOnEqualSpecificity() {
        val decls = MediaScssParser.parse(
            "${'$'}x: 1px;\n@media (max-width: 900px) { ${'$'}x: 2px; }\n@media (max-width: 900px) { ${'$'}x: 3px; }",
        )
        val tiers = TierResolver.resolve(decls, listOf(900))
        assertEquals("3px", tiers[0].values["x"])
    }

    @Test
    fun commaIsOr() {
        val decls = MediaScssParser.parse(
            "${'$'}x: 0px;\n@media (max-width: 400px), (min-width: 1200px) { ${'$'}x: 9px; }",
        )
        val tiers = TierResolver.resolve(decls, listOf(400, 800, 1200))
        assertEquals("9px", tiers[0].values["x"])
        assertEquals("0px", tiers[1].values["x"])
        assertEquals("0px", tiers[2].values["x"])
        assertEquals("9px", tiers[3].values["x"])
    }

    @Test
    fun breakpointsAreReadFromScss() {
        val decls = MediaScssParser.parse(
            "${'$'}bp-compact: 600px;\n${'$'}bp-medium: 840px;\n${'$'}modal-width: 560px;",
        )
        assertEquals(listOf(600, 840), TierResolver.breakpointsFrom(decls))
    }

    @Test
    fun unsupportedFeatureFailsLoudly() {
        val error = runCatching {
            MediaScssParser.parse("@media (orientation: landscape) { ${'$'}x: 1px; }")
        }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException, "expected IllegalArgumentException, got $error")
    }

    @Test
    fun spAndNumbersEmitCorrectTypes() {
        assertEquals("androidx.compose.ui.unit.TextUnit", ValueEmitter.kotlinType("14.sp"))
        assertEquals("androidx.compose.ui.unit.Dp", ValueEmitter.kotlinType("8.dp"))
        assertEquals("Double", ValueEmitter.kotlinType("1.5"))
        assertEquals("Float", ValueEmitter.kotlinType("0.5f"))
        assertEquals("Boolean", ValueEmitter.kotlinType("true"))
    }

    @Test
    fun percentBecomesFraction() {
        assertEquals("0.5f", ValueEmitter.expression("50%"))
        assertEquals("Float", ValueEmitter.kotlinType("0.5f"))
    }

    @Test
    fun breakpointVariableSubstitution() {
        val decls = MediaScssParser.parse(
            "${'$'}x: 0px;\n@media (max-width: ${'$'}bp-compact) { ${'$'}x: 9px; }",
            breakpointSubstitutions = mapOf("bp-compact" to 600),
        )
        val tiers = TierResolver.resolve(decls, listOf(600))
        assertEquals("9px", tiers[0].values["x"])
        assertEquals("0px", tiers[1].values["x"])
    }
}
