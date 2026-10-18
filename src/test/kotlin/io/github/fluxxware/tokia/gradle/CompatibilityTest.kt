package io.github.fluxxware.tokia.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * In-JVM behavior tests: applying the plugin, required-option validation, and the loud failures.
 *
 * The cross-version matrix (Gradle × Kotlin × JVM) lives in the Nix harness under `tests/`, not here
 * — TestKit can only exercise the JVM it runs on.
 *
 * The source-set auto-wire uses the Kotlin Multiplatform plugin, which is NOT on the TestKit
 * classpath (Tokia no longer depends on KGP). Test projects therefore disable auto-wiring; the
 * success path is covered by the real consumers and the `tests/` harness.
 */
class CompatibilityTest {

    private val noAutoWire = """
        import io.github.fluxxware.tokia.gradle.TokiaExtension

        plugins { id("io.github.fluxxware.tokia") }

        extensions.configure(TokiaExtension::class) {
            autoWireGeneratedSources.set(false)
        }
    """.trimIndent() + "\n"

    private val configuredNoAutoWire = """
        import io.github.fluxxware.tokia.gradle.TokiaExtension

        plugins { id("io.github.fluxxware.tokia") }

        extensions.configure(TokiaExtension::class) {
            autoWireGeneratedSources.set(false)
            scssRootDir.set(layout.projectDirectory.dir("src"))
            themePackage.set("com.acme.styles")
            tokensPackage.set("com.acme.styles.tokens")
            adaptivePackage.set("com.acme.styles")
        }
    """.trimIndent() + "\n"

    private val kmpMissing = """
        import io.github.fluxxware.tokia.gradle.TokiaExtension

        plugins { id("io.github.fluxxware.tokia") }

        extensions.configure(TokiaExtension::class) {
            scssRootDir.set(layout.projectDirectory.dir("src"))
            themePackage.set("com.acme.styles")
            tokensPackage.set("com.acme.styles.tokens")
            adaptivePackage.set("com.acme.styles")
        }
    """.trimIndent() + "\n"

    private fun project(buildScript: String): Path = createTempDirectory("tokia-compat").also { dir ->
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"tokia-consumer\"\n")
        dir.resolve("build.gradle.kts").writeText(buildScript)
    }

    private fun runner(dir: Path) = GradleRunner.create()
        .withProjectDir(dir.toFile())
        .withPluginClasspath()

    @Test
    fun appliesAndRegistersTasks() {
        val result = runner(project(noAutoWire)).withArguments("tasks", "--all", "--console=plain").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue(result.output.contains("generateScssTokens"), result.output)
        assertTrue(result.output.contains("checkStrayDimensions"), result.output)
    }

    @Test
    fun passesWhenMandatoryOptionsSet() {
        val result = runner(project(configuredNoAutoWire))
            .withArguments("validateTokiaOptions", "--console=plain").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateTokiaOptions")?.outcome)
    }

    @Test
    fun failsWhenMandatoryOptionsMissing() {
        val result = runner(project(noAutoWire))
            .withArguments("validateTokiaOptions", "--console=plain").buildAndFail()
        assertTrue(result.output.contains("missing required tokiA"), result.output)
    }

    @Test
    fun failsLoudlyWhenKmpNotApplied() {
        val result = runner(project(kmpMissing))
            .withArguments("tasks", "--console=plain").buildAndFail()
        assertTrue(result.output.contains("requires the Kotlin Multiplatform plugin"), result.output)
    }
}
