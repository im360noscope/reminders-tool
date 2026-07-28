package com.zacksimpson.reminders.data

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

private const val TAG = "AuthClient"

/**
 * Talks to Firebase's Identity Toolkit REST API directly. light-sdk has no Firebase
 * Android SDK on its dependency allow-list (see SYNC_PLAN.md §2.1), so this hand-rolls
 * the two calls the app actually needs instead of pulling in firebase-auth.
 *
 * Bodies are read as raw text and decoded manually (rather than via Ktor's typed
 * ContentNegotiation) so a shape mismatch can log the actual response instead of just
 * an opaque conversion exception — useful while this is still being stood up against a
 * real Firebase project's exact response shape.
 */
class AuthClient {
    // encodeDefaults=true: kotlinx.serialization omits any field left at its Kotlin
    // default, and SignInRequest.returnSecureToken=true is always exactly its default —
    // without this, Firebase never sees returnSecureToken and just verifies the
    // password instead of starting a real session (no refreshToken issued).
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val http = HttpClient(OkHttp) {
        defaultRequest {
            // Identifies this app to the Android-restricted API key — see
            // FirebaseConfig's doc comment for why this can't be skipped.
            header("X-Android-Package", FirebaseConfig.ANDROID_PACKAGE)
            header("X-Android-Cert", FirebaseConfig.ANDROID_CERT_SHA1)
            // Identifies this as a Firebase app call, matching what every real Firebase
            // SDK sends.
            header("X-Firebase-gmpid", FirebaseConfig.FIREBASE_APP_ID)
        }
    }

    suspend fun signInWithPassword(email: String, password: String): AuthTokens {
        val response = http.post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword") {
            parameter("key", FirebaseConfig.API_KEY)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(SignInRequest.serializer(), SignInRequest(email = email, password = password)))
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw AuthException(errorMessage(text, response.status.value))
        val parsed = decode(SignInResponse.serializer(), text, "signInWithPassword")
        return AuthTokens(
            idToken = parsed.idToken,
            refreshToken = parsed.refreshToken,
            uid = parsed.localId,
            expiresAt = System.currentTimeMillis() + parsed.expiresIn.toLong() * 1000L,
        )
    }

    suspend fun refresh(refreshToken: String): AuthTokens {
        val response = http.submitForm(
            url = "https://securetoken.googleapis.com/v1/token",
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            },
        ) {
            parameter("key", FirebaseConfig.API_KEY)
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw AuthException(errorMessage(text, response.status.value))
        val parsed = decode(RefreshTokenResponse.serializer(), text, "refresh")
        return AuthTokens(
            idToken = parsed.idToken,
            refreshToken = parsed.refreshToken,
            uid = parsed.userId,
            expiresAt = System.currentTimeMillis() + parsed.expiresIn.toLong() * 1000L,
        )
    }

    fun close() = http.close()

    private fun <T> decode(serializer: kotlinx.serialization.KSerializer<T>, text: String, call: String): T =
        try {
            json.decodeFromString(serializer, text)
        } catch (e: Exception) {
            Log.e(TAG, "$call: unexpected response shape: $text", e)
            throw AuthException("Sign-in failed — unexpected server response")
        }

    // Falls back to a generic message if the error body isn't the shape Identity
    // Toolkit normally returns (e.g. a 5xx from an upstream proxy, not Firebase itself).
    private fun errorMessage(text: String, statusCode: Int): String {
        Log.e(TAG, "request failed ($statusCode): $text")
        return try {
            json.decodeFromString(IdentityToolkitErrorBody.serializer(), text).error.message
        } catch (e: Exception) {
            "Sign-in failed ($statusCode)"
        }
    }
}
