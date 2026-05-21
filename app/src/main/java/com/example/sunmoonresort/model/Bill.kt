package com.example.sunmoonresort.model

data class Bill(
    val totalAmount: Double,
    val breakdown: Map<String, Double>,
    val calculationDetails: Map<String, String> = linkedMapOf(),
)

