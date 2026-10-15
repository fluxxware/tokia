package io.tokia.cmp.gradle.core.parse

data class ParsedTokens(
    val colors: MutableList<String> = mutableListOf(),
    val sizes: MutableList<String> = mutableListOf(),
    val numbers: MutableList<String> = mutableListOf(),
    val claimedNames: MutableSet<String> = mutableSetOf(),
)

object CssParser {
    private val cssNamedColors = mapOf(
        "transparent" to "00000000", "white" to "FFFFFF", "black" to "000000",
    )

    private val varExportRegex = Regex("--t([A-Za-z0-9]+):\\s*(.*?);")
    private val blockRegex = Regex("""\s*((?:\.[a-zA-Z0-9_-]+\s*)+)\s*\{([^}]*)\}""")
    private val propRegex = Regex("""([a-zA-Z-]+)\s*:\s*(.*?)\s*(?:;|$)""")

    fun parseExports(cssText: String, tokens: ParsedTokens) {
        varExportRegex.findAll(cssText).forEach { m ->
            val name = m.groupValues[1]; val value = m.groupValues[2].trim()
            if (name !in tokens.claimedNames) {
                tokens.claimedNames.add(name)
                addEntry(name, value, tokens)
            }
        }
    }

    fun parseRules(cssText: String, tokens: ParsedTokens) {
        blockRegex.findAll(cssText).forEach { block ->
            val rawSel = block.groupValues[1].trim()
            val selName = rawSel.split(".", " ", "-").filter { it.isNotBlank() }
                .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }

            propRegex.findAll(block.groupValues[2]).forEach { prop ->
                val propName = prop.groupValues[1].split("-").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
                val value = prop.groupValues[2].trim()
                val tokenName = selName + propName
                if (tokenName !in tokens.claimedNames && !value.contains(" ")) {
                    tokens.claimedNames.add(tokenName)
                    addEntry(tokenName, value, tokens)
                }
            }
        }
    }

    private fun addEntry(name: String, value: String, tokens: ParsedTokens) {
        when {
            value.startsWith("#") && value.length in 4..9 ->
                tokens.colors.add("    val $name = Color(0xFF${value.removePrefix("#")})")
            value.endsWith("px") ->
                tokens.sizes.add("    val $name = ${value.removeSuffix("px").trim()}.dp")
            value.endsWith("px !default") ->
                tokens.sizes.add("    val $name = ${value.removeSuffix("px !default").trim()}.dp")
            value.endsWith("!default") -> {
                val clean = value.removeSuffix("!default").trim()
                if (clean.toDoubleOrNull() != null)
                    tokens.numbers.add("    const val $name = $clean")
            }
            value.toDoubleOrNull() != null ->
                tokens.numbers.add("    const val $name = $value")
            else -> {
                val hex = cssNamedColors[value.lowercase()]
                if (hex != null) tokens.colors.add("    val $name = Color(0xFF$hex)")
            }
        }
    }
}
