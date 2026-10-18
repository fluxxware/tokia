package io.github.fluxxware.tokia.gradle.core.media

/**
 * Minimal but real CSS/SCSS cascade model for media-query-driven tokens.
 *
 * Supported media features: `min-width`, `max-width`, `and`, comma-separated alternatives.
 * Anything else is rejected so the build fails loudly rather than silently mis-resolving.
 */

/** A `(min-width|max-width: <n>px)` condition. */
data class MediaCondition(val minWidth: Int? = null, val maxWidth: Int? = null) {
    fun matches(width: Int): Boolean {
        if (minWidth != null && width < minWidth) return false
        if (maxWidth != null && width > maxWidth) return false
        return true
    }

    fun and(other: MediaCondition) = MediaCondition(
        minWidth = listOfNotNull(minWidth, other.minWidth).maxOrNull(),
        maxWidth = listOfNotNull(maxWidth, other.maxWidth).minOrNull(),
    )
}

/** A parsed `@media` prelude: alternatives (comma = OR), each a conjunction (`and`). */
data class MediaQuery(val alternatives: List<MediaCondition>) {
    fun matches(width: Int) = alternatives.any { it.matches(width) }
}

/** Where a variable is declared: source order + nesting depth of enclosing `@media`. */
data class DeclValue(val raw: String, val order: Int, val specificity: Int)

/** A parsed SCSS variable declaration, possibly inside one or more `@media` blocks. */
data class VarDecl(val name: String, val value: DeclValue, val query: MediaQuery?)

object MediaQueryParser {
    private val featureRegex = Regex("""\(\s*(min|max)-width\s*:\s*([0-9]+)\s*px\s*\)""")

    fun parse(prelude: String): MediaQuery {
        val alternatives = prelude.split(",").map { alt ->
            var condition = MediaCondition()
            val matches = featureRegex.findAll(alt).toList()
            if (matches.isEmpty()) {
                throw IllegalArgumentException("Unsupported @media prelude (only min-/max-width): '$prelude'")
            }
            val residue = alt
                .replace(featureRegex, "")
                .replace(Regex("""\b(and|only|screen|all)\b"""), "")
                .replace(Regex("""[\s()]"""), "")
            if (residue.isNotEmpty()) {
                throw IllegalArgumentException("Unsupported @media feature in '$prelude' (residue: '$residue')")
            }
            matches.forEach { m ->
                val kind = m.groupValues[1]
                val px = m.groupValues[2].toInt()
                condition = if (kind == "min") condition.copy(minWidth = px) else condition.copy(maxWidth = px)
            }
            condition
        }
        return MediaQuery(alternatives)
    }
}

object MediaCascade {
    fun resolve(decls: List<VarDecl>, width: Int): Map<String, String> {
        val winners = LinkedHashMap<String, VarDecl>()
        for (decl in decls) {
            val applies = decl.query == null || decl.query.matches(width)
            if (!applies) continue
            val current = winners[decl.name]
            if (current == null || beats(decl.value, current.value)) winners[decl.name] = decl
        }
        return winners.mapValues { it.value.value.raw }
    }

    private fun beats(candidate: DeclValue, incumbent: DeclValue): Boolean {
        if (candidate.specificity != incumbent.specificity) return candidate.specificity > incumbent.specificity
        return candidate.order >= incumbent.order
    }
}

/** Parses raw SCSS source into variable declarations, tracking `@media` nesting. */
object MediaScssParser {
    fun parse(source: String, breakpointSubstitutions: Map<String, Int> = emptyMap()): List<VarDecl> {
        val decls = mutableListOf<VarDecl>()
        var order = 0
        val queryStack = ArrayDeque<MediaQuery>()

        val text = source.lineSequence()
            .filterNot { it.trimStart().startsWith("//") }
            .joinToString("\n")

        val tokenRegex = Regex("""@media\s+([^{]+)\{|(\$[A-Za-z0-9_.-]+):\s*([^;{}]+);|\}""")
        for (m in tokenRegex.findAll(text)) {
            when {
                m.groupValues[1].isNotEmpty() -> {
                    var prelude = m.groupValues[1].trim()
                    breakpointSubstitutions.forEach { (name, px) -> prelude = prelude.replace("$$name", "${px}px") }
                    queryStack.addLast(MediaQueryParser.parse(prelude))
                }
                m.groupValues[2].isNotEmpty() -> {
                    val name = m.groupValues[2]
                    if (!name.startsWith("$")) continue
                    val value = m.groupValues[3].trim().removeSuffix("!default").trim()
                    val query = queryStack.lastOrNull()
                    decls.add(VarDecl(name.removePrefix("$"), DeclValue(value, order++, queryStack.size), query))
                }
                else -> {
                    if (queryStack.isNotEmpty()) queryStack.removeLast()
                }
            }
        }
        return decls
    }
}
