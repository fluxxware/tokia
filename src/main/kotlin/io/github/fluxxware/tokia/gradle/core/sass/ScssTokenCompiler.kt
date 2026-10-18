package io.github.fluxxware.tokia.gradle.core.sass

import io.github.fluxxware.tokia.gradle.core.config.TokiaConfig
import io.github.fluxxware.tokia.gradle.core.naming.Naming
import io.github.fluxxware.tokia.gradle.core.parse.ColorsFile
import io.github.fluxxware.tokia.gradle.core.parse.CssParser
import io.github.fluxxware.tokia.gradle.core.parse.ExtractedVars
import io.github.fluxxware.tokia.gradle.core.parse.ParsedTokens
import io.github.fluxxware.tokia.gradle.core.parse.ScssExtractor
import java.io.File

/**
 * Thrown when dart-sass fails to compile a `.scss` file. Tokia fails loudly rather than emitting
 * partial tokens with a green build.
 */
class ScssCompileException(scssFile: File, phase: String, cause: Throwable) : RuntimeException(
    "Tokia: sass failed to compile '${scssFile.name}' ($phase): " +
        (cause.message?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() } ?: cause.javaClass.simpleName),
    cause,
)

/** Compiles a component `.scss` (rules + exported variables) into [ParsedTokens]. */
class ScssTokenCompiler(
    private val sass: SassCompiler,
    private val workDir: File,
    private val colors: ColorsFile,
    private val config: TokiaConfig,
) {
    fun compile(file: File): ParsedTokens {
        val tokens = ParsedTokens()
        val vars = ScssExtractor.extract(file.readLines())
        compileRules(file, tokens)
        compileExports(file, vars, tokens)
        return tokens
    }

    private fun compileRules(file: File, tokens: ParsedTokens) {
        val cssFile = File(workDir, file.nameWithoutExtension + "-rules.css")
        try {
            sass.compile(file, cssFile)
        } catch (e: Exception) {
            throw ScssCompileException(file, "rules", e)
        }
        CssParser.parseRules(cssFile.readText(), tokens)
    }

    private fun compileExports(file: File, vars: ExtractedVars, tokens: ParsedTokens) {
        if (vars.names.isEmpty()) return
        val global = config.isGlobal(file.name)
        val allNames = vars.names.toMutableList()
        if (global) allNames.addAll(colors.varNames())

        val export = File(workDir, file.name)
        val exportLines = mutableListOf<String>().apply {
            addAll(vars.preamble)
            if (global) addAll(colors.preamble())
        }
        export.writeText(
            exportLines.joinToString("\n") + "\n:root {\n" +
                allNames.joinToString("\n") { "  --t${Naming.toTokenId(it)}: #{\$$it};" } + "\n}",
        )

        val cssFile = File(workDir, file.nameWithoutExtension + ".css")
        try {
            sass.compile(export, cssFile)
        } catch (e: Exception) {
            throw ScssCompileException(file, "exports", e)
        }
        CssParser.parseExports(cssFile.readText(), tokens)
    }
}
