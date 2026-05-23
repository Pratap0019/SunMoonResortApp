# Supabase Data Flow Verification

This document confirms that when `BookingStoreConfig.selectedBackend = StorageBackend.SUPABASE`, all booking operations (create, search, admin view, bill) use Supabase as the source of truth.

---

## 1) App Startup

```
SunMoonResortApp.onCreate()
  ├─ BookingLocalStore.init(context)
  └─ selectedBackend == SUPABASE?
       └─ BookingSupabaseStore.loadBookingsAsync { bookings ->
            └─ HotelData.replaceBookings(bookings)
                 └─ HotelData.bookings now contains all Supabase bookings
```

**Result**: Entire in-memory booking cache is hydrated from Supabase REST tables.

---

## 2) Create Booking (confirmBooking)

```
BookingActivity UI
  └─ BookingService.confirmBooking(roomNumber, guestName, ..., bill)
       ├─ Creates BookingDetails object
       ├─ HotelData.bookings.put(roomNumber, bookingDetails)  ← in-memory update
       └─ BookingStoreManager.saveBookings(HotelData.bookings)
            └─ selectedBackend == SUPABASE?
                 └─ BookingSupabaseStore.saveBookings()
                      ├─ POST to bookings table  ← Supabase insert
                      └─ POST to booking_breakdown_items table  ← Supabase insert
```

**Result**: Booking saved to Supabase, in-memory cache updated.

---

## 3) Search Bookings

```
HomeActivity UI (search by mobile)
  └─ BookingService.searchBookingsByMobile(mobileNumber)
       └─ buildAllBookingRecords()
            └─ iterates HotelData.bookings  ← reads in-memory (hydrated from Supabase)
                 └─ returns List<BookingRecord>
```

**Result**: Search returns bookings loaded from Supabase at startup.

---

## 4) Admin View - List All Bookings

```
AdminActivity.onCreate()
  └─ BookingService.getAllBookingRecords()
       └─ buildAllBookingRecords()
            └─ iterates HotelData.bookings  ← reads in-memory (hydrated from Supabase)
                 └─ populates AdminBookingsAdapter
```

**Result**: Admin list shows all bookings from Supabase.

---

## 5) Admin View - Bill Details

```
AdminActivity.showBillDetails(bookingRecord)
  ├─ bookingRecord.bill  ← contains breakdown + calculationDetails (LinkedHashMap with insertion-order preserved)
  │
  └─ BreakdownAdapter(breakdownItems)
       └─ displays breakdown in correct order (Room Charge → GST)
```

**Data path**:
```
Supabase booking_breakdown_items (ordered by line_order)
  └─ loaded via BookingSupabaseStore.loadBookingsAsync()
       └─ mapped to Bill.breakdown (LinkedHashMap)
            └─ HotelData.bookings stores BookingDetails with Bill
                 └─ AdminActivity reads from BookingRecord
                      └─ BreakdownAdapter displays with correct order
```

**Result**: Bill breakdown shows prices + calculations in correct order (Supabase → app → UI).

---

## 6) Cancel/Update Booking

```
AdminActivity / HomeActivity
  └─ BookingService.cancelBooking() or updateBookingStatus()
       ├─ HotelData.bookings[bookingId].status = NEW_STATUS  ← in-memory update
       └─ BookingStoreManager.saveBookings(HotelData.bookings)
            └─ BookingSupabaseStore.saveBookings()
                 └─ Full snapshot re-write to Supabase tables
```

**Result**: Status changes persisted to Supabase.

---

## 7) App Restart

```
App killed and restarted
  └─ SunMoonResortApp.onCreate()
       └─ selectedBackend == SUPABASE?
            └─ BookingSupabaseStore.loadBookingsAsync()
                 └─ reads current rows from Supabase
                      └─ HotelData.replaceBookings()
                           └─ latest bookings available in admin / search
```

**Result**: All data persisted across app restarts via Supabase.

---

## 8) Bill Breakdown Order (Key Fix)

Core issue resolved by `GsonFactory`:
- Supabase stores breakdown items in `booking_breakdown_items` table with `line_order` column
- On load, `BookingSupabaseStore` sorts by `line_order` and rebuilds `Bill.breakdown` as `LinkedHashMap`
- `LinkedHashMap` preserves insertion order:
  ```
  Room Charge (line_order=0)
  Mattress (line_order=1)
  Pet Fee (line_order=2)
  Service Charge (line_order=3)
  Subtotal (line_order=4)
  GST (line_order=5)
  ```
- Admin bill view displays in correct order ✓

---

## 9) Verification Checklist

After enabling Supabase backend and providing credentials:

- [ ] App starts and loads splash screen (5 seconds max)
- [ ] Navigates to HomeActivity showing no errors
- [ ] Create booking from BookingActivity
- [ ] Check Supabase Table Editor → bookings table has 1 row
- [ ] Check Supabase Table Editor → booking_breakdown_items table has 6+ rows (breakdown items)
- [ ] Go to AdminActivity → "Ongoing Bookings" shows your booking
- [ ] Click "View Bill" → breakdown displays in correct order (Room Charge first, GST last)
- [ ] Search booking by mobile → finds the booking
- [ ] Kill and restart app
- [ ] Admin view still shows the booking (confirms Supabase load works)
- [ ] Cancel booking → Supabase row status updated

---

## 10) Data Consistency Guarantees

- **Single source of truth**: Supabase tables
- **In-memory replica**: `HotelData.bookings` (hydrated at startup from Supabase)
- **Write strategy**: Full-snapshot replace (safe for single-admin usage)
- **Read strategy**: All queries read from in-memory cache (fast, no network on every search)
- **Sync point**: Only on app restart or explicit write

---

## 11) Next Steps (Hardening)

1. Move credentials to `local.properties` + `BuildConfig` (not in source)
2. Replace full-snapshot writes with incremental upsert/delete by `booking_id`
3. Add server-side transaction RPC for atomic multi-table save
4. Tighten RLS policies for authenticated access (currently demo-permissive)
5. Add retry logic + conflict resolution for offline scenarios

