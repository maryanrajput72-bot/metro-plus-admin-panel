# Metro + Admin

Admin panel app — **Metro+** staff app ka admin panel, Android app ke andar.
Naam: **Metro + Admin** · Package: **com.metroplusadmin** · Current version: **1.0.0(03)** (versionCode 3)

## Logo

Wahi glossy gel **M** logo jo Metro+ ka hai — **sirf rang badla hai: red → green**.
Kuch aur nahi badla — shape, shine, style wahi hai. Green version `logo-green.png`
hai; launcher icons `app/src/main/res/mipmap-*/` me hain (Metro+ wale red icons
ko hue-shift karke banaye gaye).

## Structure — har file mehfooz hai

- `panel/` — v18 admin panel website (HTML + CSS + JS, `index.html` + `logo-green.png`).
  Browser me bhi chalti hai, GitHub Pages par bhi.
- `app/` — Android app (Kotlin, WebView). Panel app ke andar bundled hai:
  `app/src/main/assets/panel/index.html` — **wahi file jo `panel/` me hai**,
  ek hi code, do jagah. Badlo to dono jagah update karo.
  - `app/src/main/java/com/metroplusadmin/MainActivity.kt` — WebView, panel kholta hai.
  - `app/src/main/java/com/metroplusadmin/UpdateCheck.kt` — app open pe 5 second
    baad `version.json` check karta hai; naya version ho to Metro+ jaisa SILENT
    auto-update (PackageInstaller session; receiver isi file ke andar hai).
  - `app/src/main/java/com/metroplusadmin/AdminApp.kt` — crash catcher: crash ho
    to agli open pe wajah dikhata hai.
  - `app/src/main/java/com/metroplusadmin/LoginActivity.kt` — lock screen.
  - `app/src/main/java/com/metroplusadmin/Auth.kt` — naam + password hash check.
- `version.json` — updater manifest: versionCode, versionName, apkUrl (GitHub Release).
- `keystore/` + `keystore.properties` — **GitHub par KABHI nahi** (`.gitignore`).
  Sirf is machine par. Ye key Metro+ staff app wali key se ALAG hai, taake
  Admin app staff app ke upar install na ho.

## Login-crash fix (v1.0.0(03))

v1.0.0(02) login ke foran baad "has stopped" se band ho jati thi — wajah:
`BiometricPrompt.authenticate()` ko `USE_BIOMETRIC` permission chahiye hoti hai,
jo manifest me nahi thi → SecurityException → crash. Fix: permission lagi hai,
`authenticate()` try/catch me hai, aur `AdminApp` crash catcher lagaya hai.

## Login — fingerprint + password (v1.0.0(02))

Har dafa app kholo to **lock screen** aayegi:
- **Fingerprint** on hai to khulte hi fingerprint mangta hai — OK hote hi login.
- Fingerprint na ho / cancel ho to **Name + Password** se login: Name `Aryan`.
- Pehli dafa password se login hone par fingerprint set karne ko kehta hai.
- Password code me **plain nahi hai** — sirf uska salted SHA-256 hash hai
  (`Auth.kt`); asal password repo ya kisi file me kahin nahi likha.

## Panel — kya hai, kya nahi (v1.0.0(02))

Tabs: Dashboard (24 stations), Staff, TL Access, Data, Versions, Premium Requests.

- **Sach:** andar ka data **SAMPLE** hai — design dekhne ke liye. Asal Firebase
  data (native_ collections, project metro-plus-5966d) ki wiring **Phase 2** me hogi.
- **Premium Requests:** tab bana hua hai, khaali hai. Metro+ app me Premium Request
  bhejne ka button abhi sirf dikhawa hai — jab uska asal logic banega, requests
  yahan aayengi aur Approve yahin se hoga. Approval sirf admin kar sakega.
- **Login:** v1 me koi login nahi — pehle sirf admin (Zabii) ka Gmail, Phase 2 me.
- Metro+ staff app ka no-WebView rule barkarar hai — WebView **sirf is Admin app**
  ke liye hai, staff app ko haath nahi laga.

## Build

```sh
cd metro-admin-app
chmod +x gradlew   # GitHub se clone ke baad ek dafa (file mode save nahi hota)
./gradlew assembleDebug --offline
# APK: app/build/outputs/apk/debug/app-debug.apk
# Naam badal ke: MetroPlusAdmin-v1.0.0-01.apk
```

Version scheme: `version(build)` — ek version me build 01 se 100 tak, phir agla
version. versionCode global counter, hamesha barhta hai. Iteration builds DEBUG;
R8 release sirf final/public stage pe.

## Release

1. APK ka naam `MetroPlusAdmin-v1.0.0-01.apk` rakho.
2. GitHub Release `v1.0.0-01` banao, APK asset ke tor par upload karo.
3. `version.json` me versionCode + versionName + apkUrl update karo (apkUrl hamesha
   nayi APK par — warna purani APK download hogi).
4. App kholte hi 5 second baad update check hoga.
