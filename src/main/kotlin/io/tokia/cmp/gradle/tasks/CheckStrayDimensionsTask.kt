package io.tokia.cmp.gradle.tasks

import io.tokia.cmp.gradle.guard.StrayDimensionGuard
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Fails the build when component code contains dimension literals (`.dp`/`.sp`). */
@CacheableTask
abstract class CheckStrayDimensionsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val ktFiles: ConfigurableFileCollection

    @get:Input
    abstract val allowedLiterals: SetProperty<String>

    /** Path substrings; matching files are exempt from the guard. */
    @get:Input
    abstract val excludedPathPatterns: ListProperty<String>

    @TaskAction
    fun check() {
        val violations = StrayDimensionGuard.check(
            ktFiles.files,
            allowedLiterals.get(),
            excludedPathPatterns.get(),
        )
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Dimension literals found in component code. " +
                    "Add a \$var to the component's .scss and reference the generated token instead.\n" +
                    violations.joinToString("\n"),
            )
        }
    }
}
