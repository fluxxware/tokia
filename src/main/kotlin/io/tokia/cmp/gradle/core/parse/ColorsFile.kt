package io.tokia.cmp.gradle.core.parse

import java.io.File

/** Access to the shared `_colors.scss` partial (variable names + the `@use` preamble). */
class ColorsFile(private val file: File) {
    fun varNames(): List<String> =
        if (file.exists()) ScssExtractor.extractAllFromFile(file).names else emptyList()

    fun preamble(): List<String> =
        if (file.exists()) ScssExtractor.extractAllFromFile(file).preamble else emptyList()
}
