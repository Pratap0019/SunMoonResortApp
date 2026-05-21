# SunMoon Resort - Agent Setup Guide

## Quick Context

**SunMoon Resort** is a single-module Android application built with modern Android development tools and libraries. It's a foundational project showcasing Material3 design with edge-to-edge display support.

- **Package**: `com.example.sunmoonresort`
- **Min/Target/Compile SDK**: 24 / 36 / 36
- **Language**: Kotlin 1.9+ (official code style)
- **Build Tool**: Gradle 9.2.1 with version catalogues

## Architecture Overview

### Single Activity + Layered Package Pattern
- **`MainActivity.kt`**: Current launcher activity, extends `AppCompatActivity`
- Entry point uses `enableEdgeToEdge()` for modern full-screen display
- Core package folders prepared for growth:
  - **`ui/`**: UI screens, adapters, view-related classes
  - **`data/`**: Repositories, data sources, mappers
  - **`model/`**: Domain/data models used across layers
- Layout: `activity_main.xml` - ConstraintLayout with centered TextView

```
app/src/main/
├── java/com/example/sunmoonresort/
│   ├── MainActivity.kt
│   ├── ui/
│   ├── data/
│   └── model/
├── res/layout/activity_main.xml (ConstraintLayout root)
├── res/values/ (strings, colors, themes)
└── AndroidManifest.xml (MainActivity is LAUNCHER)
```

**Why**: Keep the app simple with one activity while establishing clear package boundaries for future features.

## Dependency Management

### Version Catalogue Pattern
All dependencies managed via **`gradle/libs.versions.toml`** (NOT build.gradle):
- Edit this file to upgrade versions
- Reference versions in `app/build.gradle` using `libs.` notation
- Key libs: `androidx-core-ktx`, `androidx-appcompat`, `androidx-constraintlayout`, `material`

### Plugin Configuration
Android application plugin via version catalogue:
```groovy
plugins {
    alias(libs.plugins.android.application)
}
```

## Developer Workflows

### Building & Running
```bash
# Development build (from project root)
./gradlew build

# Install to emulator/device
./gradlew assembleDebug

# Build release (minification disabled currently)
./gradlew assembleRelease
```

### Testing
- **Unit Tests**: `app/src/test/java/` - JUnit 4 (run via `./gradlew test`)
- **Instrumented Tests**: `app/src/androidTest/java/` - AndroidJUnitRunner + Espresso (run via `./gradlew connectedAndroidTest`)
- Example test classes provided: `ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`

### Gradle Configuration
- **JVM Memory**: Set to 2048m in `gradle.properties` (adjust if build slowness occurs)
- **Source Compatibility**: Java 11
- **Parallel Gradle**: Currently disabled; enable in `gradle.properties` for multi-module projects

## Code Conventions

### Kotlin Style
```kotlin
// Official Kotlin code style enforced (gradle.properties)
// ✓ Do: Use modern Kotlin (coroutines, extensions, data classes)
// ✗ Don't: Java patterns (null checks, manual resource management)
```

### Layout System
- **ConstraintLayout** (2.1.4): Preferred for complex UIs, provides responsive layouts
- Constraints used for positioning elements (see `activity_main.xml` - centered TextView pattern)

### Theme & Resources
- **Material3.DayNight.NoActionBar** base theme (light/dark mode automatic)
- String resources in `res/values/strings.xml` (app_name defined here)
- Color resources in `res/values/colors.xml` (extended with custom colors as needed)
- Night resources in `res/values-night/themes.xml` (for dark mode customization)

## Key File Responsibilities

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Centralized dependency versions (edit here first) |
| `app/build.gradle` | Module build config, dependencies reference libs catalogue |
| `app/src/main/AndroidManifest.xml` | App permissions, activities, configurations |
| `app/src/main/java/com/example/sunmoonresort/` | Kotlin source code |
| `app/src/main/java/com/example/sunmoonresort/ui/` | UI layer classes (screens, presentation helpers) |
| `app/src/main/java/com/example/sunmoonresort/data/` | Data layer classes (repositories, sources) |
| `app/src/main/java/com/example/sunmoonresort/model/` | Model classes shared across app layers |
| `app/src/main/res/` | Layouts, strings, colors, drawables, themes |
| `proguard-rules.pro` | Obfuscation rules (currently unused - minifyEnabled false) |

## Common Tasks for Agents

1. **Add a new Activity**:
   - Create `NewActivity.kt` in `app/src/main/java/com/example/sunmoonresort/`
   - Create layout in `app/src/main/res/layout/activity_new.xml` (ConstraintLayout)
   - Register in `AndroidManifest.xml`

2. **Update Dependencies**:
   - Edit `gradle/libs.versions.toml` (version + library entries)
   - Reference in `app/build.gradle` using `libs.library.name`
   - Sync Gradle (IDE handles this)

3. **Modify Theme**:
   - Edit `res/values/themes.xml` for light mode
   - Edit `res/values-night/themes.xml` for dark mode
   - Material3 provides sensible defaults; customize color items

4. **Add Strings/Resources**:
   - Always use resource references (`@string/name`, `@color/name`) not hardcoded values
   - Supports easy localization and testing

5. **Add New Classes by Layer**:
   - Place UI-related classes in `app/src/main/java/com/example/sunmoonresort/ui/`
   - Place repository/data source classes in `app/src/main/java/com/example/sunmoonresort/data/`
   - Place shared entities/models in `app/src/main/java/com/example/sunmoonresort/model/`

## Repository Structure

```
SunMoonResort/
├── gradle/libs.versions.toml    ← Edit dependencies here
├── app/
│   ├── build.gradle              ← Module config
│   ├── proguard-rules.pro         ← Obfuscation (if minifyEnabled true)
│   └── src/
│       ├── main/
│       │   ├── java/com/example/sunmoonresort/
│       │   │   ├── MainActivity.kt
│       │   │   ├── ui/
│       │   │   ├── data/
│       │   │   └── model/
│       │   └── res/
│       │       ├── layout/
│       │       └── values/
│       ├── test/                  ← Unit tests (JUnit)
│       └── androidTest/           ← Instrumented tests (Espresso)
└── gradle.properties              ← JVM args, Kotlin style
```

## Integration Points

- **Android Framework**: Uses AppCompat for backward compatibility (min SDK 24)
- **Material Components**: Material3 library for modern UI
- **EdgeToEdge API**: System bar padding handling via `ViewCompat.setOnApplyWindowInsetsListener`
- **No External Services**: Currently standalone; ready for network/database integration

## Build & Debug Notes

- **Incremental Builds**: Gradle caches intelligently; clean only when necessary (`./gradlew clean`)
- **Minification**: Currently disabled (`minifyEnabled false` in build.gradle); enable before production release
- **Toolchain Resolution**: Uses Foojay resolver convention for automatic JDK resolution
- **Repository Order**: google() → mavenCentral() (order matters for artifact resolution)

