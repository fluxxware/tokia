package io.tokia.cmp.gradle.guard

import java.io.File

/**
 * Rejects dimension literals (`.dp`/`.sp`) in component code — dimensions must come from SCSS
 * tokens. Pure and unit-testable; the Gradle task just throws on a non-empty result.
 */
object StrayDimensionGuard {
    private val pattern = Regex("""\b\d+(\.\d+)?\.(sp|dp)\b""")

    data class Violation(val file: File, val line: Int, val text: String) {
        override fun toString() = "$file:$line: $text"
    }

    fun check(
        files: Collection<File>,
        allowed: Set<String> = setOf("0.dp", "1.dp"),
        excludedPathPatterns: List<String> = emptyList(),
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        files.forEach { file ->
            val path = file.invariantSeparatorsPath
            if (excludedPathPatterns.any { path.contains(it) }) return@forEach
            file.readLines().forEachIndexed { idx, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//")) return@forEachIndexed
                val matches = pattern.findAll(trimmed).map { it.value }.toList()
                if (matches.isEmpty()) return@forEachIndexed
                val outlaw = matches.filterNot { it in allowed }
                if (outlaw.isEmpty()) return@forEachIndexed
                violations.add(Violation(file, idx + 1, trimmed))
            }
        }
        return violations
    }
}
