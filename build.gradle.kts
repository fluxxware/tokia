plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
    kotlin("jvm") version "2.1.0"
}

group = "io.tokia.cmp"
version = "0.1.0"

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    // KGP is provided by the consumer at runtime (they apply the Kotlin Multiplatform plugin).
    // Pinned to the consumers' version so the plugin links against the same API surface.
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

kotlin {
    // JVM 17 floor: loads on every consumer Gradle daemon (8.x on JDK 17, 9.x on JDK 17+).
    jvmToolchain(17)
}

gradlePlugin {
    // TODO: point these at the real GitHub repo once created.
    website.set("https://github.com/equeld/tokia-cmp")
    vcsUrl.set("https://github.com/equeld/tokia-cmp")
    plugins {
        create("tokiaTheme") {
            id = "io.tokia.cmp"
            implementationClass = "io.tokia.cmp.gradle.TokiaPlugin"
            displayName = "Tokia: SCSS -> Kotlin theme tokens"
            description = "Generates Kotlin theme tokens and adaptive dimensions from co-located SCSS."
            tags.set(listOf("kotlin", "multiplatform", "compose", "scss", "sass", "design-tokens", "theme"))
        }
    }
}

// The published plugin must apply on the oldest supported Gradle; newer Gradle runs older-built
// plugins. This matrix proves both ends (see src/test/.../compatibility).
tasks.test {
    useJUnitPlatform()
}
