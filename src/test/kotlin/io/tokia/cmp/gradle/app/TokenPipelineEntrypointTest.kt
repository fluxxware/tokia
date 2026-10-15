package io.tokia.cmp.gradle.app

import io.tokia.cmp.gradle.core.config.TokiaConfig
import io.tokia.cmp.gradle.core.emit.AdaptiveDetector
import io.tokia.cmp.gradle.core.media.BreakpointReader
import io.tokia.cmp.gradle.core.parse.ColorsFile
import io.tokia.cmp.gradle.core.sass.SassCompiler
import io.tokia.cmp.gradle.core.sass.ScssTokenCompiler
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TokenPipelineEntrypointTest {

    /** Fake sass: rules -> empty; export file -> a deterministic `--t` export we can assert on. */
    private val fakeSass = SassCompiler { _, target ->
        target.parentFile.mkdirs()
        if (target.name.endsWith("-rules.css")) target.writeText("")
        else target.writeText(":root {\n  --tFixtureWidth: 100px;\n}")
    }

    private fun config() = TokiaConfig(
        themePackage = "io.tokia.cmp.styles",
        tokensPackage = "io.tokia.cmp.styles.tokens",
        adaptivePackage = "io.tokia.cmp.styles",
    )

    private fun pipeline(root: File): TokenPipelineEntrypoint {
        val cfg = config()
        val colors = ColorsFile(File(root, "styles/_colors.scss"))
        val compiler = ScssTokenCompiler(fakeSass, File(root, "work"), colors, cfg)
        return TokenPipelineEntrypoint(cfg, BreakpointReader(), compiler, AdaptiveDetector())
    }

    private fun tempDir(): File = createTempDir(prefix = "uikit-pipeline").also { it.deleteOnExit() }

    @Test
    fun emitsAdaptiveGetterAndRuntime() {
        val root = tempDir()
        val scss = File(root, "UiKitFixture.scss").apply {
            writeText(
                "${'$'}fixture-width: 100px;\n" +
                    "@media (max-width: 600px) { ${'$'}fixture-width: 50px; }\n",
            )
        }
        val bp = File(root, "_breakpoints.scss").apply { writeText("${'$'}bp-compact: 600px;\n") }

        val result = pipeline(root).run(listOf(scss), bp)

        val tokenFile = result.tokenFiles.single()
        assertEquals("io/tokia/cmp/styles/tokens/UiKitFixtureTokens.kt", tokenFile.relativePath)
        assertTrue(
            tokenFile.content.contains("val FixtureWidth: Dp @Composable @ReadOnlyComposable"),
            tokenFile.content,
        )
        assertTrue(tokenFile.content.contains("LocalTokiaAdaptive.current.UiKitFixtureTokens_FixtureWidth"))

        val adaptive = assertNotNull(result.adaptiveFile)
        assertEquals("io/tokia/cmp/styles/TokiaAdaptive.kt", adaptive.relativePath)
        assertTrue(adaptive.content.contains("internal val LocalTokiaAdaptive"))
        assertTrue(adaptive.content.contains("fun TokiaAdaptiveScope"))
        assertTrue(adaptive.content.contains("UiKitFixtureTokens_FixtureWidth = 50.dp"))
        assertTrue(adaptive.content.contains("UiKitFixtureTokens_FixtureWidth = 100.dp"))
    }

    @Test
    fun noAdaptiveFileWhenNoMediaQueries() {
        val root = tempDir()
        val scss = File(root, "UiKitPlain.scss").apply { writeText("${'$'}fixture-width: 100px;\n") }
        val result = pipeline(root).run(listOf(scss), null)
        assertEquals(null, result.adaptiveFile)
        assertTrue(result.tokenFiles.single().content.contains("val FixtureWidth = 100.dp"))
    }
}
