package io.tokia.cmp.gradle.core.media

import io.tokia.cmp.gradle.core.parse.ScssText
import java.io.File

/**
 * Declared breakpoints.
 *
 * @param substitutions `$bp-*` name -> px (for resolving `@media (max-width: $bp-x)`).
 * @param values ordered px boundaries (the cascade sampling points).
 */
data class Breakpoints(val substitutions: Map<String, Int>, val values: List<Int>) {
    companion object {
        val NONE = Breakpoints(emptyMap(), emptyList())
    }
}

/** Reads `$<prefix>*` breakpoint variables from the breakpoints file. */
class BreakpointReader {
    fun read(breakpointsFile: File?, prefix: String): Breakpoints {
        if (breakpointsFile == null || !breakpointsFile.exists()) return Breakpoints.NONE
        val decls = MediaScssParser.parse(ScssText.stripComments(breakpointsFile.readText()))
        val subs = decls.filter { it.name.startsWith(prefix) && it.query == null }
            .mapNotNull { d -> d.value.raw.removeSuffix("px").trim().toIntOrNull()?.let { d.name to it } }
            .toMap()
        return Breakpoints(subs, TierResolver.breakpointsFrom(decls, prefix))
    }
}
