package io.tokia.cmp.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * All Kotlin Gradle Plugin references live here, deliberately, so that [TokiaPlugin] itself has no
 * KGP-typed members. Gradle decorates the plugin class and resolves every type it references; if
 * those were in [TokiaPlugin], KGP would be required on the classpath just to *apply* the plugin.
 *
 * This class is only loaded from inside `plugins.withId("org.jetbrains.kotlin.multiplatform")`, so
 * the KGP types are resolved only when a KMP project actually applies it.
 */
internal object KmpWiring {
    fun wireGeneratedSources(project: Project, sourceSetName: String, generatedSources: Any) {
        project.extensions.configure(KotlinMultiplatformExtension::class.java) { kotlin ->
            kotlin.sourceSets.named(sourceSetName).configure { it.kotlin.srcDir(generatedSources) }
        }
    }
}
