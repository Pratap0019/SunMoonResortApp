# Supabase Migration - Long-Term (Relational) Path

This project now includes a first migration step toward Supabase using a normalized schema.

## What is already implemented in code

- `BookingStoreConfig.StorageBackend` enum with:
  - `LOCAL`
  - `FIREBASE`
  - `SUPABASE`
- `BookingStoreManager` routing to `BookingSupabaseStore` when backend is `SUPABASE`.
- `BookingSupabaseStore` implementation with:
  - async load from Supabase REST (`bookings`, `booking_breakdown_items`)
  - snapshot save (replace-all write strategy)
  - mapping back to `BookingDetails` + `Bill` with ordered breakdown lines
- `SunMoonResortApp` + `MainActivity` updated for generic remote backend loading.

## 1) Create tables and policies in Supabase

1. Create a Supabase project.
2. Open SQL Editor.
3. Run `supabase/schema.sql`.

This creates:
- `public.bookings`
- `public.booking_breakdown_items`
- indexes + `updated_at` trigger + demo RLS policies.

## 2) Configure backend selector and credentials

Open `app/src/main/java/com/example/sunmoonresort/data/BookingStoreConfig.kt`.

Set:

```kotlin
val selectedBackend: StorageBackend = StorageBackend.SUPABASE
const val supabaseUrl: String = "https://YOUR_PROJECT_REF.supabase.co"
const val supabaseAnonKey: String = "YOUR_SUPABASE_ANON_KEY"
```

## 3) Run and verify

1. Start app.
2. Create a booking.
3. In Supabase Table Editor verify rows inserted into:
   - `bookings`
   - `booking_breakdown_items`
4. Restart app and verify booking search still works.

## Current write strategy

`BookingSupabaseStore` currently uses a full-snapshot replace strategy:
- delete all rows from both tables
- insert the latest in-memory snapshot

This is safe for single-admin usage but should be upgraded later to incremental upserts
for concurrent multi-user scenarios.

## Recommended next steps (phase 2)

1. Move Supabase credentials out of source code into `BuildConfig`/secrets.
2. Replace full-snapshot writes with diff-based upsert/delete by `booking_id`.
3. Add server-side RPC transaction for atomic save.
4. Tighten RLS policies per user/admin identity.

