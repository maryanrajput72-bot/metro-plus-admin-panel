package com.metroplusadmin

import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Admin app lock — Name + Password, pehli kamyab login ke baad fingerprint.
 *
 * PASSWORD YAHAN NAHI HAI — sirf uska salted SHA-256 hash hai. Asal password
 * (jo Zabii ne diya) code, repo, ya kisi file me kahin nahi likha.
 * Naam "Aryan" secret nahi hai, is liye wohi plain hai.
 *
 * Settings tab se naam/password badlo to nayi values SharedPreferences me
 * save hoti hain (salt + hash — password kabhi plain save nahi hota) aur
 * login wahi nayi values check karta hai. Kuch badla na ho to neeche wali
 * default values chalti hain.
 *
 * NOTE: password me "7280" aata hai — ye sirf password hai, staff (main)
 * channel par push ka 7280 signal NAHI hai. Is se kabhi main push nahi hoga.
 */
object Auth {
    const val DEFAULT_NAME = "Aryan"

    // Salt + SHA-256(salt + ":" + password). Password kabhi save nahi karna.
    private const val DEFAULT_SALT = "e351be3a5bd99150"
    private const val DEFAULT_HASH = "061778cb34810421048fd6f02219e812bc3323e1f235e7559e766c82170afc26"

    const val PREFS = "admin_auth"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_NAME = "admin_name"
    const val KEY_SALT = "admin_salt"
    const val KEY_HASH = "admin_hash"

    fun storedName(prefs: SharedPreferences): String =
        prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME

    private fun sha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun checkCredentials(prefs: SharedPreferences, name: String, password: String): Boolean {
        if (!name.trim().equals(storedName(prefs), ignoreCase = true)) return false
        val salt = prefs.getString(KEY_SALT, DEFAULT_SALT) ?: DEFAULT_SALT
        val hash = prefs.getString(KEY_HASH, DEFAULT_HASH) ?: DEFAULT_HASH
        return sha256("$salt:$password") == hash
    }

    /**
     * Naam/password badlo. Result: "ok" / "wrong_current" / "empty" / "short".
     * Current password ghalat ho to kuch nahi badalta.
     */
    fun changeCredentials(
        prefs: SharedPreferences,
        currentPassword: String,
        newName: String,
        newPassword: String
    ): String {
        val name = newName.trim()
        if (name.isEmpty() || newPassword.isEmpty()) return "empty"
        if (newPassword.length < 4) return "short"
        if (!checkCredentials(prefs, storedName(prefs), currentPassword)) return "wrong_current"
        val newSalt = ByteArray(8).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        prefs.edit()
            .putString(KEY_NAME, name)
            .putString(KEY_SALT, newSalt)
            .putString(KEY_HASH, sha256("$newSalt:$newPassword"))
            .apply()
        return "ok"
    }
}
