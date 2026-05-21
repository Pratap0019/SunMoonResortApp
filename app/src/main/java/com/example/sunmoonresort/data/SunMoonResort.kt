package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.Bill
import com.example.sunmoonresort.model.Extras
import com.example.sunmoonresort.model.Room
import java.util.LinkedHashMap

object SunMoonResort {

    fun calculateBill(
        roomNumber: Int,
        daysStayed: Int,
        selectedExtras: List<Extras>?,
        petWeight: Double?,
    ): Bill = calculateBill(
        roomNumber = roomNumber,
        daysStayed = daysStayed,
        selectedExtras = selectedExtras,
        petWeight = petWeight,
        spaSessions = null,
    )

    fun calculateBill(
        roomNumber: Int,
        daysStayed: Int,
        selectedExtras: List<Extras>?,
        petWeight: Double?,
        spaSessions: Int?,
    ): Bill {
        require(daysStayed > 0) { "Days stayed must be greater than 0." }

        val room = getRoomDetails(roomNumber)
        val baseRate = HotelData.getRoomRate(room.roomType)
        val breakdown = LinkedHashMap<String, Double>()
        val calculationDetails = LinkedHashMap<String, String>()

        val roomCharge = baseRate * daysStayed
        breakdown["Room Charge"] = roomCharge
        calculationDetails["Room Charge"] = "₹%.2f/day × %d days".format(baseRate, daysStayed)

        var extrasCharge = 0.0
        selectedExtras.orEmpty().forEach { extra ->

            if (extra == Extras.SPA) {
                val sessions = if (spaSessions != null && spaSessions >= 1) spaSessions else 1
                val ratePerSession = HotelData.getExtraRate(extra)
                val extraCharge = ratePerSession * sessions
                val lineItem = buildString {
                    append(extra.displayName)
                    append(" (")
                    append(sessions)
                    append(" session")
                    if (sessions > 1) append("s")
                    append(")")
                }

                breakdown[lineItem] = extraCharge
                calculationDetails[lineItem] =
                    "₹%.2f × %d session%s".format(ratePerSession, sessions, if (sessions > 1) "s" else "")
                extrasCharge += extraCharge
            } else {
                val ratePerDay = HotelData.getExtraRate(extra)
                val extraCharge = ratePerDay * daysStayed
                breakdown[extra.displayName] = extraCharge
                calculationDetails[extra.displayName] = "₹%.2f/day × %d days".format(ratePerDay, daysStayed)
                extrasCharge += extraCharge
            }
        }

        val petFee = getPetFeeByWeight(petWeight)
        if (petWeight != null) {
            breakdown["Pet Fee"] = petFee
            calculationDetails["Pet Fee"] = "₹%.2f (one-time charge)".format(petFee)
        }

        val baseSubtotal = roomCharge + extrasCharge + petFee
        val serviceCharge = baseSubtotal * SERVICE_CHARGE_RATE
        breakdown["Service Charge"] = serviceCharge
        calculationDetails["Service Charge"] =
            "%.0f%% of base subtotal = ₹%.2f".format(SERVICE_CHARGE_RATE * 100, serviceCharge)

        val subtotal = baseSubtotal + serviceCharge
        breakdown["Subtotal"] = subtotal
        calculationDetails["Subtotal"] = "Room + Extras + Pet + Service = ₹%.2f".format(subtotal)

        val gstRate = if (subtotal <= GST_THRESHOLD) GST_LOWER_RATE else GST_UPPER_RATE
        val gstAmount = subtotal * gstRate
        breakdown["GST"] = gstAmount
        calculationDetails["GST"] = "%.0f%% of subtotal = ₹%.2f".format(gstRate * 100, gstAmount)

        return Bill(
            totalAmount = subtotal + gstAmount,
            breakdown = breakdown,
            calculationDetails = calculationDetails,
        )
    }

    private fun getRoomDetails(roomNumber: Int): Room =
        HotelData.findRoom(roomNumber) ?: throw IllegalArgumentException("Room not found.")

    private fun getPetFeeByWeight(petWeight: Double?): Double {
        if (petWeight == null) return 0.0

        val category = when {
            petWeight <= 8.0 -> "Small (below 8kg)"
            petWeight <= 15.0 -> "Medium (below 15kg)"
            else -> "Large (above 15kg)"
        }
        return HotelData.getPetFee(category)
    }

    private const val SERVICE_CHARGE_RATE = 0.03
    private const val GST_THRESHOLD = 7500.0
    private const val GST_LOWER_RATE = 0.05
    private const val GST_UPPER_RATE = 0.18
}

