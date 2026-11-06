package com.metroplusadmin

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Admin app lock screen — har dafa app kholo to ye screen aayegi.
 *
 * - Fingerprint on hai to khulte hi fingerprint mangta hai — OK hote hi login.
 * - Fingerprint na ho / cancel ho to Name + Password se login.
 * - Pehli dafa password se login hone par fingerprint set karne ko kehta hai.
 *
 * Framework BiometricPrompt (API 28+) — koi nayi dependency nahi.
 * API 28 se neeche sirf password se login hota hai.
 */
class LoginActivity : Activity() {

    private lateinit var nameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var statusText: TextView
    private var cancellationSignal: CancellationSignal? = null
    private var openedMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())

        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean(Auth.KEY_BIOMETRIC_ENABLED, false)

        // Fingerprint on hai to khulte hi mang lo.
        if (biometricEnabled && isBiometricAvailable()) {
            window.decorView.postDelayed({ showBiometricPrompt(enroll = false) }, 400L)
        }
    }

    override fun onPause() {
        super.onPause()
        try { cancellationSignal?.cancel() } catch (_: Exception) { }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()

    private fun buildUi(): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B3D2E"))
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(48), dp(24), dp(48))
        }
        scroll.addView(root)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(28))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
        }
        root.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val logo = ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            adjustViewBounds = true
        }
        card.addView(logo, LinearLayout.LayoutParams(dp(88), dp(88)))

        val title = TextView(this).apply {
            text = "Metro + Admin"
            textSize = 22f
            setTextColor(Color.parseColor("#111827"))
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, 0)
        }
        card.addView(title)

        val sub = TextView(this).apply {
            text = "Admin login"
            textSize = 14f
            setTextColor(Color.parseColor("#6B7280"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(18))
        }
        card.addView(sub)

        nameInput = EditText(this).apply {
            hint = "Name"
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 16f
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        card.addView(nameInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })

        passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 16f
            setPadding(dp(14), dp(12), dp(14), dp(12))
        }
        card.addView(passwordInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16)
        })

        val loginButton = Button(this).apply {
            text = "Login"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#0F766E"))
                cornerRadius = dp(12).toFloat()
            }
            setPadding(dp(16), dp(12), dp(16), dp(12))
            setOnClickListener { onPasswordLogin() }
        }
        card.addView(loginButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        statusText = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(Color.parseColor("#B91C1C"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        card.addView(statusText)

        val hint = TextView(this).apply {
            text = "Fingerprint on hai to wohi khulega — nahi to naam + password se login karo."
            textSize = 12f
            setTextColor(Color.parseColor("#9CA3AF"))
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }
        card.addView(hint)

        return scroll
    }

    private fun onPasswordLogin() {
        val name = nameInput.text.toString()
        val password = passwordInput.text.toString()
        if (!Auth.checkCredentials(name, password)) {
            statusText.text = "Naam ya password ghalat hai"
            return
        }
        statusText.text = ""
        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        val biometricEnabled = prefs.getBoolean(Auth.KEY_BIOMETRIC_ENABLED, false)
        // Pehli dafa password se login — fingerprint set karne ko kaho.
        if (!biometricEnabled && isBiometricAvailable()) {
            showBiometricPrompt(enroll = true)
        } else {
            openMain()
        }
    }

    private fun isBiometricAvailable(): Boolean {
        // Sirf API level dekho — asal availability (hardware / enrolled
        // fingerprint na hona) BiometricPrompt ke error callback me aati hai,
        // aur wahan password screen hi rehti hai.
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    }

    private fun showBiometricPrompt(enroll: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            if (enroll) openMain()
            return
        }
        val signal = CancellationSignal().also { cancellationSignal = it }
        val prompt = BiometricPrompt.Builder(this)
            .setTitle("Metro + Admin")
            .setSubtitle(
                if (enroll) "Fingerprint set karo — agli dafa fingerprint se khul jayega"
                else "Fingerprint se login karo"
            )
            .setNegativeButton(
                "Password use karo", mainExecutor,
                android.content.DialogInterface.OnClickListener { _, _ ->
                    if (enroll) openMain()
                }
            )
            .build()
        prompt.authenticate(
            signal,
            mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    if (enroll) {
                        getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
                            .edit().putBoolean(Auth.KEY_BIOMETRIC_ENABLED, true).apply()
                        Toast.makeText(
                            this@LoginActivity,
                            "Fingerprint set ho gaya", Toast.LENGTH_SHORT
                        ).show()
                    }
                    openMain()
                }

                override fun onAuthenticationFailed() {
                    statusText.text = "Fingerprint match nahi hua — dobara try karo ya password lagao"
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    // User ne cancel kiya ya fingerprint set nahi — password screen hi rehti hai.
                    // Enroll prompt tha to seedha andar (password login ho chuka hai).
                    if (enroll) openMain()
                }
            }
        )
    }

    private fun openMain() {
        if (openedMain) return
        openedMain = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
