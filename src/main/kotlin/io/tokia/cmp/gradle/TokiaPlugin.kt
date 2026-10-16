package io.tokia.cmp.gradle

import io.tokia.cmp.gradle.core.sass.SassResolver
import io.tokia.cmp.gradle.tasks.BootstrapSassTask
import io.tokia.cmp.gradle.tasks.CheckStrayDimensionsTask
import io.tokia.cmp.gradle.tasks.GenerateTokensTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Tokia theme token pipeline to a Kotlin Multiplatform project.
 *
 * Registers `generateScssTokens` (SCSS -> Kotlin tokens + adaptive runtime) and
 * `checkStrayDimensions`, wires the generated dir into the configured source set, and exposes the
 * `tokia { }` extension. `validateTokiaOptions` fails the build when the required options are unset.
 */
class TokiaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create("tokia", TokiaExtension::class.java)

        // Defaults. The four required properties (scssRootDir, themePackage, tokensPackage,
        // adaptivePackage) intentionally have NO convention -> validateTokiaOptions enforces them.
        ext.dartSassVersion.convention("1.86.3")
        ext.downloadDartSass.convention(true)
        ext.dartSassDownloadBaseUrl.convention("https://github.com/sass/dart-sass/releases/download")
        ext.dartSassCacheDir.convention(project.layout.buildDirectory.dir("generated/tokia/sass"))
        ext.sassOutputStyle.convention("expanded")
        ext.sassGenerateSourceMaps.convention(false)
        ext.scssColorsFile.convention(ext.scssRootDir.file("styles/_colors.scss"))
        ext.scssBreakpointsFile.convention(ext.scssRootDir.file("styles/_breakpoints.scss"))
        ext.breakpointVariablePrefix.convention("bp-")
        ext.adaptiveStrategy.convention("compositionLocal")
        ext.globalScssFileNames.convention(setOf("variables.scss", "_variables.scss"))
        ext.scssIncludePatterns.convention(listOf("**/*.scss"))
        ext.scssExcludePatterns.convention(listOf("**/_*.scss"))
        ext.allowedDimensionLiterals.convention(setOf("0.dp", "1.dp"))
        ext.guardExcludedPathPatterns.convention(emptyList())
        ext.guardedTasks.convention(listOf("compileKotlinDesktop", "compileKotlinWasmJs"))
        ext.kotlinSourceSetName.convention("commonMain")
        ext.generatedSourcesDirectory.convention(project.layout.buildDirectory.dir("generated/tokia"))

        val generatedDir = ext.generatedSourcesDirectory
        val workDir = generatedDir.map { it.dir("work") }

        val validate = project.tasks.register("validateTokiaOptions") { task ->
            task.group = "tokia"
            task.description = "Verify the required tokiA { } options are set."
            task.doLast {
                val missing = buildList {
                    if (!ext.scssRootDir.isPresent) add("scssRootDir")
                    if (!ext.themePackage.isPresent) add("themePackage")
                    if (!ext.tokensPackage.isPresent) add("tokensPackage")
                    if (!ext.adaptivePackage.isPresent) add("adaptivePackage")
                }
                if (missing.isNotEmpty()) {
                    throw GradleException(
                        "Tokia: missing required tokiA { } option(s): ${missing.joinToString(", ")}.\n" +
                            "Set them in build.gradle.kts, e.g. " +
                            "tokia { scssRootDir.set(...); themePackage.set(...); tokensPackage.set(...); adaptivePackage.set(...) }.",
                    )
                }
            }
        }

        val bootstrap = project.tasks.register("bootstrapTokiaSass", BootstrapSassTask::class.java) { t ->
            t.group = "tokia"
            t.description = "Ensure the dart-sass binary used to compile theme tokens."
            t.sassHomeDir.set(ext.dartSassCacheDir)
            t.downloadUrl.set(
                ext.dartSassVersion.zip(ext.dartSassDownloadBaseUrl) { version, base ->
                    SassResolver.downloadUrl(base, version)
                },
            )
            t.archiveName.set(SassResolver.archiveName())
            t.zip.set(SassResolver.isZipDownload())
            t.download.set(ext.downloadDartSass)
            t.executableOverride.set(ext.dartSassExecutable)
            t.sassBinary.set(
                ext.dartSassCacheDir.map { dir ->
                    dir.file(if (SassResolver.isZipDownload()) "dart-sass/sass.bat" else "dart-sass/sass")
                },
            )
        }

        val scssFiles = project.fileTree(ext.scssRootDir).matching { tree ->
            ext.scssIncludePatterns.get().forEach { tree.include(it) }
            ext.scssExcludePatterns.get().forEach { tree.exclude(it) }
        }

        val generate = project.tasks.register("generateScssTokens", GenerateTokensTask::class.java) { t ->
            t.group = "tokia"
            t.description = "Generate Kotlin theme tokens + adaptive runtime from SCSS."
            t.dependsOn(validate, bootstrap)
            t.scssFiles.from(scssFiles)
            t.sassBinary.set(bootstrap.flatMap { it.sassBinary })
            t.themePackage.set(ext.themePackage)
            t.tokensPackage.set(ext.tokensPackage)
            t.adaptivePackage.set(ext.adaptivePackage)
            t.globalScssFileNames.set(ext.globalScssFileNames)
            t.breakpointVariablePrefix.set(ext.breakpointVariablePrefix)
            t.adaptiveStrategy.set(ext.adaptiveStrategy)
            t.sassOutputStyle.set(ext.sassOutputStyle)
            t.sassSourceMap.set(ext.sassGenerateSourceMaps)
            t.additionalSassArguments.set(ext.additionalSassArguments)
            t.additionalSassLoadPaths.from(ext.additionalSassLoadPaths)
            t.scssRootDir.set(ext.scssRootDir)
            t.workDir.set(workDir)
            t.outputDir.set(generatedDir)
        }

        val guard = project.tasks.register("checkStrayDimensions", CheckStrayDimensionsTask::class.java) { t ->
            t.group = "tokia"
            t.description = "Reject dimension literals in component code."
            t.dependsOn(validate)
            t.ktFiles.from(project.fileTree(ext.scssRootDir) { tree -> tree.include("**/*.kt") })
            t.allowedLiterals.set(ext.allowedDimensionLiterals)
            t.excludedPathPatterns.set(ext.guardExcludedPathPatterns)
        }

        // Optional inputs and deferred wiring must be resolved after the user's tokiA { } block ran.
        project.afterEvaluate {
            ext.scssBreakpointsFile.orNull?.asFile?.takeIf { it.exists() }?.let {
                generate.configure { t -> t.breakpointsFile.set(ext.scssBreakpointsFile) }
            }
            ext.scssColorsFile.orNull?.asFile?.takeIf { it.exists() }?.let {
                generate.configure { t -> t.scssColorsFile.set(ext.scssColorsFile) }
            }

            val guardedTasks = ext.guardedTasks.get()
            project.tasks.matching { it.name in guardedTasks }.configureEach { it.dependsOn(guard) }

            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                KmpWiring.wireGeneratedSources(project, ext.kotlinSourceSetName.get(), generate)
            }
        }
    }
}
