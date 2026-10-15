package io.tokia.cmp.gradle.tasks

import io.tokia.cmp.gradle.app.TokenPipelineEntrypoint
import io.tokia.cmp.gradle.core.config.TokiaConfig
import io.tokia.cmp.gradle.core.emit.AdaptiveDetector
import io.tokia.cmp.gradle.core.media.BreakpointReader
import io.tokia.cmp.gradle.core.parse.ColorsFile
import io.tokia.cmp.gradle.core.sass.SassCompiler
import io.tokia.cmp.gradle.core.sass.ScssTokenCompiler
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

/**
 * Runs the token pipeline and writes the generated Kotlin. Cacheable: inputs are the SCSS files,
 * breakpoints, sass binary, package/naming options, and the sass CLI options; output is the
 * generated dir.
 */
@CacheableTask
abstract class GenerateTokensTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val scssFiles: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val breakpointsFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val sassBinary: RegularFileProperty

    @get:Input
    abstract val themePackage: Property<String>

    @get:Input
    abstract val tokensPackage: Property<String>

    @get:Input
    abstract val adaptivePackage: Property<String>

    @get:Input
    abstract val globalScssFileNames: SetProperty<String>

    @get:Input
    abstract val breakpointVariablePrefix: Property<String>

    @get:Input
    abstract val sassOutputStyle: Property<String>

    @get:Input
    abstract val sassSourceMap: Property<Boolean>

    @get:Input
    abstract val additionalSassArguments: ListProperty<String>

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val additionalSassLoadPaths: ConfigurableFileCollection

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val scssColorsFile: RegularFileProperty

    @get:Internal
    abstract val scssRootDir: DirectoryProperty

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val execOps: ExecOperations

    @TaskAction
    fun generate() {
        val root = scssRootDir.get().asFile
        val sassBin = sassBinary.get().asFile
        val style = sassOutputStyle.get()
        val sourceMap = sassSourceMap.get()
        val extraArgs = additionalSassArguments.get()
        val loadPaths = (listOf(root, File(root, "styles")) + additionalSassLoadPaths.files).distinct()

        val compiler = SassCompiler { source, target ->
            execOps.exec { spec ->
                spec.commandLine(
                    buildList {
                        add(sassBin.absolutePath)
                        add("--style=$style")
                        if (!sourceMap) add("--no-source-map")
                        loadPaths.forEach { add("--load-path=${it.absolutePath}") }
                        addAll(extraArgs)
                        add(source.absolutePath)
                        add(target.absolutePath)
                    },
                )
            }
        }

        val config = TokiaConfig(
            themePackage = themePackage.get(),
            tokensPackage = tokensPackage.get(),
            adaptivePackage = adaptivePackage.get(),
            globalScssFileNames = globalScssFileNames.get(),
            breakpointVariablePrefix = breakpointVariablePrefix.get(),
        )
        val colors = ColorsFile(scssColorsFile.orNull?.asFile ?: File(root, "styles/_colors.scss"))
        val tokenCompiler = ScssTokenCompiler(compiler, workDir.get().asFile, colors, config) { msg ->
            logger.warn(msg)
        }
        val pipeline = TokenPipelineEntrypoint(config, BreakpointReader(), tokenCompiler, AdaptiveDetector())
        val result = pipeline.run(scssFiles.files.toList(), breakpointsFile.orNull?.asFile)

        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        (result.tokenFiles + listOfNotNull(result.adaptiveFile)).forEach { generated ->
            File(out, generated.relativePath).apply {
                parentFile.mkdirs()
                writeText(generated.content)
            }
        }
        logger.lifecycle("Tokia theme: ${result.tokenFiles.size} token file(s) generated")
    }
}
