# Firebase Setup — One-time Steps Required

Before building, complete these steps in your Firebase Console:

---

## 1. Create a Firebase Project

1. Go to https://console.firebase.google.com
2. Click **"Add project"** → name it (e.g. `SunMoonResort`) → follow the wizard
3. Click **"Add Firebase to your Android app"**
   - Package name: `com.example.sunmoonresort`
   - Complete the registration flow
4. **Download `google-services.json`** and place it at:
   ```
   SunMoonResort/app/google-services.json
   ```
5. Sync Gradle in Android Studio (**File → Sync Project with Gradle Files**)

---

## 2. Enable Firestore

1. Firebase Console → **Firestore Database** → **Create database**
2. Choose **"Start in test mode"** (for development) or configure Security Rules for production
3. The app stores all bookings in a **single document**:
   - Collection : `hotel_bookings`
   - Document   : `bookings_data`
   - Field      : `bookings_json` — GSON-serialised `Map<RoomNumber, List<BookingDetails>>`

---

## 3. Toggle backend in local code

Open `app/src/main/java/com/example/sunmoonresort/data/BookingStoreConfig.kt`
and set the enum value, then **rebuild the app**:

```kotlin
val selectedBackend: StorageBackend = StorageBackend.FIREBASE
```

Available values:
- `StorageBackend.LOCAL` (SharedPreferences)
- `StorageBackend.FIREBASE` (Firestore)
- `StorageBackend.SUPABASE` (see `SUPABASE_MIGRATION.md`)

---

## 4. Loading Screen (remote backends only)

When backend is `FIREBASE` (or `SUPABASE`) the app shows a loading screen
(`MainActivity`) while async hydration completes:

- Displays the resort logo, a spinner, and **"Syncing bookings from cloud…"**
- Automatically navigates to `HomeActivity` once bookings are loaded
- **5-second safety timeout** — if remote load fails, app navigates anyway

When backend is `LOCAL` the loading screen is skipped entirely
(SharedPreferences is instant).

---

## 5. Firestore Security Rules (for production)

Replace the default test-mode rules with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /hotel_bookings/{document} {
      allow read, write: if request.auth != null;
    }
  }
}
```

---

## Architecture Overview

### Startup flow

```
App launch → SunMoonResortApp.onCreate()
  │
  ├─ AndroidThreeTen.init()
  ├─ BookingLocalStore.init()           ← always initialised (fallback)
  │
  ├─ StorageBackend.LOCAL
  │    → HotelData ← BookingLocalStore.loadBookings()  (sync)
  │
  ├─ StorageBackend.FIREBASE
  │    → FirebaseApp.initializeApp()
  │    → BookingFirebaseStore.loadBookingsAsync()
  │
  └─ StorageBackend.SUPABASE
       → BookingSupabaseStore.loadBookingsAsync()

MainActivity (launcher)
  │
  ├─ local backend   → navigates to HomeActivity immediately
  └─ remote backend  → shows loading screen + timeout fallback
```

### Save flow

```
BookingService.confirmBooking / cancelBooking / updateBookingStatus
  → BookingStoreManager.saveBookings(HotelData.bookings)
      ├─ LOCAL    → BookingLocalStore.saveBookings()
      ├─ FIREBASE → BookingFirebaseStore.saveBookings()
      └─ SUPABASE → BookingSupabaseStore.saveBookings()
```

### Search / Read flow

```
BookingService.searchBookingsByMobile / getAllBookingRecords / …
  → reads HotelData.bookings (always in-memory)
```

---

## New Files Added

| File | Purpose |
|------|---------|
| `data/BookingStore.kt` | Interface — contract for all store implementations |
| `data/BookingStoreConfig.kt` | Enum selector (`LOCAL` / `FIREBASE` / `SUPABASE`) |
| `data/BookingFirebaseStore.kt` | Firestore implementation of `BookingStore` |
| `data/BookingSupabaseStore.kt` | Supabase REST implementation of `BookingStore` |
| `data/BookingStoreManager.kt` | Router — delegates save/load to the active store |
| `res/layout/activity_main.xml` | Branded loading screen shown during remote fetch |

## Modified Files

| File | Change |
|------|--------|
| `data/BookingLocalStore.kt` | Implements `BookingStore`; `loadBookingsAsync` added |
| `data/service/BookingService.kt` | `BookingStoreManager.saveBookings()` used for all mutations |
| `SunMoonResortApp.kt` | Hydrates from selected backend; `onDataReady()` callback exposed |
| `MainActivity.kt` | Loading screen for remote backend + 5s timeout |
| `res/values/strings.xml` | Added `splash_loading` string |
| `gradle/libs.versions.toml` | Firebase BOM + Firestore + google-services plugin versions |
| `app/build.gradle` | Google services plugin + Firebase dependencies |
| `build.gradle` (root) | Google services plugin `apply false` |
