package com.wishat12.app.whatsapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.wishat12.app.domain.PhoneUtils

object WhatsAppLauncher {
    fun buildUrl(phoneNumber: String, message: String): String? {
        val phone = PhoneUtils.normalize(phoneNumber)
        if (phone.length !in 8..15 || phone.firstOrNull() == '0') return null
        return "https://wa.me/$phone?text=${URLEncoder.encode(message, StandardCharsets.UTF_8.name()).replace("+", "%20")}"
    }

    fun open(context: Context, phoneNumber: String, message: String): Boolean {
        val url = buildUrl(phoneNumber, message) ?: return false
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
