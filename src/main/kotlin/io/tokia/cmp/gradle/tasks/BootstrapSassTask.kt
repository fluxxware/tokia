package io.tokia.cmp.gradle.tasks

import io.tokia.cmp.gradle.core.sass.SassResolver
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Ensures a dart-sass binary is available: uses [executableOverride] if provided, otherwise
 * downloads and caches one (when [download] is enabled). The resolved path is published via
 * [sassBinary] for downstream tasks.
 */
abstract class BootstrapSassTask : DefaultTask() {

    @get:Internal
    abstract val sassHomeDir: DirectoryProperty

    @get:Input
    abstract val downloadUrl: Property<String>

    @get:Input
    abstract val archiveName: Property<String>

    @get:Input
    abstract val zip: Property<Boolean>

    /** Whether dart-sass may be downloaded. */
    @get:Input
    abstract val download: Property<Boolean>

    /** User-provided dart-sass binary; when present, no download happens. */
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val executableOverride: RegularFileProperty

    /** Resolved sass binary path handed downstream (internal, not a declared output). */
    @get:Internal
    abstract val sassBinary: RegularFileProperty

    @TaskAction
    fun ensure() {
        val override = executableOverride.orNull?.asFile
        if (override != null) {
            if (!override.exists()) {
                throw GradleException("Tokia: dartSassExecutable does not exist: ${override.absolutePath}")
            }
            sassBinary.set(override)
            return
        }
        if (!download.get()) {
            throw GradleException(
                "Tokia: downloadDartSass=false but no dartSassExecutable was set. " +
                    "Point tokiA.dartSassExecutable at a dart-sass binary, or set downloadDartSass=true.",
            )
        }
        val home = sassHomeDir.get().asFile
        val bin = SassResolver.sassBinary(home)
        if (bin.exists()) {
            sassBinary.set(bin)
            return
        }
        home.mkdirs()
        val url = downloadUrl.get()
        val archive = File(home, archiveName.get())
        logger.lifecycle("Tokia theme: downloading $url")
        project.ant.invokeMethod("get", mapOf("src" to url, "dest" to archive, "verbose" to false))
        if (zip.get()) {
            project.ant.invokeMethod("unzip", mapOf("src" to archive, "dest" to home))
        } else {
            project.ant.invokeMethod("untar", mapOf("src" to archive, "dest" to home, "compression" to "gzip"))
        }
        bin.setExecutable(true)
        archive.delete()
        sassBinary.set(bin)
    }
}
