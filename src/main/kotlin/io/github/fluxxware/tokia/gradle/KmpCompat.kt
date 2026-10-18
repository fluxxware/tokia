package io.github.fluxxware.tokia.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet

/**
 * The ONE place that deals with Kotlin/Gradle compatibility.
 *
 * Tokia never links against the Kotlin Gradle Plugin. It reaches the Kotlin Multiplatform source
 * sets by **reflection**, so it works with any KGP version (1.7.10 … 2.1.0+) on any Gradle
 * 6.8.3+ / JDK 8+ build. If anything is off, it fails **loudly** with the exact fix.
 *
 * No other file in this plugin references KGP or does version-specific juggling.
 */
internal object KmpCompat {

    const val KMP_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"

    fun isKmpApplied(project: Project): Boolean = project.pluginManager.hasPlugin(KMP_PLUGIN_ID)

    /** Throws the "KMP is required" failure. */
    fun requireKmp(): Nothing = throw GradleException(
        "Tokia requires the Kotlin Multiplatform plugin (id \"$KMP_PLUGIN_ID\"). " +
            "Apply it, or disable source auto-wiring with tokia { autoWireGeneratedSources.set(false) }.",
    )

    /**
     * Add [generated] to the KMP source set [sourceSetName]. Reflection only — no KGP types are
     * referenced, so this works across KGP versions. Any failure throws a GradleException telling
     * the user exactly how to wire it manually.
     */
    fun wireGeneratedSources(project: Project, sourceSetName: String, generated: Any) {
        val kotlin = project.extensions.findByName("kotlin")
            ?: fail(sourceSetName, "no 'kotlin' extension is registered on the project")
        try {
            val sourceSets = kotlin.javaClass.getMethod("getSourceSets").invoke(kotlin)
            val sourceSet = sourceSets.javaClass
                .getMethod("getByName", String::class.java)
                .invoke(sourceSets, sourceSetName)
            val dir = sourceSet.javaClass.getMethod("getKotlin").invoke(sourceSet)
            (dir as SourceDirectorySet).srcDir(generated) // Gradle API — carries the task dependency
        } catch (e: Throwable) {
            fail(sourceSetName, e.message ?: e.javaClass.name, e)
        }
    }

    private fun fail(sourceSetName: String, cause: String, e: Throwable? = null): Nothing = throw GradleException(
        buildString {
            appendLine("Tokia could not add its generated sources to the Kotlin source set '$sourceSetName'.")
            appendLine()
            appendLine("Tokia auto-wires the generated directory WITHOUT depending on a specific Kotlin Gradle Plugin version.")
            appendLine("Fix it with ONE of the following in build.gradle.kts:")
            appendLine("  1) kotlin { sourceSets.getByName(\"$sourceSetName\").kotlin.srcDir(tasks.named(\"generateScssTokens\")) }")
            appendLine("  2) tokia { autoWireGeneratedSources.set(false) }")
            appendLine()
            appendLine("Cause: $cause")
        },
        e,
    )
}
