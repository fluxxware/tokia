package io.tokia.cmp.gradle.core.emit

import io.tokia.cmp.gradle.core.config.AdaptiveStrategy
import io.tokia.cmp.gradle.core.media.ResolvedTier

data class TokenClass(
    val packageName: String,
    val className: String,
    val colors: List<String>,
    val sizes: List<String>,
    val numbers: List<String>,
    /** Member names that have `@media` variants; emitted as `@Composable` getters. */
    val adaptive: Set<String> = emptySet(),
    /** Package of the generated adaptive runtime (`LocalTokiaAdaptive`); defaults to this file's package. */
    val adaptivePackage: String = packageName,
    /** How adaptive getters obtain the window width. */
    val adaptiveStrategy: AdaptiveStrategy = AdaptiveStrategy.COMPOSITION_LOCAL,
    /** Per-member adaptive data (per-tier expressions), needed by the platform strategies. */
    val adaptiveMembers: List<AdaptiveMember> = emptyList(),
    /** Tier bounds, needed by the platform strategies. */
    val tiers: List<ResolvedTier> = emptyList(),
)

object TokenWriter {
    private val memberRegex = Regex("""(?:const\s+)?val\s+([A-Za-z0-9_]+)""")
    private val exprRegex = Regex("""=\s*(.+)$""")

    fun generate(tokenClass: TokenClass): String {
        val all = tokenClass.colors + tokenClass.sizes + tokenClass.numbers
        val hasAdaptive = all.any { memberNameOf(it) in tokenClass.adaptive }
        val byName = tokenClass.adaptiveMembers.associateBy { it.memberName }

        val sb = StringBuilder()
        sb.appendLine("package ${tokenClass.packageName}")
        sb.appendLine()
        sb.appendLine("import androidx.compose.ui.graphics.Color")
        sb.appendLine("import androidx.compose.ui.unit.dp")
        if (hasAdaptive) {
            sb.appendLine("import androidx.compose.runtime.Composable")
            sb.appendLine("import androidx.compose.runtime.ReadOnlyComposable")
            sb.appendLine("import androidx.compose.ui.unit.Dp")
            sb.appendLine("import androidx.compose.ui.unit.TextUnit")
            sb.appendLine("import androidx.compose.ui.unit.sp")
            when (tokenClass.adaptiveStrategy) {
                AdaptiveStrategy.COMPOSITION_LOCAL ->
                    if (tokenClass.packageName != tokenClass.adaptivePackage) {
                        sb.appendLine("import ${tokenClass.adaptivePackage}.LocalTokiaAdaptive")
                    }

                AdaptiveStrategy.WINDOW_INFO -> {
                    sb.appendLine("import androidx.compose.ui.platform.LocalDensity")
                    sb.appendLine("import androidx.compose.ui.platform.LocalWindowInfo")
                }

                AdaptiveStrategy.WINDOW_SIZE_CLASS -> {
                    sb.appendLine("import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2")
                    sb.appendLine("import androidx.window.core.layout.WindowWidthSizeClass")
                }
            }
        }
        sb.appendLine()
        sb.appendLine("object ${tokenClass.className} {")
        all.forEach { line ->
            val name = memberNameOf(line)
            if (name.isNotEmpty() && name in tokenClass.adaptive) {
                val member = byName[name]
                    ?: error("Tokia: adaptive member '$name' has no per-tier data")
                emitAdaptiveGetter(sb, tokenClass, name, ValueEmitter.shortType(exprOf(line)), member)
            } else {
                sb.appendLine(line)
            }
        }
        sb.appendLine("}")
        return sb.toString() + "\n"
    }

    private fun emitAdaptiveGetter(
        sb: StringBuilder,
        tokenClass: TokenClass,
        name: String,
        type: String,
        member: AdaptiveMember,
    ) {
        when (tokenClass.adaptiveStrategy) {
            AdaptiveStrategy.COMPOSITION_LOCAL -> sb.appendLine(
                "    val $name: $type @Composable @ReadOnlyComposable " +
                    "get() = LocalTokiaAdaptive.current.${tokenClass.className}_$name",
            )

            AdaptiveStrategy.WINDOW_INFO -> {
                sb.appendLine("    val $name: $type")
                sb.appendLine("        @Composable @ReadOnlyComposable")
                sb.appendLine("        get() {")
                sb.appendLine("            val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }")
                sb.appendLine("            return when {")
                tokenClass.tiers.sortedBy { it.maxWidth ?: Int.MAX_VALUE }.forEach { tier ->
                    val expr = member.perTier[tier.id] ?: member.perTier.values.firstOrNull() ?: "0.dp"
                    if (tier.maxWidth != null) sb.appendLine("                width <= ${tier.maxWidth}.dp -> $expr")
                    else sb.appendLine("                else -> $expr")
                }
                sb.appendLine("            }")
                sb.appendLine("        }")
            }

            AdaptiveStrategy.WINDOW_SIZE_CLASS -> {
                sb.appendLine("    val $name: $type")
                sb.appendLine("        @Composable @ReadOnlyComposable")
                sb.appendLine("        get() = when (currentWindowAdaptiveInfoV2().windowSizeClass.windowWidthSizeClass) {")
                sb.appendLine("            WindowWidthSizeClass.COMPACT -> ${exprAt(tokenClass.tiers, member, 0)}")
                sb.appendLine("            WindowWidthSizeClass.MEDIUM -> ${exprAt(tokenClass.tiers, member, 600)}")
                sb.appendLine("            WindowWidthSizeClass.EXPANDED -> ${exprAt(tokenClass.tiers, member, 840)}")
                sb.appendLine("            else -> ${exprAt(tokenClass.tiers, member, 840)}")
                sb.appendLine("        }")
            }
        }
    }

    /** Tier applying at [width], sampled against the SCSS tier bounds. */
    private fun exprAt(tiers: List<ResolvedTier>, member: AdaptiveMember, width: Int): String {
        val tier = tiers.sortedBy { it.maxWidth ?: Int.MAX_VALUE }
            .firstOrNull { it.maxWidth == null || width <= it.maxWidth }
        return tier?.let { member.perTier[it.id] } ?: member.perTier.values.firstOrNull() ?: "0.dp"
    }

    private fun memberNameOf(line: String): String =
        memberRegex.find(line)?.groupValues?.get(1) ?: ""

    private fun exprOf(line: String): String =
        exprRegex.find(line)?.groupValues?.get(1)?.trim() ?: "0.dp"
}
