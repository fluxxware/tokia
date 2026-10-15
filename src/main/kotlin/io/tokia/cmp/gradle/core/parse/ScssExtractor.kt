package io.tokia.cmp.gradle.core.parse

/** Variable names + the `@use`/`@import`/`$var` lines needed to replay them through sass. */
data class ExtractedVars(
    val names: List<String>,
    val preamble: List<String>,
)

object ScssExtractor {
    private val varRegex = Regex("^\\s*\\$([a-zA-Z0-9_.-]+):\\s*(.*?)\\s*;")
    private val useRegex = Regex("^@(?:import|use)\\s")

    fun extract(lines: List<String>): ExtractedVars {
        val vars = lines.mapNotNull { varRegex.matchEntire(it) }
        val names = vars.map { it.groupValues[1] }
        val preamble = lines.filter { useRegex.containsMatchIn(it) || varRegex.containsMatchIn(it) }
        return ExtractedVars(names, preamble)
    }

    fun extractAllFromFile(file: java.io.File): ExtractedVars = extract(file.readLines())
}
