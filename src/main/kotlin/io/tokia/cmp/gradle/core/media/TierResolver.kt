package io.tokia.cmp.gradle.core.media

/**
 * A resolved set of tokens for one width tier.
 *
 * @param id stable identifier, e.g. `Regular`, `Tier1`.
 * @param minWidth inclusive lower bound (null = -inf).
 * @param maxWidth inclusive upper bound (null = +inf).
 * @param values variable name -> raw SCSS value.
 */
data class ResolvedTier(
    val id: String,
    val minWidth: Int?,
    val maxWidth: Int?,
    val values: Map<String, String>,
)

object TierResolver {
    private const val SAMPLE_CEILING = 100_000

    /** Builds the tier set: N declared breakpoints -> N+1 frozen value sets. */
    fun resolve(decls: List<VarDecl>, breakpoints: List<Int>): List<ResolvedTier> {
        val sorted = breakpoints.distinct().sorted()
        if (sorted.isEmpty()) {
            return listOf(ResolvedTier("Regular", null, null, MediaCascade.resolve(decls, SAMPLE_CEILING)))
        }
        val tiers = mutableListOf<ResolvedTier>()
        val edges = listOf(0) + sorted + listOf(SAMPLE_CEILING)
        for (i in 0 until edges.size - 1) {
            val lower = if (i == 0) null else edges[i]
            val upper = if (i == edges.size - 2) null else edges[i + 1] - 1
            val sample = if (i == 0) edges[1] - 1 else edges[i + 1] - 1
            tiers.add(
                ResolvedTier(
                    id = if (i == 0) "Regular" else "Tier$i",
                    minWidth = lower,
                    maxWidth = upper,
                    values = MediaCascade.resolve(decls, sample.coerceAtLeast(0)),
                ),
            )
        }
        return tiers
    }

    /** Extracts breakpoint declarations (px) from parsed vars, matching the configured prefix. */
    fun breakpointsFrom(decls: List<VarDecl>, prefix: String = "bp-"): List<Int> =
        decls.filter { it.name.startsWith(prefix) && it.query == null }
            .mapNotNull { it.value.raw.removeSuffix("px").trim().toIntOrNull() }
            .distinct()
            .sorted()
}
