package io.tokia.cmp.gradle.core.emit

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
)

object TokenWriter {
    private val memberRegex = Regex("""(?:const\s+)?val\s+([A-Za-z0-9_]+)""")
    private val exprRegex = Regex("""=\s*(.+)$""")

    fun generate(tokenClass: TokenClass): String {
        val all = tokenClass.colors + tokenClass.sizes + tokenClass.numbers
        val hasAdaptive = all.any { memberNameOf(it) in tokenClass.adaptive }

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
        }
        if (hasAdaptive && tokenClass.packageName != tokenClass.adaptivePackage) {
            sb.appendLine("import ${tokenClass.adaptivePackage}.LocalTokiaAdaptive")
        }
        sb.appendLine()
        sb.appendLine("object ${tokenClass.className} {")
        all.forEach { line ->
            val name = memberNameOf(line)
            if (name != null && name in tokenClass.adaptive) {
                val expr = exprOf(line)
                val type = ValueEmitter.shortType(expr)
                sb.appendLine(
                    "    val $name: $type @Composable @ReadOnlyComposable " +
                        "get() = LocalTokiaAdaptive.current.${tokenClass.className}_$name",
                )
            } else {
                sb.appendLine(line)
            }
        }
        sb.appendLine("}")
        return sb.toString() + "\n"
    }

    private fun memberNameOf(line: String): String =
        memberRegex.find(line)?.groupValues?.get(1) ?: ""

    private fun exprOf(line: String): String =
        exprRegex.find(line)?.groupValues?.get(1)?.trim() ?: "0.dp"
}
