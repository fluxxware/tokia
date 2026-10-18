package io.github.fluxxware.tokia.gradle.core.parse

/** SCSS text utilities. */
object ScssText {
    fun stripComments(text: String): String =
        text.lineSequence().filterNot { it.trimStart().startsWith("//") }.joinToString("\n")
}
