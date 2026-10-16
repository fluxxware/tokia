package io.tokia.cmp.gradle.core.config

import io.tokia.cmp.gradle.core.naming.Naming

/**
 * Everything the token pipeline needs that is *not* a file path: the package names it emits into,
 * the generated-object naming (fixed), and the breakpoint discovery convention.
 */
data class TokiaConfig(
    /** Package for the global theme tokens (e.g. `com.acme.ui.styles`). */
    val themePackage: String,
    /** Package for per-component tokens (e.g. `com.acme.ui.styles.tokens`). */
    val tokensPackage: String,
    /** Package for the generated adaptive runtime file (usually == [themePackage]). */
    val adaptivePackage: String,
    /** File names treated as the global theme file. */
    val globalScssFileNames: Set<String> = setOf("variables.scss", "_variables.scss"),
    /** Variable-name prefix that marks a breakpoint in the breakpoints file. */
    val breakpointVariablePrefix: String = "bp-",
    /** How generated adaptive token getters obtain the window width. */
    val adaptiveStrategy: AdaptiveStrategy = AdaptiveStrategy.COMPOSITION_LOCAL,
) {
    fun isGlobal(scssFileName: String): Boolean = scssFileName in globalScssFileNames

    fun classNameFor(scssFileName: String): String =
        Naming.tokenClassName(scssFileName, isGlobal(scssFileName))

    fun packageFor(scssFileName: String): String = if (isGlobal(scssFileName)) themePackage else tokensPackage
}
