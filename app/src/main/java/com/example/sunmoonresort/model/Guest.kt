package com.example.sunmoonresort.model

data class Guest(
    val name: String,
    val contactNumber: String, // includes country code (e.g. +91-9876543210)
) {
    fun getName(): String = name
    fun getContactNumber(): String = contactNumber
}

