# Tokia

**SCSS-powered design tokens for Kotlin Multiplatform** — type-safe Kotlin from your SCSS, with a
build-time guard against hardcoded dimensions.

[![Gradle](https://img.shields.io/badge/Gradle-6.8.3%2B-blue)](#requirements)
[![JVM](https://img.shields.io/badge/JVM-8%2B-orange)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-any-blueviolet)](#requirements)
[![version](https://img.shields.io/badge/version-0.1.0-informational)](#install)
[![license](https://img.shields.io/badge/license-MIT-green)](#license)

Keep writing SCSS. Use native Kotlin. Enforce design consistency at build time.

## What it is

Tokia compiles co-located `.scss` (variables, `@use`/`@forward`, mixins, functions, media queries)
into typed Kotlin objects for **Compose Multiplatform** — no runtime CSS — and **fails the build** on
hardcoded `.dp`/`.sp` literals.

Every `.scss` file becomes a `<Name>Tokens` object; the global file (by default `variables.scss`)
becomes `GeneratedThemeTokens`.

## Requirements

- **Gradle 6.8.3+** (verified through 9.6)
- **JVM 8+**
- **Any Kotlin/KGP version** — Tokia has no Kotlin-plugin dependency
- **Kotlin Multiplatform + Compose Multiplatform**
- **dart-sass** — downloaded automatically by default, or bring your own (`dartSassExecutable`)

## Install

```kotlin
// build.gradle.kts
plugins {
    id("io.github.fluxxware.tokia") version "0.1.0"
}
```

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()          // only needed before it is published to the Portal
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
```

## Quick start

```kotlin
tokia {
    // These four have NO default — the build fails if any is missing.
    scssRootDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/acme/ui"))
    themePackage.set("com.acme.ui.styles")
    tokensPackage.set("com.acme.ui.styles.tokens")
    adaptivePackage.set("com.acme.ui.styles")
}
```

The generated sources are added to `commonMain` automatically — nothing else is required.

## Options

### Required

| Option | Meaning |
|---|---|
| `scssRootDir` | Directory containing the component `.scss` files (searched recursively) |
| `themePackage` | Package of the global theme object |
| `tokensPackage` | Package of the per-component token objects |
| `adaptivePackage` | Package of the generated adaptive runtime (where `TokiaAdaptiveScope` lives) |

### Sass toolchain

| Option | Default | Meaning |
|---|---|---|
| `dartSassVersion` | `1.86.3` | dart-sass release to download |
| `downloadDartSass` | `true` | Auto-download dart-sass |
| `dartSassExecutable` | *(unset)* | Path to your own dart-sass; skips the download |
| `dartSassDownloadBaseUrl` | GitHub releases | Base URL hosting the dart-sass assets |
| `dartSassCacheDir` | `build/tokia/dart-sass` | Where a downloaded binary is cached |

### Sass compilation

| Option | Default | Meaning |
|---|---|---|
| `sassOutputStyle` | `expanded` | dart-sass `--style`: `expanded` or `compressed` |
| `sassGenerateSourceMaps` | `false` | Emit source maps (`false` → `--no-source-map`) |
| `additionalSassLoadPaths` | *(empty)* | Extra `@use`/`@import` dirs, on top of `scssRootDir` and its `styles/` |
| `additionalSassArguments` | *(empty)* | Extra raw dart-sass CLI flags |

### Inputs

| Option | Default | Meaning |
|---|---|---|
| `scssColorsFile` | `<scssRootDir>/styles/_colors.scss` | Shared colors partial (folded into the global object) |
| `scssBreakpointsFile` | `<scssRootDir>/styles/_breakpoints.scss` | File declaring breakpoints |
| `breakpointVariablePrefix` | `bp-` | Prefix marking a variable in that file as a breakpoint |
| `globalScssFileNames` | `variables.scss`, `_variables.scss` | File names that produce the global theme object |
| `scssIncludePatterns` | `**/*.scss` | Glob patterns of `.scss` files to compile |
| `scssExcludePatterns` | `**/_*.scss` | Glob patterns to exclude (partials) |

### Adaptive runtime

| Option | Default | Meaning |
|---|---|---|
| `adaptiveStrategy` | `compositionLocal` | `compositionLocal` \| `windowInfo` \| `windowSizeClass` |

### Dimension guard

| Option | Default | Meaning |
|---|---|---|
| `allowedDimensionLiterals` | `0.dp`, `1.dp` | Dimension literals permitted in code |
| `guardExcludedPathPatterns` | *(empty)* | Path substrings exempt from the guard |
| `guardedTasks` | `compileKotlinDesktop`, `compileKotlinWasmJs` | Tasks that trigger the guard |

### Wiring

| Option | Default | Meaning |
|---|---|---|
| `kotlinSourceSetName` | `commonMain` | Source set the generated tokens are added to |
| `autoWireGeneratedSources` | `true` | Add generated sources to the KMP source set automatically |
| `generatedSourcesDirectory` | `build/generated/tokia` | Where generated Kotlin is written |

## Tasks

| Task | Purpose |
|---|---|
| `generateScssTokens` | Compile SCSS → Kotlin tokens (+ adaptive runtime) |
| `checkStrayDimensions` | Fail on hardcoded `.dp`/`.sp` literals in component code |
| `bootstrapTokiaSass` | Resolve/download the dart-sass binary |
| `validateTokiaOptions` | Fail if a required `tokia { }` option is unset |

## Full configuration (every option)

```kotlin
tokia {
    scssRootDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/acme/ui"))
    themePackage.set("com.acme.ui.styles")
    tokensPackage.set("com.acme.ui.styles.tokens")
    adaptivePackage.set("com.acme.ui.styles")
    
    dartSassVersion.set("1.86.3")                          // downloaded release
    downloadDartSass.set(true)                             // false = use dartSassExecutable
    dartSassExecutable.set(file("/path/to/dart-sass/sass"))// your own binary; skips the download
    dartSassDownloadBaseUrl.set("https://github.com/sass/dart-sass/releases/download")
    dartSassCacheDir.set(layout.buildDirectory.dir("tokia/dart-sass"))
    
    sassOutputStyle.set("expanded")                        // expanded | compressed
    sassGenerateSourceMaps.set(false)
    additionalSassLoadPaths.from(layout.projectDirectory.dir("src/commonMain/kotlin"))
    additionalSassArguments.add("--quiet-deps")
    
    scssColorsFile.set(layout.projectDirectory.file("src/commonMain/kotlin/com/acme/ui/styles/_colors.scss"))
    scssBreakpointsFile.set(layout.projectDirectory.file("src/commonMain/kotlin/com/acme/ui/styles/_breakpoints.scss"))
    breakpointVariablePrefix.set("bp-")                    // marks a var in the breakpoints file
    globalScssFileNames.set(setOf("variables.scss", "_variables.scss"))
    scssIncludePatterns.set(listOf("**/*.scss"))
    scssExcludePatterns.set(listOf("**/_*.scss"))
    
    adaptiveStrategy.set("compositionLocal")               // compositionLocal | windowInfo | windowSizeClass
    
    allowedDimensionLiterals.set(setOf("0.dp", "1.dp"))
    guardExcludedPathPatterns.set(listOf())                // path substrings to exempt (e.g. "SomeFile.kt")
    guardedTasks.set(listOf("compileKotlinDesktop", "compileKotlinWasmJs"))
    
    kotlinSourceSetName.set("commonMain")                  // source set the tokens are added to
    autoWireGeneratedSources.set(true)                     // false = wire them yourself
    generatedSourcesDirectory.set(layout.buildDirectory.dir("generated/tokia"))
}
```

Only the four **Required** options must be present; every other line above can be omitted to use its
default. See the [Options](#options) tables for the defaults and meanings.

## Documentation

- **Full guide** — examples, `@media`/adaptive strategies, the dimension guard, how it works, and
  building the plugin: [`docs/GUIDE.md`](docs/GUIDE.md)
- **Integration tests**: [`tests/README.md`](tests/README.md)

## License

[MIT](LICENSE)
