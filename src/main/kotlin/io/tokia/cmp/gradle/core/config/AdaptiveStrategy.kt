package io.tokia.cmp.gradle.core.config

/**
 * How generated adaptive token getters obtain the window width.
 *
 * - [COMPOSITION_LOCAL]: Tokia owns a private `LocalTokiaAdaptive`; the consumer wraps each window in
 *   `TokiaAdaptiveScope { }`. Works on any platform; arbitrary SCSS breakpoints.
 * - [WINDOW_INFO]: getters read `LocalWindowInfo.current.containerSize` directly. No wrapper, no
 *   extra dependency; arbitrary SCSS breakpoints.
 * - [WINDOW_SIZE_CLASS]: getters read `currentWindowAdaptiveInfoV2().windowSizeClass`
 *   (COMPACT/MEDIUM/EXPANDED). No wrapper; requires
 *   `org.jetbrains.compose.material3.adaptive:adaptive`.
 */
enum class AdaptiveStrategy(val id: String) {
    COMPOSITION_LOCAL("compositionLocal"),
    WINDOW_INFO("windowInfo"),
    WINDOW_SIZE_CLASS("windowSizeClass"),
    ;

    companion object {
        fun fromId(id: String): AdaptiveStrategy =
            entries.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException(
                    "Tokia: unknown adaptiveStrategy '$id'. Expected one of: " +
                        entries.joinToString(", ") { it.id },
                )
    }
}
