package com.zacksimpson.reminders.data

import com.zacksimpson.reminders.BuildConfig

/**
 * The `reminders-web` Firebase project this app syncs against. `projectId` is also
 * needed by the Phase 3 Firestore REST client.
 *
 * [API_KEY] comes from [BuildConfig.FIREBASE_API_KEY] — set via `firebaseApiKey` in the
 * gitignored `local.properties` (see `local.properties.sample`), never hardcoded in
 * source. Unlike a normal Firebase *web* key (genuinely fine to publish, per
 * reminders-web's own firebase.ts comment), this one's Android-app restriction is
 * enforced purely by the X-Android-Package/X-Android-Cert headers a REST client sends
 * itself — since [ANDROID_PACKAGE] and [ANDROID_CERT_SHA1] necessarily live in this same
 * source file, publishing the key alongside them would let anyone reconstruct a request
 * that passes the restriction. Treat it as a real secret.
 *
 * It's a separate key from the one reminders-web uses, not a copy of it — Google Cloud
 * API keys only support one "Application restrictions" mode at a time, so a phone client
 * and a website can never share a key (referrer-restricted vs. Android-restricted are
 * mutually exclusive). This one is scoped to just identitytoolkit/securetoken and
 * restricted to package `com.zacksimpson.reminders` signed with the local dev keystore's
 * cert (`sdk/keys/lightsdk-dev.jks`, SHA-1 B8:22:D4:81:49:9E:E5:C3:CC:7D:B9:0A:EE:C2:94:5D:8F:23:07:15).
 * A build signed with a different cert (e.g. once Light's own build server signs release
 * builds) will need that cert's SHA-1 added to this key's allowed applications too, via
 * `gcloud services api-keys update`.
 */
object FirebaseConfig {
    val API_KEY: String = BuildConfig.FIREBASE_API_KEY
    const val PROJECT_ID = "reminders-web-zs2026"

    // Sent as X-Android-Package/X-Android-Cert on every request — a raw REST call has
    // no Play-Services-provided app identity, so this API key's Android-app restriction
    // can only be satisfied by setting these headers ourselves. Must match exactly what
    // the key's allowed-application entry above was created with.
    const val ANDROID_PACKAGE = "com.zacksimpson.reminders"
    const val ANDROID_CERT_SHA1 = "B822D481499EE5C3CC7DB90AEEC2945D8F230715"

    // Sent as X-Firebase-gmpid — the app ID every real Firebase SDK attaches to identify
    // itself. Kept since it's correct practice even though testing showed it isn't what's
    // causing the signInWithPassword response issue above.
    const val FIREBASE_APP_ID = "1:1055613872869:web:5d1911c7d04e6819e5020c"
}
