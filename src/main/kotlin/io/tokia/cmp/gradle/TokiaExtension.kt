package io.tokia.cmp.gradle

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

/**
 * `tokia { }` configuration — the single configuration point of the plugin.
 *
 * Only [scssRootDir], [themePackage], [tokensPackage] and [adaptivePackage] are **required**; the
 * `validateTokiaOptions` task fails the build if any of them is not set. Every other option has a
 * default that preserves the plugin's standard behavior.
 */
abstract class TokiaExtension {

    // ── Required ─────────────────────────────────────────────────────────────

    /** Directory containing the component `.scss` files (searched recursively). */
    abstract val scssRootDir: DirectoryProperty

    /** Package of the single global theme object (e.g. `com.acme.ui.styles`). */
    abstract val themePackage: Property<String>

    /** Package of the per-component token objects (e.g. `com.acme.ui.styles.tokens`). */
    abstract val tokensPackage: Property<String>

    /** Package of the generated adaptive runtime (where `TokiaAdaptiveScope` lives). */
    abstract val adaptivePackage: Property<String>

    // ── Sass toolchain ───────────────────────────────────────────────────────

    /** dart-sass release version to download. */
    abstract val dartSassVersion: Property<String>

    /** Download dart-sass automatically. Set `false` to use [dartSassExecutable] instead. */
    abstract val downloadDartSass: Property<Boolean>

    /** Path to your own dart-sass binary; when set, no download happens. */
    abstract val dartSassExecutable: RegularFileProperty

    /** Base URL hosting the dart-sass release assets. */
    abstract val dartSassDownloadBaseUrl: Property<String>

    /** Directory where a downloaded dart-sass binary is cached. */
    abstract val dartSassCacheDir: DirectoryProperty

    // ── Sass compilation ─────────────────────────────────────────────────────

    /** dart-sass `--style`: `expanded` or `compressed`. */
    abstract val sassOutputStyle: Property<String>

    /** Emit source maps. `false` passes `--no-source-map`. */
    abstract val sassGenerateSourceMaps: Property<Boolean>

    /** Extra `@use`/`@import` resolution directories, on top of [scssRootDir] and its `styles/`. */
    abstract val additionalSassLoadPaths: ConfigurableFileCollection

    /** Extra raw dart-sass CLI arguments, appended verbatim. */
    abstract val additionalSassArguments: ListProperty<String>

    // ── Inputs ───────────────────────────────────────────────────────────────

    /** Shared `_colors.scss` partial, if any (its variables are folded into the global theme object). */
    abstract val scssColorsFile: RegularFileProperty

    /** File declaring breakpoints; optional. */
    abstract val scssBreakpointsFile: RegularFileProperty

    /** Variable-name prefix that marks a breakpoint inside [scssBreakpointsFile] (default `bp-`). */
    abstract val breakpointVariablePrefix: Property<String>

    /** SCSS file names that produce the global theme object. */
    abstract val globalScssFileNames: SetProperty<String>

    /** Glob patterns selecting the `.scss` files to compile. */
    abstract val scssIncludePatterns: ListProperty<String>

    /** Glob patterns excluding `.scss` files from compilation (default: partials prefixed with `_`). */
    abstract val scssExcludePatterns: ListProperty<String>

    // ── Dimension guard ──────────────────────────────────────────────────────

    /** Dimension literals permitted in component code. */
    abstract val allowedDimensionLiterals: SetProperty<String>

    /** Path substrings; matching source files are exempt from the dimension guard. */
    abstract val guardExcludedPathPatterns: ListProperty<String>

    /** Tasks that trigger the dimension guard. */
    abstract val guardedTasks: ListProperty<String>

    // ── Wiring ───────────────────────────────────────────────────────────────

    /** Kotlin source set the generated tokens are added to. */
    abstract val kotlinSourceSetName: Property<String>

    /** Directory the generated Kotlin sources are written to. */
    abstract val generatedSourcesDirectory: DirectoryProperty
}
