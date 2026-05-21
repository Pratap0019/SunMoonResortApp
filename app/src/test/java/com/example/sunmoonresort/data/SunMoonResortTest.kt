package com.example.sunmoonresort.data

import com.example.sunmoonresort.model.Extras
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SunMoonResortTest {

    @Test
    fun calculateBill_withoutExtrasOrPet_usesLowerGstSlab() {
        val bill = SunMoonResort.calculateBill(
            roomNumber = 101,
            daysStayed = 2,
            selectedExtras = emptyList(),
            petWeight = null,
        )

        assertEquals(4000.0, bill.breakdown["Room Charge"] ?: 0.0, 0.001)
        assertEquals(120.0, bill.breakdown["Service Charge"] ?: 0.0, 0.001)
        assertEquals(206.0, bill.breakdown["GST"] ?: 0.0, 0.001)
        assertEquals(4326.0, bill.totalAmount, 0.001)
    }

    @Test
    fun calculateBill_withSpaAndPet_calculatesDetailedBreakdown() {
        val bill = SunMoonResort.calculateBill(
            roomNumber = 102,
            daysStayed = 2,
            selectedExtras = listOf(Extras.SPA, Extras.GymPASS),
            petWeight = 10.0,
            spaSessions = 2,
        )

        assertEquals(7000.0, bill.breakdown["Room Charge"] ?: 0.0, 0.001)
        assertEquals(3000.0, bill.breakdown["SPA (2 sessions)"] ?: 0.0, 0.001)
        assertEquals(1000.0, bill.breakdown["Gym Access"] ?: 0.0, 0.001)
        assertEquals(350.0, bill.breakdown["Pet Fee"] ?: 0.0, 0.001)
        assertEquals(13794.79, bill.totalAmount, 0.001)
    }

    @Test
    fun calculateBill_withMattressExtra_calculatesChargePerDay() {
        val bill = SunMoonResort.calculateBill(
            roomNumber = 104,
            daysStayed = 1,
            selectedExtras = listOf(Extras.MATTRESS),
            petWeight = null,
        )

        assertEquals(500.0, bill.breakdown["Extra Mattress"] ?: -1.0, 0.001)
        assertTrue(bill.totalAmount > 0.0)
    }
}

