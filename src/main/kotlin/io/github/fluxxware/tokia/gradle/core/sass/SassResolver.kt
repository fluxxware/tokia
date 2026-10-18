package io.github.fluxxware.tokia.gradle.core.sass

import java.io.File

/** Resolves the dart-sass binary location for the host OS/arch. */
object SassResolver {
    fun sassBinary(sassHomeDir: File): File {
        val name = if (isWindows()) "sass.bat" else "sass"
        return File(File(sassHomeDir, "dart-sass"), name)
    }

    fun downloadUrl(baseUrl: String, version: String): String {
        val base = "${baseUrl.trimEnd('/')}/$version"
        val archSuffix = if (isArm()) "arm64" else "x64"
        val os = System.getProperty("os.name").toLowerCase()
        return when {
            os.contains("win") -> "$base/dart-sass-$version-windows-$archSuffix.zip"
            os.contains("mac") -> "$base/dart-sass-$version-macos-$archSuffix.tar.gz"
            else -> "$base/dart-sass-$version-linux-$archSuffix.tar.gz"
        }
    }

    fun archiveName(): String = if (isWindows()) "sass.zip" else "sass.tar.gz"

    fun isZipDownload(): Boolean = isWindows()

    private fun isWindows(): Boolean = System.getProperty("os.name").toLowerCase().contains("win")

    private fun isArm(): Boolean {
        val arch = System.getProperty("os.arch").toLowerCase()
        return arch.contains("aarch64") || arch.contains("arm64")
    }
}

/** Compiles a `.scss` file to a `.css` file. Implemented by the Gradle task via `ExecOperations`. */
fun interface SassCompiler {
    fun compile(source: File, target: File)
}
