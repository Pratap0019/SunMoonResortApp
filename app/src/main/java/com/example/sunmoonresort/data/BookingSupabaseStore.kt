package com.example.sunmoonresort.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.sunmoonresort.model.Bill
import com.example.sunmoonresort.model.BookingDetails
import com.example.sunmoonresort.model.BookingStatus
import com.example.sunmoonresort.model.Guest
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Supabase implementation of [BookingStore] using PostgREST endpoints.
 *
 * Long-term schema (normalized):
 *  - bookings
 *  - booking_breakdown_items
 */
object BookingSupabaseStore : BookingStore {

    private const val TAG = "BookingSupabaseStore"

    private val gson = GsonFactory.instance
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun saveBookings(bookings: Map<Int, List<BookingDetails>>) {
        if (!isConfigured()) {
            Log.w(TAG, "Supabase config missing. Skip save.")
            return
        }

        // Keep booking flow non-blocking.
        ioExecutor.execute {
            try {
                val bookingRows = mutableListOf<SupabaseBookingRow>()
                val breakdownRows = mutableListOf<SupabaseBreakdownRow>()

                bookings.forEach { (roomNumber, roomBookings) ->
                    roomBookings.forEach { booking ->
                        bookingRows.add(
                            SupabaseBookingRow(
                                bookingId = booking.bookingId,
                                roomNumber = roomNumber,
                                guestName = booking.guest.name,
                                contactNumber = booking.guest.contactNumber,
                                daysStayed = booking.daysStayed,
                                checkInDate = booking.checkInDate,
                                checkOutDate = booking.checkOutDate,
                                status = booking.status.name,
                                totalAmount = booking.bill.totalAmount,
                            )
                        )

                        val orderedItems = booking.bill.breakdown.entries.toList()
                        orderedItems.forEachIndexed { index, entry ->
                            breakdownRows.add(
                                SupabaseBreakdownRow(
                                    bookingId = booking.bookingId,
                                    lineOrder = index,
                                    itemName = entry.key,
                                    calculation = booking.bill.calculationDetails[entry.key].orEmpty(),
                                    amount = entry.value,
                                )
                            )
                        }
                    }
                }

                // Full-snapshot replacement strategy.
                deleteAll("booking_breakdown_items", "id=not.is.null")
                deleteAll("bookings", "booking_id=not.is.null")

                if (bookingRows.isNotEmpty()) {
                    upsertRows("bookings", bookingRows)
                }
                if (breakdownRows.isNotEmpty()) {
                    upsertRows("booking_breakdown_items", breakdownRows)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save snapshot to Supabase", e)
            }
        }
    }

    override fun loadBookings(): MutableMap<Int, MutableList<BookingDetails>> = mutableMapOf()

    fun confirmBookingAtomic(
        roomNumber: Int,
        booking: BookingDetails,
        onComplete: (AtomicConfirmResult) -> Unit,
    ) {
        if (!isConfigured()) {
            postAtomicResult(onComplete, AtomicConfirmResult.Error("Supabase configuration is missing."))
            return
        }

        ioExecutor.execute {
            try {
                val orderedItems = booking.bill.breakdown.entries.toList()
                val request = ConfirmBookingRequest(
                    bookingId = booking.bookingId,
                    roomNumber = roomNumber,
                    guestName = booking.guest.name,
                    contactNumber = booking.guest.contactNumber,
                    daysStayed = booking.daysStayed,
                    checkInDate = booking.checkInDate,
                    checkOutDate = booking.checkOutDate,
                    status = booking.status.name,
                    totalAmount = booking.bill.totalAmount,
                    breakdownItems = orderedItems.mapIndexed { index, entry ->
                        ConfirmBreakdownRow(
                            lineOrder = index,
                            itemName = entry.key,
                            calculation = booking.bill.calculationDetails[entry.key].orEmpty(),
                            amount = entry.value,
                        )
                    },
                )

                val responseJson = postJson("rpc/confirm_booking_atomic", request)
                val response = gson.fromJson(responseJson, ConfirmBookingRpcResponse::class.java)
                when {
                    response?.ok == true -> {
                        postAtomicResult(onComplete, AtomicConfirmResult.Success(booking.bookingId))
                    }

                    response?.reason == "ROOM_ALREADY_BOOKED" -> {
                        postAtomicResult(onComplete, AtomicConfirmResult.RoomAlreadyBooked)
                    }

                    else -> {
                        val message = response?.message ?: response?.reason ?: "Booking confirmation failed"
                        postAtomicResult(onComplete, AtomicConfirmResult.Error(message))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Atomic booking confirm failed", e)
                postAtomicResult(onComplete, AtomicConfirmResult.Error(e.message ?: "Booking confirmation failed"))
            }
        }
    }

    override fun loadBookingsAsync(onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit) {
        if (!isConfigured()) {
            Log.w(TAG, "Supabase config missing. Returning empty map.")
            postResult(onComplete, mutableMapOf())
            return
        }

        ioExecutor.execute {
            try {
                val bookingJson = getJson(
                    "bookings?select=booking_id,room_number,guest_name,contact_number,days_stayed,check_in_date,check_out_date,status,total_amount"
                )
                val breakdownJson = getJson(
                    "booking_breakdown_items?select=booking_id,line_order,item_name,calculation,amount&order=line_order.asc"
                )

                val bookingType = object : TypeToken<List<SupabaseBookingRow>>() {}.type
                val breakdownType = object : TypeToken<List<SupabaseBreakdownRow>>() {}.type

                val bookings = gson.fromJson<List<SupabaseBookingRow>>(bookingJson, bookingType).orEmpty()
                val breakdownItems = gson.fromJson<List<SupabaseBreakdownRow>>(breakdownJson, breakdownType).orEmpty()

                val breakdownByBookingId = breakdownItems.groupBy { it.bookingId }
                val result = mutableMapOf<Int, MutableList<BookingDetails>>()

                bookings.forEach { row ->
                    val lines = breakdownByBookingId[row.bookingId].orEmpty().sortedBy { it.lineOrder }
                    val breakdownMap = linkedMapOf<String, Double>()
                    val calculationMap = linkedMapOf<String, String>()

                    lines.forEach { line ->
                        breakdownMap[line.itemName] = line.amount
                        if (line.calculation.isNotBlank()) {
                            calculationMap[line.itemName] = line.calculation
                        }
                    }

                    val details = BookingDetails(
                        bookingId = row.bookingId,
                        guest = Guest(name = row.guestName, contactNumber = row.contactNumber),
                        bill = Bill(
                            totalAmount = row.totalAmount,
                            breakdown = breakdownMap,
                            calculationDetails = calculationMap,
                        ),
                        daysStayed = row.daysStayed,
                        checkInDate = row.checkInDate,
                        checkOutDate = row.checkOutDate,
                        status = parseStatus(row.status),
                    )
                    result.computeIfAbsent(row.roomNumber) { mutableListOf() }.add(details)
                }

                postResult(onComplete, result)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bookings from Supabase", e)
                postResult(onComplete, mutableMapOf())
            }
        }
    }

    private fun parseStatus(raw: String): BookingStatus {
        return try {
            BookingStatus.valueOf(raw.uppercase())
        } catch (_: Exception) {
            BookingStatus.CONFIRMED
        }
    }

    private fun postResult(
        onComplete: (MutableMap<Int, MutableList<BookingDetails>>) -> Unit,
        result: MutableMap<Int, MutableList<BookingDetails>>,
    ) {
        mainHandler.post { onComplete(result) }
    }

    private fun postAtomicResult(
        onComplete: (AtomicConfirmResult) -> Unit,
        result: AtomicConfirmResult,
    ) {
        mainHandler.post { onComplete(result) }
    }

    private fun isConfigured(): Boolean {
        return BookingStoreConfig.supabaseUrl.isNotBlank() && BookingStoreConfig.supabaseAnonKey.isNotBlank()
    }

    private fun upsertRows(table: String, rows: Any) {
        val json = gson.toJson(rows)
        val conn = openConnection(path = table, method = "POST")
        conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal")
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(json) }
        ensureSuccess(conn)
        conn.disconnect()
    }

    private fun deleteAll(table: String, filter: String) {
        val conn = openConnection(path = "$table?$filter", method = "DELETE")
        conn.setRequestProperty("Prefer", "return=minimal")
        ensureSuccess(conn)
        conn.disconnect()
    }

    private fun getJson(path: String): String {
        val conn = openConnection(path = path, method = "GET")
        val response = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return response
    }

    private fun postJson(path: String, payload: Any): String {
        val conn = openConnection(path = path, method = "POST")
        conn.setRequestProperty("Prefer", "return=representation")
        conn.doOutput = true
        OutputStreamWriter(conn.outputStream).use { it.write(gson.toJson(payload)) }
        ensureSuccess(conn)
        val response = conn.inputStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()
        return response
    }

    private fun openConnection(path: String, method: String): HttpURLConnection {
        val baseUrl = BookingStoreConfig.supabaseUrl.trimEnd('/')
        val endpoint = "$baseUrl/rest/v1/$path"
        val conn = URL(endpoint).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("apikey", BookingStoreConfig.supabaseAnonKey)
        conn.setRequestProperty("Authorization", "Bearer ${BookingStoreConfig.supabaseAnonKey}")
        conn.setRequestProperty("Content-Type", "application/json")
        return conn
    }

    private fun ensureSuccess(conn: HttpURLConnection) {
        val code = conn.responseCode
        if (code in 200..299) return
        val errorBody = try {
            conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
        } catch (_: Exception) {
            ""
        }
        throw IllegalStateException("Supabase request failed: HTTP $code $errorBody")
    }

    private data class SupabaseBookingRow(
        @SerializedName("booking_id")
        val bookingId: String,
        @SerializedName("room_number")
        val roomNumber: Int,
        @SerializedName("guest_name")
        val guestName: String,
        @SerializedName("contact_number")
        val contactNumber: String,
        @SerializedName("days_stayed")
        val daysStayed: Int,
        @SerializedName("check_in_date")
        val checkInDate: String,
        @SerializedName("check_out_date")
        val checkOutDate: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("total_amount")
        val totalAmount: Double,
    )

    private data class SupabaseBreakdownRow(
        @SerializedName("booking_id")
        val bookingId: String,
        @SerializedName("line_order")
        val lineOrder: Int,
        @SerializedName("item_name")
        val itemName: String,
        @SerializedName("calculation")
        val calculation: String,
        @SerializedName("amount")
        val amount: Double,
    )

    private data class ConfirmBookingRequest(
        @SerializedName("booking_id")
        val bookingId: String,
        @SerializedName("room_number")
        val roomNumber: Int,
        @SerializedName("guest_name")
        val guestName: String,
        @SerializedName("contact_number")
        val contactNumber: String,
        @SerializedName("days_stayed")
        val daysStayed: Int,
        @SerializedName("check_in_date")
        val checkInDate: String,
        @SerializedName("check_out_date")
        val checkOutDate: String,
        @SerializedName("status")
        val status: String,
        @SerializedName("total_amount")
        val totalAmount: Double,
        @SerializedName("breakdown_items")
        val breakdownItems: List<ConfirmBreakdownRow>,
    )

    private data class ConfirmBreakdownRow(
        @SerializedName("line_order")
        val lineOrder: Int,
        @SerializedName("item_name")
        val itemName: String,
        @SerializedName("calculation")
        val calculation: String,
        @SerializedName("amount")
        val amount: Double,
    )

    private data class ConfirmBookingRpcResponse(
        @SerializedName("ok")
        val ok: Boolean? = null,
        @SerializedName("reason")
        val reason: String? = null,
        @SerializedName("message")
        val message: String? = null,
    )

    sealed class AtomicConfirmResult {
        data class Success(val bookingId: String) : AtomicConfirmResult()
        data object RoomAlreadyBooked : AtomicConfirmResult()
        data class Error(val message: String) : AtomicConfirmResult()
    }
}
