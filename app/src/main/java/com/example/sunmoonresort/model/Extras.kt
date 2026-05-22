package com.example.sunmoonresort.model

enum class Extras(val displayName: String) {
    MATTRESS("Extra Mattress"),
    SPA("SPA"),
    GymPASS("Gym Access"),
    PoolPASS("Pool Access");


    override fun toString(): String = displayName
}

