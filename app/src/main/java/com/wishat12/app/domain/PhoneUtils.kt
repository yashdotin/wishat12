package com.wishat12.app.domain

object PhoneUtils {
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val prefixNormalized = if (trimmed.startsWith("00")) trimmed.drop(2) else trimmed
        return prefixNormalized.filter { it.isDigit() }
    }

    fun isValid(raw: String): Boolean {
        val digits = normalize(raw)
        return digits.length in 8..15 && digits.firstOrNull() != '0'
    }
}
