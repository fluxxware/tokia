# Tokia integration tests (Nix)

This is how the plugin proves its compatibility reach: **any KGP · JDK 8+ · Gradle 6.8.3+.**

## How a case runs

`tests/happy_path.sh` is a thin orchestrator. It resolves the shared env (`WORK`, `GRADLE_USER_HOME`
— done in the orchestrator because a child script can't export to its parent) and then runs these
standalone steps in order:

| Step | Does |
|---|---|
| `steps/10-wrapper.sh` | installs the Gradle wrapper, pinned to the case's Gradle |
| `steps/20-project.sh` | scaffolds the consumer project from `tests/fixtures/consumer/` |
| `steps/30-run.sh` | runs `./gradlew generateScssTokens` |
| `steps/40-assert.sh` | asserts `GeneratedThemeTokens.kt` contains the expected token |
| `steps/99-diagnose.sh` | on failure: prints the generated tree + dart-sass cache |

Shared helpers live in `tests/lib/common.sh`; the consumer project lives in
`tests/fixtures/consumer/`. Every step reads the exported env (`WORK`, `REPO_ROOT`, `GRADLE_USER_HOME`,
`TOKIA_TEST_*`, `JAVA_HOME`), so each can also be run on its own for debugging.

That run exercises: plugin apply, **KGP-agnostic source-set auto-wiring (reflection)**, the sass
bootstrap + compile, and the token pipeline.

## Run

```bash
tests/run-all.sh            # publishes the plugin to mavenLocal, then runs every case (fresh homes)
```
### Or
```bash
tests/run-all.sh --cached   # same, but reuse one shared Gradle home (fast, see below)
```

One case (flags forward through `nix-shell --run` only via env — pass `--cached` directly):

```bash
nix-shell tests/cases/06-jdk17-gradle-8.10-kotlin-2.1.0.nix --run 'bash tests/happy_path.sh --cached'
```

## Gradle home / caching

By default each case uses a **fresh temporary** `GRADLE_USER_HOME`, so it re-downloads its Gradle
distribution and dependencies every run (clean-room, slow).

`--cached` shares **one persistent** home so those download once and are reused across cases and runs:

- location: `$TOKIA_TEST_GRADLE_HOME`, else `${XDG_CACHE_HOME:-$HOME/.cache}/tokia-tests/gradle-home`
  (outside the repo — nothing to `.gitignore`).
- `TOKIA_TEST_CACHED=1` is equivalent to `--cached`.
- expect ~1–3 GB once populated; Gradle namespaces caches per version, so different Gradle/Kotlin
  versions do not cross-contaminate.

CI always runs `--cached` with an `actions/cache` step on that directory, so the matrix is fast there too.

## Cases

| # | JVM | Gradle | Kotlin (KGP) |
|---|-----|--------|--------------|
| 01 | 8  | 6.8.3 | 1.9.10 |
| 02 | 11 | 6.8.3 | 2.0.0 |
| 03 | 11 | 7.0   | 1.7.10 |
| 04 | 11 | 7.0   | 1.8.20 |
| 05 | 17 | 7.6.3 | 2.1.0 |
| 06 | 17 | 8.10  | 2.1.0 |
| 07 | 21 | 9.6   | 2.4.0 |

Add a case by dropping a new `cases/<name>.nix` that calls `tests/lib.nix`.

## Notes

- Network is required (nixpkgs tarball, Gradle distributions, KGP, dart-sass).
- The plugin must be in mavenLocal first (the plugin's own build runs on a modern Gradle; old Gradle
  only *consumes* the published artifact). `run-all.sh` publishes it.
