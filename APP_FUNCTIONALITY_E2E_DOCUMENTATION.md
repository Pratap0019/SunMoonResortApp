# SunMoon Resort App - Functional Documentation (All Pages + E2E Scenarios)

## 1) Purpose and Scope
This document explains the current Android app functionality page-by-page, including:
- what each page is intended to do,
- what business logic it uses,
- and end-to-end (E2E) user scenarios.

It is based on the current implementation in:
- `app/src/main/java/com/example/sunmoonresort/`
- `app/src/main/res/layout/`
- `app/src/main/java/com/example/sunmoonresort/data/service/`

## 2) High-Level Architecture
The app follows a layered structure:
- **UI layer**: Activities + RecyclerView adapters (`ui/`)
- **Service layer**: core booking/admin logic (`data/service/`)
- **Model layer**: Room, Booking, Bill, Guest, Status (`model/`)
- **In-memory data**: rooms + bookings in `HotelData`

Important behavior:
- Bookings are stored in memory (`HotelData.bookings`), not persisted in DB.
- If app process restarts, in-memory bookings can reset.

## 3) Page Map (Screens)

### 3.1 `MainActivity` (`activity_main.xml`)
**Current role**:
- Launcher activity from `AndroidManifest.xml`.
- Shows a simple placeholder screen (`Hello World!`).

**Implementation**:
- `MainActivity.kt` uses edge-to-edge insets and sets `activity_main.xml`.

**Note**:
- This page currently does not navigate to Home directly.

---

### 3.2 Home Page - `HomeActivity` (`activity_home_new.xml`)
**Intended role**:
- Public landing page for guests.
- Shows resort intro, carousel, room type availability, pricing, booking search.

**Main functionality**:
1. **Admin entry**
   - Admin button opens `AdminLoginActivity`.
2. **Primary CTA**
   - "Book Stay" CTA opens `BookingActivity`.
3. **Carousel**
   - Three image slides with titles/descriptions and CTA actions.
4. **Room availability summary**
   - Uses `BookingService.buildRoomTypeSummary()`.
5. **Pricing sections**
   - Room rates, extras, and pet fees shown in lists.
6. **Search booking by mobile**
   - Validates 10-digit input.
   - Fetches bookings via `BookingService.searchBookingsByMobile()`.
   - Shows matching bookings.
7. **Guest-side cancellation**
   - Cancels only confirmed bookings tied to searched mobile using `BookingService.cancelBooking()`.

**User feedback**:
- Success/error via Snackbar.

---

### 3.3 Booking Page - `BookingActivity` (`activity_booking_new.xml`)
**Role**:
- End-to-end guest booking flow.

**Main functionality**:
1. **Date selection**
   - Material date pickers for check-in/check-out.
   - Validates check-out > check-in.
2. **Availability check**
   - Calls `BookingService.getRoomTypeAvailability(checkIn, checkOut)`.
   - Enables room type selection if available.
3. **Extras selection**
   - Chips for Mattress, SPA, Gym Pass, Pool Pass.
   - SPA sessions selector shown only when SPA selected.
4. **Price calculation**
   - Finds first available room of selected type.
   - Calculates bill via `BookingService.calculateBill(...)`.
   - Shows itemized breakdown using `BreakdownAdapter`.
5. **Guest details validation**
   - Name: letters/spaces only.
   - Mobile: 10 digits, starts with 6-9.
6. **Booking confirmation**
   - Calls `BookingService.confirmBooking(...)`.
   - Generates Booking ID (`UUID`).
   - Shows success card with booking summary.

**Business rules enforced**:
- No overlap for confirmed/check-in bookings in same room.
- Room assignment uses first available room by type.

---

### 3.4 Admin Login Page - `AdminLoginActivity` (`activity_admin_login.xml`)
**Role**:
- Protect admin area with password-based login.

**Main functionality**:
1. **Password input** (masked) with validation:
   - Required
   - Minimum 6 chars
2. **Authentication**
   - Uses `AdminService.verifyAdmin(password)`.
   - Current admin password in app: `Bhanu@0001`.
3. **Session behavior**
   - If already logged in, auto-navigates to admin panel.
4. **Error handling**
   - Invalid password shown inline.
   - Clears password field on failure.
5. **Navigation**
   - Back button closes screen.

**Session storage**:
- In-memory flag `isAdminLoggedIn` in `AdminService`.

---

### 3.5 Admin Bookings Page - `AdminActivity` (`activity_admin_bookings_new.xml`)
**Role**:
- Admin dashboard for booking operations and room inventory.

**Main functionality**:
1. **Guarded access**
   - Redirects to `AdminLoginActivity` if not logged in.
2. **Bookings list**
   - Loads all bookings via `BookingService.getAllBookingRecords()`.
   - Sorts by check-in date descending.
   - Uses `AdminBookingsAdapter`.
3. **Admin actions per booking**
   - **Check In** (for confirmed bookings)
   - **Check Out** (for checked-in bookings)
   - **Cancel** (for confirmed bookings)
   - **View Bill** (for checked-out bookings)
4. **Bill details dialog**
   - Uses `dialog_bill_details.xml` + `BreakdownAdapter`.
5. **Room inventory panel**
   - Shows all rooms and active booking ranges.
   - Uses `RoomInventoryAdapter`.
6. **Header actions**
   - **New Booking** -> opens `BookingActivity`
   - **Logout** -> clears admin session and returns to login
7. **Empty state**
   - Shows "No bookings yet" card when no bookings exist.

**Feedback**:
- Success/error messages via Snackbar.

## 4) Core Business Logic

### 4.1 Availability and overlap
`BookingService.isRoomAvailableForRange(...)` ensures room is not overlapping with:
- `CONFIRMED`
- `CHECKED_IN`
bookings.

### 4.2 Booking lifecycle
Main statuses (`BookingStatus`):
- `CONFIRMED`
- `CHECKED_IN`
- `CHECKED_OUT`
- `CANCELLED`

Transitions intended:
- Confirmed -> Checked In
- Checked In -> Checked Out
- Confirmed -> Cancelled

### 4.3 Guest cancellation constraints
Guest cancellation (from Home search results):
- Booking must belong to searched mobile number.
- Booking must be `CONFIRMED`.

### 4.4 Admin authentication
`AdminService` currently validates against local constant password:
- `ADMIN_PASSWORD = "Bhanu@0001"`

## 5) End-to-End Scenarios

## Scenario A: Guest makes a new booking
**Goal**: Complete booking with bill generation.

**Steps**:
1. Open Home page (`HomeActivity`) and tap Book Stay.
2. Select valid check-in and check-out dates.
3. Tap Check Availability.
4. Select a room type from available options.
5. Optionally choose extras and pet details.
6. Tap Calculate Price and review breakdown.
7. Enter guest name and mobile number.
8. Tap Confirm Booking.

**Expected result**:
- Booking ID is generated.
- Booking stored in `HotelData.bookings` with status `CONFIRMED`.
- Success confirmation is shown.

---

## Scenario B: Guest searches and cancels own booking
**Goal**: User self-service cancellation with validation.

**Steps**:
1. On Home page, enter 10-digit mobile in search section.
2. Tap Search Booking.
3. Review matching bookings list.
4. Tap Cancel on a booking.

**Expected result**:
- Cancel works only if booking is `CONFIRMED` and tied to searched number.
- Status changes to `CANCELLED`.
- List refreshes.
- If invalid, user sees explanatory error.

---

## Scenario C: Admin logs in and performs check-in/check-out
**Goal**: Admin manages booking lifecycle.

**Steps**:
1. Tap Admin from Home/Booking page.
2. Enter admin password (`Bhanu@0001`) in `AdminLoginActivity`.
3. Open bookings dashboard (`AdminActivity`).
4. For a confirmed booking, tap Check In.
5. For the same booking after check-in, tap Check Out.

**Expected result**:
- Booking status transitions accordingly.
- Snackbar confirms actions.
- List refresh reflects updated state.

---

## Scenario D: Unauthorized admin access prevention
**Goal**: Ensure admin screen requires login.

**Steps**:
1. Try opening `AdminActivity` without active admin session.

**Expected result**:
- User is redirected to `AdminLoginActivity`.

---

## Scenario E: Admin views bill details
**Goal**: View complete billing breakdown from admin panel.

**Steps**:
1. Log in as admin.
2. Open a booking where View Bill is available.
3. Tap View Bill.

**Expected result**:
- Dialog opens with guest info, room info, duration, breakdown items, total.

## 6) Current Integration Notes

### 6.1 Spring Boot alignment
- You shared Spring property: `admin.password=Bhanu@0001`.
- Android `AdminService` is aligned to same value.
- Current Android auth is local (no API call yet).

### 6.2 Entry-point and manifest notes
- Current launcher is `MainActivity` (placeholder Hello World).
- Feature activities (`HomeActivity`, `BookingActivity`) should be verified in manifest + navigation strategy for production UX.

### 6.3 Data persistence note
- Bookings are in-memory; process restart may clear runtime state.
- For production, move to persistent storage (server/database).

## 7) Recommended Next Improvements
1. Make `HomeActivity` the first real user-facing launch flow (or route from `MainActivity`).
2. Replace local admin password check with secure backend authentication.
3. Persist bookings in API/database instead of in-memory map.
4. Add automated tests for:
   - booking overlap validation,
   - status transitions,
   - guest cancellation constraints,
   - admin auth guard.
5. Localize hardcoded user-facing strings into `strings.xml` where needed.

---

## 8) Quick Functional Summary
- **MainActivity**: launcher placeholder.
- **HomeActivity**: showcase + availability + pricing + search/cancel + entry to booking/admin.
- **BookingActivity**: full booking funnel from date -> availability -> price -> confirm.
- **AdminLoginActivity**: secure password gate for admin area.
- **AdminActivity**: admin operations, inventory view, lifecycle actions, bill dialog.

This is the single-source functional document for current app behavior and E2E journeys.

