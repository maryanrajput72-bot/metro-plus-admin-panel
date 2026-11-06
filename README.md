# Metro + Admin

Admin panel app — **Metro+** staff app ka admin panel, Android app ke andar.
Naam: **Metro + Admin** · Package: **com.metroplusadmin** · Current version: **1.0.0(07)** (versionCode 7)

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

## Proper home screen (v1.0.0(07))

Online research (mobile dashboard patterns) + Gemini ideas (gemini-3.5-flash) se:
- **Top bar:** ghari (HH:MM, live) + ⚙️ gear button — Settings seedha upar se.
- **Hero card:** pehle jaisa summary + har tile par progress ring (Opening 87%,
  Closing 75%, Attendance 98%).
- **Alert strip:** sirf pending cheezein (Opening 3, Closing 6, absent 2) — tap
  karne par us tab par jata hai.
- **Categories hub:** 6 module cards (Staff, TL Access, Data, Versions, Premium
  Requests, Settings) — badge counts ke saath, tap par tab khulta hai.
- **Station matrix:** 24 stations ab table nahi, 2/4-column cards — naam, number,
  Opening/Closing pills, attendance. Phone par 2-column.
- **Audit stream:** aaj ki activity timeline (sample).
- **Settings detail:** Security (fingerprint on/off toggle — asal kaam karta hai,
  bridge se), Admin Login, App (manual update check), Data & Backup (sample
  notice), About.
- Panel data abhi bhi SAMPLE hai (Phase 2 Firebase wiring pending).

## Updater diagnose (v1.0.0(06))

- (04) se (05) andar se nahi aayi thi, kuch dikha bhi nahi tha — is liye check ab
  **nazar aata hai**: khulte hi "Update check ho raha hai…" toast; nayi na ho to
  "Koi nayi update nahi hai"; fail ho to wajah ke saath toast + 20 second baad
  ek retry.
- **Fallback:** version.json (raw.githubusercontent.com) na khule to GitHub API
  `releases/latest` se tag + APK asset URL leta hai.
- **Manual check:** Settings tab > App > "Abhi update check karo" button
  (JS bridge `AdminSettings.checkUpdateNow()`).

## Splash 5s + logo adjust + home hero + update progress (v1.0.0(05))

- **Splash 5 second** (pehle ~1.8s tha) — logo + progress bar, phir login.
- **Logo adjust:** `logo-green.png` ke kaale kone + upar ki laal lakeer hatayi —
  saaf tile `res/drawable/logo_tile.png` (rounded-rect mask + 4px crop), splash
  aur login card me seedha tile dikhta hai, white circle nahi.
- **Home screen fit:** Dashboard ke upar ab EK hero card hai (green gradient) —
  usi me saara summary: Stations 24, Opening 21/24, Closing 18/24, Attendance
  118/120. Stations table `overflow-x: auto` wrap me hai taake phone par fit rahe.
- **Update progress:** update mile to ab dialog dikhta hai — "% + MB" ke saath
  kitni download hui; fail ho to toast batata hai (pehle kuch nahi dikhta tha).

## Splash + nayi login + Settings (v1.0.0(04))

- **Splash:** har open pe green M logo (white circle, green ring) + neeche progress
  bar chalti hai (~1.8s), phir login screen aati hai.
- **Login redesign:** green gradient background, white rounded card, logo circle,
  gol input boxes, green gradient button, fingerprint button (on ho to).
- **Settings tab (panel):** upar hero card + "Admin Login" — abhi ka password
  daal ke naam/password badlo. Nayi values SharedPreferences me salt+hash ke
  tor par save hoti hain (`Auth.kt`); login wahi check karta hai. Browser
  preview me Settings kaam nahi karta — sirf app ke andar (JS bridge).

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
