plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
    kotlin("jvm") version "2.1.0"
}

group = "io.github.fluxxware"
version = "0.1.0"

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    // THE compatibility floor: compile against the OLDEST supported Gradle API (6.8.3) while the
    // build itself runs on a modern Gradle. This is what lets the plugin run on Gradle 6.8.3+.
    compileOnly(gradleApi("6.8.3"))
    // NOTE: deliberately NO Kotlin Gradle Plugin dependency — the plugin is Kotlin-version-agnostic
    // and reaches the KMP source sets by reflection (see KmpCompat).
    compileOnly(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

java {
    // Bytecode floor: the plugin runs on any JDK 8+ Gradle daemon (Gradle 6.8.3+).
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

gradlePlugin {
    website.set("https://github.com/fluxxware/tokia")
    vcsUrl.set("https://github.com/fluxxware/tokia")
    plugins {
        create("tokiaTheme") {
            id = "io.github.fluxxware.tokia"
            implementationClass = "io.github.fluxxware.tokia.gradle.TokiaPlugin"
            displayName = "Tokia: SCSS -> Kotlin theme tokens"
            description = "Generates Kotlin theme tokens and adaptive dimensions from co-located SCSS."
            tags.set(listOf("kotlin", "multiplatform", "compose", "scss", "sass", "design-tokens", "theme"))
        }
    }
}

// The published plugin must apply on the oldest supported Gradle; newer Gradle runs older-built
// plugins. This matrix proves both ends (see src/test/.../compatibility).
tasks.jar {
    // Old Gradle (<8) Kotlin DSL cannot parse the Kotlin 2.x `.kotlin_module` metadata and throws
    // InvalidProtocolBufferException during build-script compilation. The plugin doesn't need it.
    exclude("**/*.kotlin_module")
}

tasks.test {
    useJUnitPlatform()
}
