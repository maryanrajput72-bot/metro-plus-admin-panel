package com.metroplusadmin

import java.security.MessageDigest

/**
 * Admin app lock — Name + Password, pehli kamyab login ke baad fingerprint.
 *
 * PASSWORD YAHAN NAHI HAI — sirf uska salted SHA-256 hash hai. Asal password
 * (jo Zabii ne diya) code, repo, ya kisi file me kahin nahi likha.
 * Naam "Aryan" secret nahi hai, is liye wohi plain hai.
 *
 * NOTE: password me "7280" aata hai — ye sirf password hai, staff (main)
 * channel par push ka 7280 signal NAHI hai. Is se kabhi main push nahi hoga.
 */
object Auth {
    const val LOGIN_NAME = "Aryan"

    // Salt + SHA-256(salt + ":" + password). Password kabhi save nahi karna.
    private const val PASSWORD_SALT = "e351be3a5bd99150"
    private const val PASSWORD_HASH = "061778cb34810421048fd6f02219e812bc3323e1f235e7559e766c82170afc26"

    const val PREFS = "admin_auth"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    fun checkCredentials(name: String, password: String): Boolean {
        if (!name.trim().equals(LOGIN_NAME, ignoreCase = true)) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$PASSWORD_SALT:$password".toByteArray(Charsets.UTF_8))
        val hex = bytes.joinToString("") { "%02x".format(it) }
        return hex == PASSWORD_HASH
    }
}
