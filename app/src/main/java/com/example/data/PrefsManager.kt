package com.example.data

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("focusguard_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_BLOCKING_ENABLED = "blocking_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until"
    }

    var isBlockingEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCKING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCKING_ENABLED, value).apply()

    fun isPinSet(): Boolean {
        return getPinHash() != null
    }

    fun getPinHash(): String? {
        return prefs.getString(KEY_PIN_HASH, null)
    }

    fun savePinHash(pin: String) {
        val hash = hashSha256(pin)
        prefs.edit().putString(KEY_PIN_HASH, hash).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = getPinHash() ?: return false
        val incoming = hashSha256(pin)
        return stored == incoming
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN_HASH).apply()
    }

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    var lockoutUntilEpoch: Long
        get() = prefs.getLong(KEY_LOCKOUT_UNTIL, 0L)
        set(value) = prefs.edit().putLong(KEY_LOCKOUT_UNTIL, value).apply()

    fun isLockedOut(): Boolean {
        return System.currentTimeMillis() < lockoutUntilEpoch
    }

    fun getRemainingLockoutTimeSec(): Long {
        val diff = lockoutUntilEpoch - System.currentTimeMillis()
        return if (diff > 0) diff / 1000L else 0L
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
