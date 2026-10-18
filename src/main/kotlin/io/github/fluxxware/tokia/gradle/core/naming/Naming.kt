package io.github.fluxxware.tokia.gradle.core.naming

/**
 * Single source of truth for SCSS -> Kotlin identifier/class naming.
 *
 * The generated top-level names are intentionally fixed (not configurable) so the generated API is
 * stable and predictable across projects — like `BuildConfig`/`R`:
 *  - the global theme object is always `GeneratedThemeTokens`
 *  - per-component objects are `<Component>Tokens`
 *  - the adaptive scope composable is `TokiaAdaptiveScope`
 */
object Naming {
    const val THEME_OBJECT_NAME = "GeneratedThemeTokens"
    const val TOKEN_OBJECT_SUFFIX = "Tokens"
    const val ADAPTIVE_SCOPE_NAME = "TokiaAdaptiveScope"

    /** `space-xxs` / `space_xxs` / `space.xxs` -> `SpaceXxs`. */
    fun toTokenId(name: String): String =
        name.split('-', '_', '.').filter { it.isNotBlank() }
            .joinToString("") { it.capitalize() }

    /** Token object class name for a component `.scss` (global file -> `GeneratedThemeTokens`). */
    fun tokenClassName(scssFileName: String, isGlobal: Boolean): String =
        if (isGlobal) THEME_OBJECT_NAME
        else scssFileName.removeSuffix(".scss").capitalize() + TOKEN_OBJECT_SUFFIX
}
