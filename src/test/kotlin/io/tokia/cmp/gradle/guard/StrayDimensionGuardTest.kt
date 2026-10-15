package io.tokia.cmp.gradle.guard

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StrayDimensionGuardTest {

    private fun kt(content: String): File =
        File.createTempFile("guard", ".kt").apply { writeText(content) }

    @Test
    fun flagsLiteralDimensions() {
        val f = kt("val x = Modifier.width(24.dp)")
        val v = StrayDimensionGuard.check(listOf(f))
        assertEquals(1, v.size)
        assertTrue(v[0].text.contains("24.dp"))
    }

    @Test
    fun allowsZeroAndOne() {
        assertEquals(0, StrayDimensionGuard.check(listOf(kt("val x = 0.dp\nval y = 1.dp"))).size)
    }

    @Test
    fun ignoresComments() {
        assertEquals(0, StrayDimensionGuard.check(listOf(kt("// 24.dp is fine in a comment"))).size)
    }

    @Test
    fun flagsSpToo() {
        assertEquals(1, StrayDimensionGuard.check(listOf(kt("val t = 14.sp"))).size)
    }
}
