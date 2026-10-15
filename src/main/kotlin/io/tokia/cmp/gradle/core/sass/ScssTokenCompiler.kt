package io.tokia.cmp.gradle.core.sass

import io.tokia.cmp.gradle.core.config.TokiaConfig
import io.tokia.cmp.gradle.core.naming.Naming
import io.tokia.cmp.gradle.core.parse.ColorsFile
import io.tokia.cmp.gradle.core.parse.CssParser
import io.tokia.cmp.gradle.core.parse.ExtractedVars
import io.tokia.cmp.gradle.core.parse.ParsedTokens
import io.tokia.cmp.gradle.core.parse.ScssExtractor
import java.io.File

/** Compiles a component `.scss` (rules + exported variables) into [ParsedTokens]. */
class ScssTokenCompiler(
    private val sass: SassCompiler,
    private val workDir: File,
    private val colors: ColorsFile,
    private val config: TokiaConfig,
    private val log: (String) -> Unit = {},
) {
    fun compile(file: File): ParsedTokens {
        val tokens = ParsedTokens()
        val vars = ScssExtractor.extract(file.readLines())
        compileRules(file, tokens)
        compileExports(file, vars, tokens)
        return tokens
    }

    private fun compileRules(file: File, tokens: ParsedTokens) {
        try {
            val cssFile = File(workDir, file.nameWithoutExtension + "-rules.css")
            sass.compile(file, cssFile)
            CssParser.parseRules(cssFile.readText(), tokens)
        } catch (e: Exception) {
            log("SCSS: ${file.name} rule error: ${e.message?.substringBefore('\n')}")
        }
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

        try {
            val cssFile = File(workDir, file.nameWithoutExtension + ".css")
            sass.compile(export, cssFile)
            CssParser.parseExports(cssFile.readText(), tokens)
        } catch (e: Exception) {
            log("SCSS: ${file.name} export error: ${e.message?.substringBefore('\n')}")
        }
    }
}
