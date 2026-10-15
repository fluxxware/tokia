package io.tokia.cmp.gradle.app

/** A generated Kotlin file: relative path (package-as-directories) + content. */
data class GeneratedFile(val relativePath: String, val content: String)

data class PipelineResult(
    val tokenFiles: List<GeneratedFile>,
    val adaptiveFile: GeneratedFile?,
)
