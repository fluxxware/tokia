package io.github.fluxxware.tokia.gradle.core.sass

import io.github.fluxxware.tokia.gradle.core.config.TokiaConfig
import io.github.fluxxware.tokia.gradle.core.parse.ColorsFile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ScssTokenCompilerTest {

    @Test
    fun sassFailureFailsLoudly() {
        val root = File.createTempFile("tokia-scss-fail", "").apply { delete(); mkdirs(); deleteOnExit() }
        val work = File(root, "work").apply { mkdirs() }
        val scss = File(root, "Broken.scss").apply { writeText("\$x: 1px;\n") }

        val throwingSass = SassCompiler { _, _ -> throw RuntimeException("sass exploded: unmatched brace") }
        val compiler = ScssTokenCompiler(
            throwingSass,
            work,
            ColorsFile(File(root, "styles/_colors.scss")),
            TokiaConfig("a", "a.tokens", "a"),
        )

        val ex = assertFailsWith<ScssCompileException> { compiler.compile(scss) }
        assertTrue(ex.message!!.contains("Broken.scss"), ex.message)
        assertTrue(ex.message!!.contains("sass exploded"), ex.message)
    }
}
