package io.github.fluxxware.tokia.gradle.core.config

/**
 * How generated adaptive token getters obtain the window width.
 *
 * Deliberately NOT an `enum`: Kotlin 1.9+ emits a synthetic `entries` property on every enum that
 * references `kotlin.enums.EnumEntriesKt`, which does not exist in the Kotlin stdlib bundled with
 * older Gradle (e.g. 6.8.3/7.x). Using plain singletons keeps the plugin loadable everywhere.
 *
 * - [COMPOSITION_LOCAL]: Tokia owns a private `LocalTokiaAdaptive`; the consumer wraps each window in
 *   `TokiaAdaptiveScope { }`. Works on any platform; arbitrary SCSS breakpoints.
 * - [WINDOW_INFO]: getters read `LocalWindowInfo.current.containerSize` directly. No wrapper, no
 *   extra dependency; arbitrary SCSS breakpoints.
 * - [WINDOW_SIZE_CLASS]: getters read `currentWindowAdaptiveInfoV2().windowSizeClass`
 *   (COMPACT/MEDIUM/EXPANDED). No wrapper; requires
 *   `org.jetbrains.compose.material3.adaptive:adaptive`.
 */
class AdaptiveStrategy private constructor(val id: String) {
    companion object {
        val COMPOSITION_LOCAL = AdaptiveStrategy("compositionLocal")
        val WINDOW_INFO = AdaptiveStrategy("windowInfo")
        val WINDOW_SIZE_CLASS = AdaptiveStrategy("windowSizeClass")

        private val ALL = listOf(COMPOSITION_LOCAL, WINDOW_INFO, WINDOW_SIZE_CLASS)

        fun fromId(id: String): AdaptiveStrategy =
            ALL.firstOrNull { it.id == id }
                ?: throw IllegalArgumentException(
                    "Tokia: unknown adaptiveStrategy '$id'. Expected one of: " +
                        ALL.joinToString(", ") { it.id },
                )
    }
}
