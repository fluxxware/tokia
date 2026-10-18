package io.github.fluxxware.tokia.gradle.core.emit

import io.github.fluxxware.tokia.gradle.core.media.Breakpoints
import io.github.fluxxware.tokia.gradle.core.media.MediaScssParser
import io.github.fluxxware.tokia.gradle.core.media.TierResolver
import io.github.fluxxware.tokia.gradle.core.naming.Naming
import io.github.fluxxware.tokia.gradle.core.parse.ScssText
import java.io.File

/** Finds the members of a component `.scss` that have `@media` variants, resolved per tier. */
class AdaptiveDetector {
    fun detect(file: File, className: String, breakpoints: Breakpoints): List<AdaptiveMember> {
        if (breakpoints.values.isEmpty()) return emptyList()
        val decls = MediaScssParser.parse(ScssText.stripComments(file.readText()), breakpoints.substitutions)
        val adaptiveVars = decls.filter { it.query != null }.map { it.name }.toSet()
        if (adaptiveVars.isEmpty()) return emptyList()

        val tiers = TierResolver.resolve(decls, breakpoints.values)
        return adaptiveVars.mapNotNull { varName ->
            val perTier = tiers.associate { tier ->
                val raw = tier.values[varName] ?: return@associate tier.id to ""
                tier.id to (ValueEmitter.expression(raw) ?: "")
            }
            val firstExpr = perTier.values.firstOrNull { it.isNotEmpty() } ?: return@mapNotNull null
            AdaptiveMember(className, Naming.toTokenId(varName), ValueEmitter.kotlinType(firstExpr), perTier)
        }
    }
}
