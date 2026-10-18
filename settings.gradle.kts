plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    // Enables `gradleApi("<version>")` so we can compile the plugin against the oldest supported
    // Gradle API (6.8.3) while building with a modern Gradle. See the compatibility floor in build.gradle.kts.
    id("dev.gradleplugins.gradle-plugin-development") version "1.9.0"
}
rootProject.name = "tokia"