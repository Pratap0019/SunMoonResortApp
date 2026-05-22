# SunMoon Resort - Agent Setup Guide

## Quick Context

**SunMoon Resort** is a single-module Android application built with modern Android development tools and libraries. It's a foundational project showcasing Material3 design with edge-to-edge display support.

- **Package**: `com.example.sunmoonresort`
- **Min/Target/Compile SDK**: 24 / 36 / 36
- **Language**: Kotlin 1.9+ (official code style)
- **Build Tool**: Gradle 9.2.1 with version catalogues

## Architecture Overview

### Multi-Activity + Layered Package Pattern
- **`SunMoonResortApp.kt`**: Custom Application class initializing AndroidThreeTen and data persistence on startup
- **`MainActivity.kt`**: Splash screen launcher activity that immediately transitions to `HomeActivity` and finishes
- **Core Activities**: 
  - `HomeActivity`: Main booking interface with carousel and room availability
  - `BookingActivity` / `BookingActivity_New`: Booking flow screens
  - `AdminLoginActivity`: Admin authentication screen
  - `AdminActivity`: Admin dashboard for managing bookings
- **Core package layers**:
  - **`ui/`**: Multiple activity screens and `adapter/` subfolder with RecyclerView adapters (Carousel, Booking, Room Inventory, Pricing, Admin)
  - **`data/`**: Data sources and business logic (`HotelData` singleton, `BookingLocalStore` for persistence, `service/` subfolder with `bookingService` and `AdminService`)
  - **`model/`**: Comprehensive domain models (Room, RoomType, BookingDetails, BookingRecord, BookingStatus, Guest, Pet, Bill, Extras)
- **View Binding**: Enabled in build.gradle (all activities use generated binding classes)

```
app/src/main/
├── java/com/example/sunmoonresort/
│   ├── SunMoonResortApp.kt               ← Custom Application class
│   ├── MainActivity.kt                   ← Splash screen to HomeActivity
│   ├── ui/
│   │   ├── HomeActivity.kt
│   │   ├── BookingActivity.kt
│   │   ├── AdminLoginActivity.kt
│   │   ├── AdminActivity.kt
│   │   └── adapter/                      ← RecyclerView adapters
│   │       ├── CarouselAdapter.kt
│   │       ├── BookingResultAdapter.kt
│   │       ├── RoomAvailabilityAdapter.kt
│   │       └── ... (PriceRowAdapter, BreakdownAdapter, etc.)
│   ├── data/
│   │   ├── HotelData.kt                  ← Singleton with room inventory, rates, bookings
│   │   ├── BookingLocalStore.kt          ← SharedPreferences persistence
│   │   ├── SunMoonResort.kt              ← Main data model/repository
│   │   └── service/
│   │       ├── BookingService.kt         ← Booking business logic
│   │       └── AdminService.kt           ← Admin business logic
│   └── model/                            ← Domain models
│       ├── Room.kt, RoomType.kt
│       ├── BookingDetails.kt, BookingRecord.kt, BookingStatus.kt
│       ├── Guest.kt, Pet.kt
│       ├── Bill.kt, Extras.kt
│       └── ... (other domain models)
├── res/
│   ├── layout/ (activity_home_new.xml, activity_booking.xml, etc.)
│   ├── drawable/ (includes logoicon for app icon)
│   ├── values/ (strings, colors, themes)
│   ├── values-night/ (dark mode themes)
│   └── mipmap-*/ (app icons in all densities)
└── AndroidManifest.xml (SunMoonResortApp as application class, multiple activities registered)
```

**Data Flow**: `SunMoonResortApp` initializes thyroid: `BookingLocalStore.loadBookings()` → `HotelData.replaceBookings()`. Activities interact with `HotelData` and `*Service` classes for business logic.

## Dependency Management

### Version Catalogue Pattern
All dependencies managed via **`gradle/libs.versions.toml`** (NOT build.gradle):
- Edit this file to upgrade versions
- Reference versions in `app/build.gradle` using `libs.` notation
- **Key UI libraries**: `androidx-core-ktx`, `androidx-appcompat`, `androidx-constraintlayout`, `material`, `androidx-activity-ktx`
- **Date/Time**: `threetenabp` (ThreeTen Backport for org.threeten.bp API on Android)
- **JSON serialization**: `gson`
- **Testing**: `junit`, `androidx-junit`, `androidx-espresso-core`

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

### View Binding
- **Enabled by default** in build.gradle (`viewBinding true`)
- Use generated binding classes in all activities (e.g., `ActivityHomeNewBinding`)
- Pattern: `binding = ActivityNameBinding.inflate(layoutInflater)` then `setContentView(binding.root)`
- Provides type-safe view access and prevents findViewById boilerplate

### Layout System
- **ConstraintLayout** (2.1.4): Preferred for complex UIs, provides responsive layouts
- Constraints used for positioning elements
- All activities use ConstraintLayout with generated binding classes

### Theme & Resources
- **Material3.DayNight.NoActionBar** base theme (light/dark mode automatic)
- String resources in `res/values/strings.xml` (app_name defined here)
- Color resources in `res/values/colors.xml` (extended with custom colors as needed)
- Night resources in `res/values-night/themes.xml` (for dark mode customization)

## Key File Responsibilities

| File | Purpose |
|------|---------|
| `gradle/libs.versions.toml` | Centralized dependency versions (edit here first) |
| `app/build.gradle` | Module build config, dependencies reference libs catalogue, viewBinding enabled |
| `app/src/main/AndroidManifest.xml` | App permissions, activities (MainActivity, HomeActivity, BookingActivity, AdminLoginActivity, AdminActivity), SunMoonResortApp as application class |
| `app/src/main/java/com/example/sunmoonresort/` | Kotlin source code root |
| `app/src/main/java/com/example/sunmoonresort/SunMoonResortApp.kt` | Custom Application class; initializes AndroidThreeTen and loads bookings from local store |
| `app/src/main/java/com/example/sunmoonresort/MainActivity.kt` | Splash/launcher activity that transitions to HomeActivity |
| `app/src/main/java/com/example/sunmoonresort/ui/` | UI layer: activities (HomeActivity, BookingActivity, etc.) and adapter subfolder |
| `app/src/main/java/com/example/sunmoonresort/ui/adapter/` | RecyclerView adapters (CarouselAdapter, BookingResultAdapter, RoomAvailabilityAdapter, PriceRowAdapter, BreakdownAdapter, AdminBookingsAdapter, RoomInventoryAdapter) |
| `app/src/main/java/com/example/sunmoonresort/data/` | Data layer: HotelData (singleton), BookingLocalStore (persistence), SunMoonResort (main model), service/ subfolder |
| `app/src/main/java/com/example/sunmoonresort/data/service/` | Business logic services (BookingService, AdminService) |
| `app/src/main/java/com/example/sunmoonresort/model/` | Domain models (Room, RoomType, BookingDetails, BookingRecord, BookingStatus, Guest, Pet, Bill, Extras) |
| `app/src/main/res/` | Layouts, strings, colors, drawables (including logoicon), themes, mipmaps |
| `proguard-rules.pro` | Obfuscation rules (currently unused - minifyEnabled false) |

## Common Tasks for Agents

1. **Add a new Activity**:
   - Create `NewActivity.kt` in `app/src/main/java/com/example/sunmoonresort/ui/`
   - Create layout file `activity_new.xml` in `app/src/main/res/layout/` using ConstraintLayout
   - Generate binding class by enabling view binding and using `ActivityNewBinding.inflate(layoutInflater)` in onCreate
   - Register in `AndroidManifest.xml` with appropriate `android:screenOrientation="portrait"` (preferred for this app)
   - Set `android:exported="false"` unless it's the launcher activity

2. **Add a RecyclerView Adapter**:
   - Create `NewAdapter.kt` in `app/src/main/java/com/example/sunmoonresort/ui/adapter/`
   - Follow existing adapter patterns (extend RecyclerView.Adapter<VH>, use binding for item layout inflation)
   - Place adapter item layout in `app/src/main/res/layout/adapter_item_*.xml`

3. **Add a Service (Business Logic)**:
   - Create `NewService.kt` in `app/src/main/java/com/example/sunmoonresort/data/service/`
   - Implement as a singleton or static object with methods operating on HotelData and other data sources
   - Example: BookingService handles booking validation, AdminService manages admin operations

4. **Add a Model Class**:
   - Create `NewModel.kt` in `app/src/main/java/com/example/sunmoonresort/model/`
   - Use Kotlin data classes for immutability and serialization (GSON compatible)

5. **Update Dependencies**:
   - Edit `gradle/libs.versions.toml` (add version entry under `[versions]` and library entry under `[libraries]`)
   - Reference in `app/build.gradle` using `libs.library.name`
   - Sync Gradle (IDE handles this)
   - **Important**: If adding threetenabp-related types, ensure `AndroidThreeTen.init()` is called in `SunMoonResortApp.onCreate()`

6. **Modify Theme**:
   - Edit `res/values/themes.xml` for light mode
   - Edit `res/values-night/themes.xml` for dark mode
   - Material3 provides sensible defaults; customize theme items as needed

7. **Persist Data**:
   - Use `BookingLocalStore` pattern for local persistence (SharedPreferences based)
   - Call `HotelData.replaceBookings()` to update in-memory state after loading from store
   - Bookings are stored per room number in `HotelData.bookings: MutableMap<Int, MutableList<BookingDetails>>`

8. **Add Strings/Resources**:
   - Always use resource references (`@string/name`, `@color/name`) not hardcoded values
   - Supports easy localization and testing
   - Drawable resources in `res/drawable/` (including vector drawables for scaling)

## Repository Structure

```
SunMoonResort/
├── gradle/libs.versions.toml              ← Edit dependencies here
├── gradle.properties                      ← JVM args, Kotlin style
├── app/
│   ├── build.gradle                       ← Module config, viewBinding enabled
│   ├── proguard-rules.pro                 ← Obfuscation (if minifyEnabled true)
│   └── src/
│       ├── main/
│       │   ├── java/com/example/sunmoonresort/
│       │   │   ├── SunMoonResortApp.kt    ← Custom Application, init AndroidThreeTen
│       │   │   ├── MainActivity.kt        ← Splash launcher
│       │   │   ├── ui/
│       │   │   │   ├── HomeActivity.kt
│       │   │   │   ├── BookingActivity.kt
│       │   │   │   ├── AdminLoginActivity.kt
│       │   │   │   ├── AdminActivity.kt
│       │   │   │   └── adapter/           ← RecyclerView adapters
│       │   │   ├── data/
│       │   │   │   ├── HotelData.kt       ← Singleton repository
│       │   │   │   ├── BookingLocalStore.kt
│       │   │   │   ├── SunMoonResort.kt
│       │   │   │   └── service/
│       │   │   │       ├── BookingService.kt
│       │   │   │       └── AdminService.kt
│       │   │   └── model/                 ← Domain models
│       │   │       ├── Room.kt, RoomType.kt
│       │   │       ├── BookingDetails.kt, BookingStatus.kt, etc.
│       │   ├── res/
│       │   │   ├── layout/                ← Activity and adapter item layouts
│       │   │   ├── drawable/              ← Including logoicon
│       │   │   ├── values/                ← Strings, colors, themes
│       │   │   ├── values-night/          ← Dark mode themes
│       │   │   ├── mipmap-*/              ← App icons in all densities
│       │   │   └── xml/                   ← System config (backup, extraction rules)
│       │   └── AndroidManifest.xml
│       ├── test/                          ← Unit tests (JUnit 4)
│       └── androidTest/                   ← Instrumented tests (Espresso)
```

## Integration Points

- **Android Framework**: Uses AppCompat for backward compatibility (min SDK 24)
- **Material Components**: Material3 library for modern UI
- **View Binding**: Type-safe automated layouts (viewBinding enabled in build.gradle)
- **Date/Time**: AndroidThreeTen (org.threeten.bp) initialized in SunMoonResortApp for timezone database
- **JSON Serialization**: GSON for serializing/deserializing domain models
- **Local Storage**: SharedPreferences via BookingLocalStore for booking persistence
- **RecyclerView**: Multiple adapters for lists, carousels, and inventory views
- **No External Services**: Currently standalone (room inventory and bookings managed in-memory with local store backup)

## Build & Debug Notes

- **Incremental Builds**: Gradle caches intelligently; clean only when necessary (`./gradlew clean`)
- **View Binding**: Generated classes auto-created during build; invalidate cache if binding classes not recognized
- **Minification**: Currently disabled (`minifyEnabled false` in build.gradle); enable before production release
- **Toolchain Resolution**: Uses Foojay resolver convention for automatic JDK resolution
- **Repository Order**: google() → mavenCentral() (order matters for artifact resolution)
- **AndroidThreeTen**: Requires app to call `AndroidThreeTen.init(context)` on app startup (done in SunMoonResortApp)
- **Portrait Orientation**: All UI activities locked to portrait in manifest; add `android:screenOrientation="portrait"` when creating new activities
- **View Binding Performance**: Generated binding classes can increase build times; disable if build is very slow (`viewBinding false` in build.gradle)

## Domain Model & Business Rules

### Core Entities
- **Room**: Hotel room with number, type (SINGLE/DOUBLE/SUITE), and amenities (WiFi free, optional paid amenities)
- **RoomType**: Enum with pricing (SINGLE: ₹2000, DOUBLE: ₹3500, SUITE: ₹5000 per night)
- **BookingDetails**: Complete booking with guest info, room number, dates, extras, and pets
- **BookingRecord**: Historical record of all bookings
- **BookingStatus**: Enum tracking booking states
- **Extras**: Available room add-ons (MATTRESS, SPA, GymPASS, PoolPASS; WiFi and MiniFridge are free)
- **Guest**: Guest information (name, contact, ID proof)
- **Pet**: Pet info with size category affecting fees (Small <8kg: ₹200, Medium <15kg: ₹350, Large >15kg: ₹500)
- **Bill**: Final bill with room charges, extras costs, pet fees, and total

### Data Architecture
- **HotelData** singleton holds:
  - `rooms`: 18 predefined rooms (3 floors × 6 rooms each)
  - `roomRates`: Fixed pricing by RoomType
  - `extrasRate`: Fixed pricing for paid amenities
  - `petFeeRates`: Pet fees by size category
  - `bookings`: In-memory MutableMap<RoomNumber, List<BookingDetails>>
- **BookingLocalStore**: Uses GSON to persist/load bookings to SharedPreferences (key: "bookings_key")
- **Services**: BookingService and AdminService provide business logic (validation, booking creation, etc.)

### Key Patterns
- **Singleton Repositories**: `HotelData` and `*Service` objects for shared state
- **View Binding**: All activities use generated binding classes for type-safe UI updates
- **Adapter Callbacks**: CarouselAdapter auto-slides every 5 seconds; adapters handle list updates
- **Local Persistence**: Bookings persisted on every change via BookingLocalStore; reloaded at app startup
