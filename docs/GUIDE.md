# Tokia — Guide

> [← Back to README](../README.md)

## What it does

For every `.scss` file under `scssRootDir` (except partials starting with `_`), Tokia generates a
Kotlin object:

```
src/commonMain/kotlin/com/exampled/ui/            build/generated/tokia/...
├── button/Button.scss        ─────────────▶  com/exampled/ui/styles/tokens/ButtonTokens.kt
├── modal/ModalFrame.scss     ─────────────▶  com/exampled/ui/styles/tokens/ModalFrameTokens.kt
└── styles/variables.scss     ─────────────▶  com/exampled/ui/styles/GeneratedThemeTokens.kt   (the global object)
```

The global theme file (by default `variables.scss`) becomes `GeneratedThemeTokens`; every other file
becomes `<Component>Tokens`. Generated names are **fixed**, so the generated API is stable across
projects.

### Example

```scss
// styles/variables.scss
@forward 'colors';
$space-sm: 9px;
$control-height: 36px;
```

```scss
// button/Button.scss
@use 'styles/variables' as *;
.button {
  height: $control-height;
  border-radius: 6px;
}
$label-gap: $space-sm;
```

```kotlin
// generated: com/exampled/ui/styles/GeneratedThemeTokens.kt
object GeneratedThemeTokens {
    val SpaceSm = 9.dp
    val ControlHeight = 36.dp
}

// generated: com/exampled/ui/styles/tokens/ButtonTokens.kt
object ButtonTokens {
    val ButtonHeight = 36.dp        // from the .button { height } rule
    val ButtonBorderRadius = 6.dp
    val LabelGap = 9.dp             // from $label-gap
}
```

```kotlin
import com.exampled.ui.styles.tokens.ButtonTokens

Button(modifier = Modifier.height(ButtonTokens.ButtonHeight))
```

### How it works

```
.scss ──▶ dart-sass ──▶ CSS ──▶ parser ──▶ {Name}Tokens.kt ──▶ your Compose code
          (resolves       (resolved   (exports   (Kotlin objects)
           @use/@forward,  values)     + rules)
           mixins,
           functions, media)
```

- **dart-sass** does the real compilation, so `darken()`, `mix()`, `@forward`, `@mixin`, `$gap * 2`
  all resolve to concrete values before Tokia reads them.
- Values become Kotlin: `#RRGGBB` → `Color`, `Npx` → `N.dp`, `Nsp` → `N.sp`, `0.82` → `Number`/`Double`.
- Only single-value properties are extracted (multi-value like `padding: 0 8px` is skipped).

---

## Adaptive tokens (`@media`)

If a component's `.scss` declares variables inside `@media (min|max-width: …)`, those members become
**adaptive**: Tokia emits a `@Composable` getter that returns the right value for the current window
width, using breakpoints declared in `scssBreakpointsFile`.

```scss
// styles/_breakpoints.scss
$bp-compact: 600px;
$bp-medium: 840px;
```

```scss
// modal/ModalFrame.scss
$modal-width: 320px;
@media (min-width: $bp-compact) { $modal-width: 440px; }
@media (min-width: $bp-medium)  { $modal-width: 560px; }
```

```kotlin
object ModalFrameTokens {
    val ModalWidth: Dp @Composable @ReadOnlyComposable get() = /* 320 | 440 | 560 .dp */
}
```

Because adaptive members are `@Composable`, read them **inside composition** (as an argument or in the
body) — never as a default parameter value.

How the getter obtains the window width is chosen by `adaptiveStrategy` — see
[Connecting Tokia](#connecting-tokia). Adaptive output is generated **only** when a `.scss` file
actually has `@media` variants *and* a breakpoints file exists.

---

## Requirements

- **Gradle 6.8.3+** — the plugin is compiled against the Gradle 6.8.3 API and verified on
  6.8.3 / 7.0 / 7.6.3 / 8.10 / 9.6 (see `../tests`).
- **JVM 8+** — the plugin jar targets JVM 8 bytecode.
- **Any Kotlin/KGP version** — Tokia never links against the Kotlin Gradle Plugin; it reaches the KMP
  source sets by reflection.
- **Kotlin Multiplatform + Compose Multiplatform** in the consuming module (the generated code imports
  `androidx.compose.*`).
- A **dart-sass** binary: downloaded automatically by default, or point at your own (see
  `dartSassExecutable`) — useful for offline/Nix builds.

---

## Install

```kotlin
// build.gradle.kts
plugins {
    id("io.github.fluxxware.tokia") version "0.1.0"
}
```

Resolution: the plugin is a normal Gradle plugin. Add `gradlePluginPortal()` (and, while developing,
`mavenLocal()` after running `./gradlew publishToMavenLocal` in this repo) to `pluginManagement`
repositories in `../settings.gradle.kts`:

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
// build.gradle.kts
plugins {
    id("io.github.fluxxware.tokia") version "0.1.0"
}

tokia {
    // The only four options without a default. The build fails if any is missing.
    scssRootDir.set(layout.projectDirectory.dir("src/commonMain/kotlin/com/exampled/ui"))
    themePackage.set("com.exampled.ui.styles")
    tokensPackage.set("com.exampled.ui.styles.tokens")
    adaptivePackage.set("com.exampled.ui.styles")

    scssBreakpointsFile.set(layout.projectDirectory.file("src/commonMain/kotlin/com/exampled/ui/styles/_breakpoints.scss"))
}
```

The generated sources are added to `commonMain` automatically; nothing else is required.

---

## Connecting Tokia

**Build-time:** Tokia is applied with `plugins { id("io.github.fluxxware.tokia") }` and wires the generated Kotlin
into the configured source set (`kotlinSourceSetName`, default `commonMain`).

**Runtime (adaptive tokens only):** `adaptiveStrategy` selects how the generated adaptive getters
obtain the window width. Plain token objects (`GeneratedThemeTokens`, `<Component>Tokens`) are
ordinary values and work anywhere — the strategy affects **only** adaptive (`@media`) members.

| `adaptiveStrategy` | Getter reads | Wrapper needed | Extra dependency |
|---|---|---|---|
| `compositionLocal` *(default)* | generated `LocalTokiaAdaptive` | **Yes** — wrap each window in `TokiaAdaptiveScope { }` | — |
| `windowInfo` | `LocalWindowInfo.current.containerSize` | **No** | — |
| `windowSizeClass` | `currentWindowAdaptiveInfo().windowSizeClass` | **No** | `org.jetbrains.compose.material3.adaptive:adaptive` (1.0.1 for Compose 1.7.x; 1.1.x needs Compose 1.8.x) |

> The strategy that matters is the one of the module that **owns the `@media` SCSS** (the module that
> generates the adaptive tokens). Consumers of those tokens inherit the requirement.

### `compositionLocal` (default) — wrap each window

```scss
// in the owning module's .scss
$modal-width: 320px;
@media (min-width: 600px) { $modal-width: 440px; }
```

```kotlin
import com.exampled.ui.styles.TokiaAdaptiveScope

Window(...) {
    Theme {
        TokiaAdaptiveScope {          // provides LocalTokiaAdaptive for THIS window
            App()
        }
    }
}
```

Each `Window { }` is its own composition, so the scope goes **inside** every window's content. The
wrapper is required only when adaptive tokens exist.

### `windowInfo` — no wrapper, no extra dependency

```kotlin
tokia { /* … */ adaptiveStrategy.set("windowInfo") }
```

No `TokiaAdaptiveScope`/`TokiaAdaptive.kt` is generated; each adaptive getter reads the platform's
window info directly and keeps your arbitrary SCSS breakpoints:

```kotlin
val ModalWidth: Dp
    @Composable @ReadOnlyComposable
    get() {
        val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        return when {
            width <= 599.dp -> 320.dp
            width <= 839.dp -> 440.dp
            else -> 560.dp
        }
    }
```

```kotlin
Window(...) { Theme { App() } }   // no scope
```

### `windowSizeClass` — no wrapper, Material3 adaptive

```kotlin
dependencies {
    implementation("org.jetbrains.compose.material3.adaptive:adaptive:1.0.1")
}
```

```kotlin
tokia { /* … */ adaptiveStrategy.set("windowSizeClass") }
```

```kotlin
val ModalWidth: Dp
    @Composable
    get() = when (currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass) {
        WindowWidthSizeClass.COMPACT -> 320.dp
        WindowWidthSizeClass.MEDIUM -> 440.dp
        WindowWidthSizeClass.EXPANDED -> 560.dp
        else -> 560.dp
    }
```

SCSS breakpoints are sampled at the platform thresholds (COMPACT 0, MEDIUM 600, EXPANDED 840 dp), so
align `$bp-*` with those bounds.

---

## Dimension guard

`checkStrayDimensions` scans the `.kt` files under `scssRootDir` and **fails the build** if it finds a
`.dp`/`.sp` literal that isn't in `allowedDimensionLiterals` (default `0.dp`, `1.dp`). It runs before
each task in `guardedTasks` (default `compileKotlinDesktop`, `compileKotlinWasmJs`).

```
Dimension literals found in component code. Add a $var to the component's .scss and reference the generated token instead.
source/.../NameCell.kt:20:    .padding(horizontal = 12.dp)
```

Use `guardExcludedPathPatterns` to exempt files whose path contains a given substring.

---

## Configuration reference

### Required

| Option | Type | Meaning |
|---|---|---|
| `scssRootDir` | `Directory` | Directory containing the component `.scss` files (searched recursively) |
| `themePackage` | `String` | Package of the global theme object |
| `tokensPackage` | `String` | Package of the per-component token objects |
| `adaptivePackage` | `String` | Package of the generated adaptive runtime (where `TokiaAdaptiveScope` lives) |

### Sass toolchain

| Option | Default | Meaning |
|---|---|---|
| `dartSassVersion` | `1.86.3` | dart-sass release to download |
| `downloadDartSass` | `true` | Auto-download dart-sass |
| `dartSassExecutable` | *(unset)* | Path to your own dart-sass; skips the download |
| `dartSassDownloadBaseUrl` | GitHub releases | Base URL hosting dart-sass assets |
| `dartSassCacheDir` | `build/tokia/dart-sass` | Where a downloaded binary is cached |

### Sass compilation

| Option | Default | Meaning |
|---|---|---|
| `sassOutputStyle` | `expanded` | dart-sass `--style`: `expanded` or `compressed` |
| `sassGenerateSourceMaps` | `false` | Emit source maps (`false` → `--no-source-map`) |
| `additionalSassLoadPaths` | *(empty)* | Extra `@use`/`@import` resolution dirs, on top of `scssRootDir` and its `styles/` |
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
| `adaptiveStrategy` | `compositionLocal` | `compositionLocal` \| `windowInfo` \| `windowSizeClass` (see above) |

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
| `autoWireGeneratedSources` | `true` | Add the generated sources to the KMP source set automatically (KGP-agnostic, via reflection). Set `false` to wire them yourself |
| `generatedSourcesDirectory` | `build/generated/tokia` | Where generated Kotlin is written |

---

## Tasks

| Task | Runs | Purpose |
|---|---|---|
| `bootstrapTokiaSass` | on demand | Resolve or download the dart-sass binary |
| `validateTokiaOptions` | before generation/guard | Fail with a list of missing required `tokia { }` options |
| `generateScssTokens` | wired into the source set | Compile SCSS → Kotlin tokens (+ adaptive runtime) |
| `checkStrayDimensions` | before each guarded compile task | Reject hardcoded `.dp`/`.sp` literals |

A `.scss` compile error **fails the build** (`ScssCompileException`) — Tokia never emits partial tokens
with a green build.

---

## Building this plugin

The published plugin targets **JVM 8** bytecode, so it runs on any JDK 8+ Gradle daemon. *Building
this repo* runs on JDK 17 with the Gradle 8.10 wrapper and Kotlin 2.1.0.

```bash
# NixOS (shell.nix pins JDK 17 and makes the downloaded dart-sass runnable):
nix-shell --run './gradlew build'

# Otherwise, run Gradle with JDK 17, e.g.:
JAVA_HOME=/path/to/jdk17 ./gradlew build
# …or set org.gradle.java.home in ~/.gradle/gradle.properties
```

```bash
./gradlew test                 # in-JVM unit + TestKit behavior tests
./gradlew publishToMavenLocal  # install locally for a consumer to try
./gradlew publishPlugins       # release to the Gradle Plugin Portal
```

### Integration tests (Nix)

The dependency ig test (JVM × Gradle × Kotlin versions mix) lives in `../tests` as Nix cases:

```bash
tests/run-all.sh            # publishes to mavenLocal, then runs every case
tests/run-all.sh --cached   # reuse a shared Gradle home (fast)
```

See [`../tests/README.md`](../tests/README.md).


---

## What is currently limiting

- Extraction is best-effort. **Skipped:** multi-line `$var:` definitions, namespaced `@use 'x' as ns`
  (`ns.$var`), multi-value properties (`padding: 0 8px`), `calc(…)` (contains spaces).
- The generated Kotlin is **Compose-only** (imports `androidx.compose.*`).
- Downloading dart-sass requires network unless `dartSassExecutable` (or `downloadDartSass=false`) is set.

---

## License

[MIT](../LICENSE) © 2026 Tokia
