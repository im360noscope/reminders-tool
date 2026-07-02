# Reminders — Native (light-sdk) rewrite

Native Kotlin/Jetpack Compose rewrite of the Reminders tool for the **Light Phone III**,
built on the official [`light-sdk`](https://github.com/lightphone/light-sdk).

This branch (`native-sdk`) supersedes the React Native / Expo app that lives on `main`.
It is a ground-up rebuild — no source is shared with the RN app.

## How this builds

A Light tool is **not** a standalone Gradle project; it must be built as a module
*inside* the `light-sdk` multi-project build (it depends on `project(":sdk:client")`,
the repo-local signing key, and the composite `light-sdk` Gradle plugin). There is no
published-artifact standalone path yet.

So this directory contains **only the tool module**. To build it, it is wired into a
local clone of `light-sdk` via one line in that repo's `settings.gradle.kts`:

```kotlin
include(":reminders")
project(":reminders").projectDir = file("/Users/zacksimpson/Dev/reminders-native")
```

The SDK clone lives at `~/Dev/light/light-sdk` and is **not** tracked by this repo —
keep it up to date with `git pull` as the SDK evolves (it changes fast).

### Build the debug APK

```bash
cd ~/Dev/light/light-sdk
./gradlew :reminders:assembleDebug
# APK: ~/Dev/reminders-native/build/outputs/apk/debug/
```

### Install on device / emulator

```bash
adb install -r ~/Dev/reminders-native/build/outputs/apk/debug/reminders-debug.apk
```

Test on an Android emulator running the [LightOS emulator system app](https://github.com/lightphone/light-sdk/tree/main/docs/system_app),
or sideload onto real LP3 hardware.

## Status

| Area | State |
|------|-------|
| Scaffold (module, entry point, boot screen) | ✅ compiles |
| Theme (black/white) | ⏳ next |
| Fonts | ✅ automatic — SDK provides **Akkurat**; we bundle nothing (see below) |
| Data layer (Task/List/Settings + DataStore) | ⏳ |
| Screens (19) + components (27) | ⏳ |
| Notifications | ⛔ deferred — SDK has no exact-time local alarm (see below) |

## Fonts (Akkurat — handled by the SDK)

The Light Phone uses **Akkurat**, and the SDK applies it for us automatically:
`LightTheme` calls `rememberLightTypography()` → `lightFontFamily(context)`
(`sdk/ui/.../LightFont.kt`), and every `LightText` reads its typography from the theme.
So any composable wrapped in `LightTheme { }` renders in Akkurat with no work from us.

Resolution order in `lightFontFamily()`:
1. **System Akkurat** — on real LP3 / the LightOS emulator, read from the OS font set.
2. **Bundled Akkurat** — `akkuratll_light` / `akkuratll_regular` / `akkuratpro_bold` ship
   inside the `light-keyboard` AAR, which `sdk:ui` re-exports (`api`). They merge into our
   APK transitively, so Akkurat also renders on a **plain non-LightOS emulator**.
3. `FontFamily.Default` — last-resort fallback (not normally hit).

We do **not** (and legally can't) bundle Akkurat ourselves — it's a licensed Lineto
typeface. The old RN app's PublicSans has been removed. Weight mapping for the old
"Thin" numeric displays (time-picker digits, recurrence interval): use Akkurat
`FontWeight.Light`.

## Notifications (deferred)

The RN app fired **exact-time** local reminders via `SCHEDULE_EXACT_ALARM`. The SDK's
permission allow-list currently excludes `SCHEDULE_EXACT_ALARM` and `RECEIVE_BOOT_COMPLETED`,
and `LightWork` (its WorkManager wrapper) is ≥15-min and inexact.

**This is temporary.** Light has confirmed notifications are coming — delivered via
**UnifiedPush with Light supplying the server** (`LightEntryPoint.onPushNotification` /
`enablePushNotifications` are already the hooks for it). The SDK is an explicit
work-in-progress and Light is opening up permissions over time.

Per project decision, notifications are **deferred** — we build every other screen now
and wire in the push-based notification story once Light ships that side of the SDK.

See `~/Dev/reminders/LIGHT_SDK_MIGRATION.md` for the full per-screen migration audit.
