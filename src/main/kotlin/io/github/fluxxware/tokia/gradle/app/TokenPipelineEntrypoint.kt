package io.github.fluxxware.tokia.gradle.app

import io.github.fluxxware.tokia.gradle.core.config.AdaptiveStrategy
import io.github.fluxxware.tokia.gradle.core.config.TokiaConfig
import io.github.fluxxware.tokia.gradle.core.emit.AdaptiveDetector
import io.github.fluxxware.tokia.gradle.core.emit.AdaptiveMember
import io.github.fluxxware.tokia.gradle.core.emit.AdaptiveWriter
import io.github.fluxxware.tokia.gradle.core.emit.TokenClass
import io.github.fluxxware.tokia.gradle.core.emit.TokenWriter
import io.github.fluxxware.tokia.gradle.core.media.BreakpointReader
import io.github.fluxxware.tokia.gradle.core.media.ResolvedTier
import io.github.fluxxware.tokia.gradle.core.media.TierResolver
import io.github.fluxxware.tokia.gradle.core.parse.ParsedTokens
import io.github.fluxxware.tokia.gradle.core.sass.ScssTokenCompiler
import java.io.File

/**
 * Entrypoint of the token pipeline. Orchestration only: read breakpoints, compile each SCSS into
 * tokens, detect adaptive members, then emit the token objects + (for the composition-local strategy)
 * the adaptive runtime file.
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
        val tiers = if (bp.values.isEmpty()) emptyList() else TierResolver.resolve(emptyList(), bp.values)
        val tokenFiles = files.mapIndexed { i, file -> emitToken(file, tokens[i], adaptive[i], tiers) }
        val adaptiveFile = emitAdaptive(adaptive.flatten(), tiers)
        return PipelineResult(tokenFiles, adaptiveFile)
    }

    private fun emitToken(
        file: File,
        tokens: ParsedTokens,
        adaptive: List<AdaptiveMember>,
        tiers: List<ResolvedTier>,
    ): GeneratedFile {
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
                adaptiveStrategy = config.adaptiveStrategy,
                adaptiveMembers = adaptive,
                tiers = tiers,
            ),
        )
        return GeneratedFile("${pkg.replace('.', '/')}/$className.kt", code)
    }

    private fun emitAdaptive(adaptive: List<AdaptiveMember>, tiers: List<ResolvedTier>): GeneratedFile? {
        // The composition-local strategy needs the generated runtime file; the platform strategies
        // read the platform's own window state, so no runtime file is emitted.
        if (config.adaptiveStrategy != AdaptiveStrategy.COMPOSITION_LOCAL) return null
        if (adaptive.isEmpty()) return null
        val code = AdaptiveWriter.generate(config.adaptivePackage, tiers, adaptive)
        return GeneratedFile("${config.adaptivePackage.replace('.', '/')}/TokiaAdaptive.kt", code)
    }
}
