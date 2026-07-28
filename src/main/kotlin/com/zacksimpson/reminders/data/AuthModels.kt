package com.zacksimpson.reminders.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Normalized result of any Identity Toolkit auth call — signInWithPassword and the
 *  token-refresh endpoint return differently-shaped, differently-cased JSON, but both
 *  get mapped into this one shape for everything downstream to use. */
data class AuthTokens(
    val idToken: String,
    val refreshToken: String,
    val uid: String,
    val expiresAt: Long, // epoch millis
)

class AuthException(message: String) : Exception(message)

@Serializable
internal data class SignInRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
)

@Serializable
internal data class SignInResponse(
    val idToken: String,
    val refreshToken: String,
    val localId: String,
    val expiresIn: String,
)

@Serializable
internal data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("user_id") val userId: String,
    @SerialName("expires_in") val expiresIn: String,
)

@Serializable
internal data class IdentityToolkitErrorBody(val error: IdentityToolkitError)

@Serializable
internal data class IdentityToolkitError(val code: Int, val message: String)
