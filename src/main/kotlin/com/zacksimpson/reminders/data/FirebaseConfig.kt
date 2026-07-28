package com.zacksimpson.reminders.data

import com.zacksimpson.reminders.BuildConfig

/**
 * The reminders-web Firebase project this app syncs against.
 *
 * [API_KEY] comes from [BuildConfig.FIREBASE_API_KEY], set via the gitignored
 * `local.properties` — never hardcode it here. It's Android-app restricted (scoped to
 * [ANDROID_PACKAGE] + [ANDROID_CERT_SHA1]), a separate key from reminders-web's own web
 * key since a Google Cloud API key can't be both referrer- and Android-restricted.
 * A build signed with a different cert will need its SHA-1 added to the key's allowed
 * applications via `gcloud services api-keys update`.
 */
object FirebaseConfig {
    val API_KEY: String = BuildConfig.FIREBASE_API_KEY
    const val PROJECT_ID = "reminders-web-zs2026"

    // Sent as X-Android-Package/X-Android-Cert on every request — a raw REST call has
    // no Play-Services app identity, so these headers satisfy the restriction manually.
    const val ANDROID_PACKAGE = "com.zacksimpson.reminders"
    const val ANDROID_CERT_SHA1 = "B822D481499EE5C3CC7DB90AEEC2945D8F230715"

    // Sent as X-Firebase-gmpid — the app ID every Firebase SDK attaches to itself.
    const val FIREBASE_APP_ID = "1:1055613872869:web:5d1911c7d04e6819e5020c"
}
