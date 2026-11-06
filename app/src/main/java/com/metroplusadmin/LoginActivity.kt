package com.metroplusadmin

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
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
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Splash + lock screen.
 *
 * Har dafa app kholo to pehle SPLASH: green M logo (white circle, green ring)
 * + neeche progress bar chalti hai → phir login screen aati hai.
 *
 * Login screen (nayi design, v1.0.0(04)): green gradient background, white
 * rounded card, logo circle upar, gol input boxes, green button.
 * - Fingerprint on hai to splash ke baad khud fingerprint mangta hai.
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
    private lateinit var splashView: View
    private lateinit var loginView: View
    private var cancellationSignal: CancellationSignal? = null
    private var openedMain = false
    private var splashDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = FrameLayout(this)
        splashView = buildSplash()
        loginView = buildLoginUi().apply { visibility = View.GONE }
        root.addView(splashView)
        root.addView(loginView)
        setContentView(root)

        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        // Pichhli dafa app band hui thi to wajah dikhao (screenshot ke liye).
        prefs.getString("last_crash", null)?.let { crash ->
            prefs.edit().remove("last_crash").apply()
            android.app.AlertDialog.Builder(this)
                .setTitle("Pichhli dafa app band hone ki wajah")
                .setMessage(crash)
                .setPositiveButton("Theek hai", null)
                .show()
        }

        // Naam pehle se bhara ho (Settings se badla ho to wahi aayega).
        nameInput.setText(Auth.storedName(prefs))

        runSplash()
    }

    override fun onPause() {
        super.onPause()
        try { cancellationSignal?.cancel() } catch (_: Exception) { }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()

    private fun greenGradient(): GradientDrawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(Color.parseColor("#065F46"), Color.parseColor("#022C22"))
    )

    private fun buildSplash(): View {
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = greenGradient()
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }

        // Logo — saaf tile (kaale kone hataye hue), seedha gradient par,
        // bilkul beech me. White circle nahi — tile khud hi gol-kone wala hai.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_tile)
            adjustViewBounds = true
        }
        splash.addView(logo, LinearLayout.LayoutParams(dp(120), dp(120)))

        val title = TextView(this).apply {
            text = "Metro + Admin"
            textSize = 24f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }
        splash.addView(title)

        val sub = TextView(this).apply {
            text = "Admin Panel"
            textSize = 13f
            setTextColor(Color.parseColor("#A7F3D0"))
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(28))
        }
        splash.addView(sub)

        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            progressTintList = ColorStateList.valueOf(Color.parseColor("#34D399"))
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            alpha = 0.35f
            id = View.generateViewId()
            tag = "splashBar"
        }
        splash.addView(bar, LinearLayout.LayoutParams(dp(200), dp(6)))

        val loading = TextView(this).apply {
            text = "Khul raha hai…"
            textSize = 12f
            setTextColor(Color.parseColor("#D1FAE5"))
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }
        splash.addView(loading)

        return splash
    }

    private fun runSplash() {
        val bar = splashView.findViewWithTag<ProgressBar>("splashBar") ?: run {
            showLogin()
            return
        }
        ValueAnimator.ofInt(0, 100).apply {
            duration = 5000L
            interpolator = LinearInterpolator()
            addUpdateListener { bar.progress = it.animatedValue as Int }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    showLogin()
                }
            })
            start()
        }
    }

    private fun showLogin() {
        if (splashDone) return
        splashDone = true
        splashView.animate().alpha(0f).setDuration(300L).withEndAction {
            splashView.visibility = View.GONE
        }.start()
        loginView.alpha = 0f
        loginView.visibility = View.VISIBLE
        loginView.animate().alpha(1f).setDuration(300L).start()

        // Fingerprint on hai to splash ke baad khud mang lo.
        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(Auth.KEY_BIOMETRIC_ENABLED, false) && isBiometricAvailable()) {
            loginView.postDelayed({ showBiometricPrompt(enroll = false) }, 400L)
        }
    }

    private fun roundedInputBg(): GradientDrawable = GradientDrawable().apply {
        setColor(Color.parseColor("#F3F4F6"))
        cornerRadius = dp(12).toFloat()
    }

    private fun buildLoginUi(): View {
        val scroll = ScrollView(this).apply {
            background = greenGradient()
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
                cornerRadius = dp(24).toFloat()
            }
        }
        root.addView(card, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // Logo tile upar — saaf, beech me.
        val logo = ImageView(this).apply {
            setImageResource(R.drawable.logo_tile)
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
            setPadding(0, dp(4), 0, dp(20))
        }
        card.addView(sub)

        nameInput = EditText(this).apply {
            hint = "Name"
            inputType = InputType.TYPE_CLASS_TEXT
            textSize = 16f
            background = roundedInputBg()
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }
        card.addView(nameInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(10)
        })

        passwordInput = EditText(this).apply {
            hint = "Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            textSize = 16f
            background = roundedInputBg()
            setPadding(dp(16), dp(13), dp(16), dp(13))
        }
        card.addView(passwordInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(16)
        })

        val loginButton = Button(this).apply {
            text = "Login"
            textSize = 16f
            setTextColor(Color.WHITE)
            background = GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.parseColor("#0F766E"), Color.parseColor("#14B8A6"))
            ).apply { cornerRadius = dp(14).toFloat() }
            setPadding(dp(16), dp(13), dp(16), dp(13))
            setOnClickListener { onPasswordLogin() }
        }
        card.addView(loginButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        // Fingerprint on hai to haath se bhi mangne ka button.
        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(Auth.KEY_BIOMETRIC_ENABLED, false) && isBiometricAvailable()) {
            val fpButton = Button(this).apply {
                text = "Fingerprint se login karo"
                textSize = 15f
                setTextColor(Color.parseColor("#0F766E"))
                background = GradientDrawable().apply {
                    setColor(Color.WHITE)
                    setStroke(dp(2), Color.parseColor("#0F766E"))
                    cornerRadius = dp(14).toFloat()
                }
                setPadding(dp(16), dp(12), dp(16), dp(12))
                setOnClickListener { showBiometricPrompt(enroll = false) }
            }
            card.addView(fpButton, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(10)
            })
        }

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
        val prefs = getSharedPreferences(Auth.PREFS, MODE_PRIVATE)
        if (!Auth.checkCredentials(prefs, name, password)) {
            statusText.text = "Naam ya password ghalat hai"
            return
        }
        statusText.text = ""
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
        try {
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
        } catch (e: Exception) {
            // Fingerprint khul hi na sake (permission/hardware) — app band
            // nahi hogi; password se login rehta hai.
            statusText.text = "Fingerprint nahi khula — password se login karo"
            if (enroll) openMain()
        }
    }

    private fun openMain() {
        if (openedMain) return
        openedMain = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
