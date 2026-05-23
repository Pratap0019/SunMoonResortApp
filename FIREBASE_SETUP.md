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

## 3. Toggle the Store — local flag in code

Open `app/src/main/java/com/example/sunmoonresort/data/BookingStoreConfig.kt`
and change the single constant, then **rebuild the app**:

```kotlin
// ✅ Save & load from Firebase Firestore:
const val storeDataInFirebase: Boolean = true

// ✅ Save & load locally via SharedPreferences (default):
const val storeDataInFirebase: Boolean = false
```

> No Firebase Console changes needed to flip the switch — it is entirely code-side.

---

## 4. Loading Screen (Firebase path only)

When `storeDataInFirebase = true` the app shows a **branded loading screen**
(`MainActivity`) while it waits for Firestore to respond:

- Displays the resort logo, a spinner, and **"Syncing bookings from cloud…"**
- Automatically navigates to `HomeActivity` once bookings are loaded
- **5-second safety timeout** — if Firestore never responds the app navigates anyway,
  so users are never permanently stuck on the splash screen

When `storeDataInFirebase = false` the loading screen is skipped entirely
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
  ├─ FirebaseApp.initializeApp()        ← always initialised
  ├─ BookingLocalStore.init()           ← always initialised (fallback)
  │
  ├─ storeDataInFirebase = false
  │     └─ HotelData ← BookingLocalStore.loadBookings()  (sync, instant)
  │           └─ notifyDataReady()
  │
  └─ storeDataInFirebase = true
        └─ BookingFirebaseStore.loadBookingsAsync()
              └─ [Firestore responds] → HotelData.replaceBookings()
                    └─ notifyDataReady()

MainActivity (launcher)
  │
  ├─ storeDataInFirebase = false  →  navigates to HomeActivity immediately
  │
  └─ storeDataInFirebase = true
        └─ shows loading screen (logo + spinner)
              ├─ SunMoonResortApp.onDataReady { navigateToHome() }   ← normal path
              └─ 5-second timeout fallback    { navigateToHome() }   ← safety net
```

### Save flow (every booking operation)

```
BookingService.confirmBooking / cancelBooking / updateBookingStatus
  └─ BookingStoreManager.saveBookings(HotelData.bookings)
        ├─ storeDataInFirebase = false  →  BookingLocalStore.saveBookings()   (sync)
        └─ storeDataInFirebase = true   →  BookingFirebaseStore.saveBookings() (async, fire-and-forget)
```

### Search / Read flow

```
BookingService.searchBookingsByMobile / getAllBookingRecords / …
  └─ reads HotelData.bookings  (always in-memory, hydrated on startup from the active store)
```

---

## New Files Added

| File | Purpose |
|------|---------|
| `data/BookingStore.kt` | Interface — contract for all store implementations |
| `data/BookingStoreConfig.kt` | **Single `const val` flag** — `storeDataInFirebase: Boolean` |
| `data/BookingFirebaseStore.kt` | Firestore implementation of `BookingStore` |
| `data/BookingStoreManager.kt` | Router — delegates save/load to the active store |
| `res/layout/activity_main.xml` | Branded loading screen shown during Firestore fetch |

## Modified Files

| File | Change |
|------|--------|
| `data/BookingLocalStore.kt` | Now implements `BookingStore` interface; `loadBookingsAsync` added (calls sync result immediately) |
| `data/service/BookingService.kt` | All `BookingLocalStore.saveBookings()` → `BookingStoreManager.saveBookings()` |
| `SunMoonResortApp.kt` | Stores loaded via flag; `companion object` exposes `onDataReady()` callback |
| `MainActivity.kt` | Shows loading screen on Firebase path; registers `onDataReady` + 5s timeout |
| `res/values/strings.xml` | Added `splash_loading` string |
| `gradle/libs.versions.toml` | Added Firebase BOM + Firestore + google-services plugin |
| `app/build.gradle` | Applied google-services plugin; added Firebase dependencies |
| `build.gradle` (root) | Added google-services plugin `apply false` |
