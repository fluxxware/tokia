package io.github.fluxxware.tokia.gradle.core.emit

import io.github.fluxxware.tokia.gradle.core.media.ResolvedTier

/** One adaptive token member: which `XxxTokens` object/member, its type, and per-tier expressions. */
data class AdaptiveMember(
    val className: String,
    val memberName: String,
    val type: String,
    val perTier: Map<String, String>,
)

/**
 * Emits the single generated adaptive runtime file: the frozen per-tier data, the
 * `LocalTokiaAdaptive` CompositionLocal, the tier selector, and the `TokiaAdaptiveScope` root
 * composable. Per-component `XxxTokens` members read from `LocalTokiaAdaptive.current`.
 */
object AdaptiveWriter {
    fun generate(
        packageName: String,
        tierMeta: List<ResolvedTier>,
        members: List<AdaptiveMember>,
        scopeName: String = "TokiaAdaptiveScope",
    ): String {
        val sb = StringBuilder()
        sb.appendLine("package $packageName")
        sb.appendLine()
        sb.appendLine("import androidx.compose.runtime.Composable")
        sb.appendLine("import androidx.compose.runtime.CompositionLocalProvider")
        sb.appendLine("import androidx.compose.runtime.Immutable")
        sb.appendLine("import androidx.compose.runtime.staticCompositionLocalOf")
        sb.appendLine("import androidx.compose.ui.ExperimentalComposeUiApi")
        sb.appendLine("import androidx.compose.ui.graphics.Color")
        sb.appendLine("import androidx.compose.ui.platform.LocalDensity")
        sb.appendLine("import androidx.compose.ui.platform.LocalWindowInfo")
        sb.appendLine("import androidx.compose.ui.unit.Dp")
        sb.appendLine("import androidx.compose.ui.unit.TextUnit")
        sb.appendLine("import androidx.compose.ui.unit.dp")
        sb.appendLine("import androidx.compose.ui.unit.sp")
        sb.appendLine()

        val fieldName = { m: AdaptiveMember -> "${m.className}_${m.memberName}" }

        sb.appendLine("@Immutable")
        sb.appendLine("internal data class TokiaTier(")
        members.forEach { m -> sb.appendLine("    val ${fieldName(m)}: ${ValueEmitter.shortType(m.perTier.values.firstOrNull() ?: "0.dp")},") }
        sb.appendLine(")")
        sb.appendLine()

        tierMeta.forEach { tier ->
            val instName = "Tokia${tier.id}Tier"
            sb.appendLine("internal val $instName = TokiaTier(")
            members.forEach { m ->
                val expr = m.perTier[tier.id] ?: m.perTier.values.firstOrNull() ?: "0.dp"
                sb.appendLine("    ${fieldName(m)} = $expr,")
            }
            sb.appendLine(")")
            sb.appendLine()
        }

        sb.appendLine("/**")
        sb.appendLine(" * Current adaptive tier for this composition, provided by [$scopeName].")
        sb.appendLine(" *")
        sb.appendLine(" * Adaptive token members (e.g. `DashboardPageTokens.ActionsTouchTarget`) are `@Composable` getters")
        sb.appendLine(" * over this local — read them inside composition (as arguments or in the body). They are NOT")
        sb.appendLine(" * valid as default parameter values (Compose forbids `@Composable` calls there).")
        sb.appendLine(" *")
        sb.appendLine(" * Because this is a CompositionLocal (not a global), it is per-composition: multiple windows")
        sb.appendLine(" * each get their own tier, and there is no lag when the window resizes.")
        sb.appendLine(" */")
        sb.appendLine("internal val LocalTokiaAdaptive = staticCompositionLocalOf { Tokia${tierMeta.first().id}Tier }")
        sb.appendLine()

        sb.appendLine("internal fun selectTokiaTier(width: Dp): TokiaTier = when {")
        tierMeta.forEach { tier ->
            val inst = "Tokia${tier.id}Tier"
            if (tier.maxWidth != null) sb.appendLine("    width <= ${tier.maxWidth}.dp -> $inst")
            else sb.appendLine("    else -> $inst")
        }
        sb.appendLine("}")
        sb.appendLine()

        sb.appendLine("/**")
        sb.appendLine(" * Root scope for adaptive tokens. Reads the window width via [LocalWindowInfo] (no")
        sb.appendLine(" * SubcomposeLayout) and provides the matching generated tier through [LocalTokiaAdaptive].")
        sb.appendLine(" * Wrap the app once.")
        sb.appendLine(" */")
        sb.appendLine("@Composable")
        sb.appendLine("@OptIn(ExperimentalComposeUiApi::class)")
        sb.appendLine("fun $scopeName(content: @Composable () -> Unit) {")
        sb.appendLine("    val widthDp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }")
        sb.appendLine("    CompositionLocalProvider(LocalTokiaAdaptive provides selectTokiaTier(widthDp)) {")
        sb.appendLine("        content()")
        sb.appendLine("    }")
        sb.appendLine("}")
        return sb.toString()
    }
}
