package io.tokia.cmp.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Cross-version compatibility matrix.
 *
 * The plugin is compiled against the oldest supported Gradle (the wrapper version), so newer Gradle
 * must run it. These tests prove the plugin *applies* and registers its tasks under each version.
 *
 * Opt-in by version list (avoids downloading distributions in the default build):
 *   ./gradlew test -DtokiaCompatGradleVersions=8.10,9.6
 * With no property set, only the current wrapper Gradle is exercised.
 */
class CompatibilityTest {

    private fun project(): Path = createTempDirectory("tokia-compat").also { dir ->
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"tokia-consumer\"\n")
        dir.resolve("build.gradle.kts").writeText("plugins { id(\"io.tokia.cmp\") }\n")
    }

    private fun configuredProject(): Path = createTempDirectory("tokia-configured").also { dir ->
        dir.resolve("settings.gradle.kts").writeText("rootProject.name = \"tokia-consumer\"\n")
        dir.resolve("build.gradle.kts").writeText(
            """
            import io.tokia.cmp.gradle.TokiaExtension

            plugins { id("io.tokia.cmp") }

            extensions.configure(TokiaExtension::class) {
                scssRootDir.set(layout.projectDirectory.dir("src"))
                themePackage.set("com.acme.styles")
                tokensPackage.set("com.acme.styles.tokens")
                adaptivePackage.set("com.acme.styles")
            }
            """.trimIndent() + "\n",
        )
    }

    private fun versions(): List<String> =
        System.getProperty("tokiaCompatGradleVersions")
            ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()

    @Test
    fun appliesOnCurrentGradle() {
        val dir = project()
        val result = GradleRunner.create()
            .withProjectDir(dir.toFile())
            .withPluginClasspath()
            .withArguments("tasks", "--all", "--console=plain")
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue(result.output.contains("generateScssTokens"), result.output)
        assertTrue(result.output.contains("checkStrayDimensions"), result.output)
    }

    @Test
    fun passesWhenMandatoryOptionsSet() {
        val dir = configuredProject()
        val result = GradleRunner.create()
            .withProjectDir(dir.toFile())
            .withPluginClasspath()
            .withArguments("validateTokiaOptions", "--console=plain")
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":validateTokiaOptions")?.outcome)
    }

    @Test
    fun failsWhenMandatoryOptionsMissing() {
        val dir = project()
        val result = GradleRunner.create()
            .withProjectDir(dir.toFile())
            .withPluginClasspath()
            .withArguments("validateTokiaOptions", "--console=plain")
            .buildAndFail()
        assertTrue(result.output.contains("missing required tokiA"), result.output)
    }

    @Test
    fun appliesOnConfiguredGradleMatrix() {
        val targets = versions()
        if (targets.isEmpty()) return
        targets.forEach { version ->
            val dir = project()
            val result = GradleRunner.create()
                .withProjectDir(dir.toFile())
                .withPluginClasspath()
                .withGradleVersion(version)
                .withArguments("tasks", "--all", "--console=plain")
                .build()
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome, "Gradle $version")
            assertTrue(result.output.contains("generateScssTokens"), "Gradle $version:\n${result.output}")
        }
    }
}
