package io.github.fluxxware.tokia.gradle.core.emit

/** Converts a raw CSS/SCSS value into a Kotlin expression. */
object ValueEmitter {
    private val cssNamedColors = mapOf(
        "transparent" to "00000000", "white" to "FFFFFF", "black" to "000000",
    )

    fun expression(raw: String): String? {
        val value = raw.trim()
        return when {
            value == "true" || value == "false" -> value
            value.startsWith("#") && value.length in 4..9 ->
                "Color(0xFF${value.removePrefix("#")})"
            value.endsWith("px") && value.removeSuffix("px").trim().toDoubleOrNull() != null ->
                "${value.removeSuffix("px").trim()}.dp"
            value.endsWith("sp") && value.removeSuffix("sp").trim().toDoubleOrNull() != null ->
                "${value.removeSuffix("sp").trim()}.sp"
            value.endsWith("%") && value.removeSuffix("%").trim().toDoubleOrNull() != null ->
                "${value.removeSuffix("%").trim().toDouble() / 100.0}f"
            value.toDoubleOrNull() != null -> value
            cssNamedColors[value.toLowerCase()] != null -> "Color(0xFF${cssNamedColors[value.toLowerCase()]})"
            else -> null
        }
    }

    fun isColor(raw: String): Boolean {
        val v = raw.trim()
        return (v.startsWith("#") && v.length in 4..9) || cssNamedColors.containsKey(v.toLowerCase())
    }

    fun kotlinType(expression: String): String = when {
        expression.endsWith(".dp") -> "androidx.compose.ui.unit.Dp"
        expression.endsWith(".sp") -> "androidx.compose.ui.unit.TextUnit"
        expression.startsWith("Color") -> "androidx.compose.ui.graphics.Color"
        expression == "true" || expression == "false" -> "Boolean"
        expression.endsWith("f") && expression.dropLast(1).toDoubleOrNull() != null -> "Float"
        expression.toDoubleOrNull() != null -> "Double"
        else -> "Any"
    }

    fun shortType(expression: String): String = when (kotlinType(expression)) {
        "androidx.compose.ui.unit.Dp" -> "Dp"
        "androidx.compose.ui.unit.TextUnit" -> "TextUnit"
        "androidx.compose.ui.graphics.Color" -> "Color"
        else -> kotlinType(expression)
    }
}
