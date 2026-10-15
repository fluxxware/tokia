package io.tokia.cmp.gradle.app

import io.tokia.cmp.gradle.core.config.TokiaConfig
import io.tokia.cmp.gradle.core.emit.AdaptiveDetector
import io.tokia.cmp.gradle.core.emit.AdaptiveMember
import io.tokia.cmp.gradle.core.emit.AdaptiveWriter
import io.tokia.cmp.gradle.core.emit.TokenClass
import io.tokia.cmp.gradle.core.emit.TokenWriter
import io.tokia.cmp.gradle.core.media.Breakpoints
import io.tokia.cmp.gradle.core.media.BreakpointReader
import io.tokia.cmp.gradle.core.media.TierResolver
import io.tokia.cmp.gradle.core.parse.ParsedTokens
import io.tokia.cmp.gradle.core.sass.ScssTokenCompiler
import java.io.File

/**
 * Entrypoint of the token pipeline. Orchestration only: read breakpoints, compile each SCSS into
 * tokens, detect adaptive members, then emit the token objects + the adaptive runtime file.
 *
 * The stages are injected ([BreakpointReader], [ScssTokenCompiler], [AdaptiveDetector]); this class
 * only sequences them, so it stays a thin, readable entrypoint.
 */
class TokenPipelineEntrypoint(
    private val config: TokiaConfig,
    private val breakpoints: BreakpointReader,
    private val compiler: ScssTokenCompiler,
    private val detector: AdaptiveDetector,
) {
    fun run(scssFiles: List<File>, breakpointsFile: File?): PipelineResult {
        val bp = breakpoints.read(breakpointsFile, config.breakpointVariablePrefix)
        val files = scssFiles.sortedBy { it.absolutePath }
        val tokens = files.map { compiler.compile(it) }
        val adaptive = files.map { detector.detect(it, config.classNameFor(it.name), bp) }
        val tokenFiles = files.mapIndexed { i, file -> emitToken(file, tokens[i], adaptive[i]) }
        val adaptiveFile = emitAdaptive(bp, adaptive.flatten())
        return PipelineResult(tokenFiles, adaptiveFile)
    }

    private fun emitToken(file: File, tokens: ParsedTokens, adaptive: List<AdaptiveMember>): GeneratedFile {
        val className = config.classNameFor(file.name)
        val pkg = config.packageFor(file.name)
        val code = TokenWriter.generate(
            TokenClass(
                packageName = pkg,
                className = className,
                colors = tokens.colors,
                sizes = tokens.sizes,
                numbers = tokens.numbers,
                adaptive = adaptive.map { it.memberName }.toSet(),
                adaptivePackage = config.adaptivePackage,
            ),
        )
        return GeneratedFile("${pkg.replace('.', '/')}/$className.kt", code)
    }

    private fun emitAdaptive(bp: Breakpoints, adaptive: List<AdaptiveMember>): GeneratedFile? {
        if (adaptive.isEmpty()) return null
        val tierMeta = TierResolver.resolve(emptyList(), bp.values)
        val code = AdaptiveWriter.generate(config.adaptivePackage, tierMeta, adaptive)
        return GeneratedFile("${config.adaptivePackage.replace('.', '/')}/TokiaAdaptive.kt", code)
    }
}
