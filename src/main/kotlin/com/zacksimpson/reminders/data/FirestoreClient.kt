package com.zacksimpson.reminders.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class FirestoreException(message: String) : Exception(message)

/** A single Firestore document as this app sees it: its ID plus the field map already
 *  unwrapped from Firestore's typed Value format into plain JSON. */
data class FirestoreDocument(val id: String, val fields: JsonObject)

/**
 * Talks to Firestore's REST API directly (see SYNC_PLAN.md §3) — light-sdk has no
 * Firestore Android SDK on its dependency allow-list, so this is a thin client over
 * `firestore.googleapis.com`, authenticated with the ID token [AuthRepository] manages.
 * `firestore.rules` already scopes every request to `request.auth.uid == uid`; no
 * server-side changes were needed for the phone to read/write here.
 *
 * Generic over a collection's document shape (`FirestoreDocument.fields` is plain JSON,
 * not a `Task`/`ReminderList`) — converting to/from those model types is the sync
 * engine's job (Phase 4), using [firestoreModelJson] so it doesn't repeat the
 * `encodeDefaults` mistake [AuthClient] made (see that file's history).
 */
class FirestoreClient(private val authRepo: AuthRepository) {
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(OkHttp)

    private val baseUrl =
        "https://firestore.googleapis.com/v1/projects/${FirebaseConfig.PROJECT_ID}/databases/(default)/documents"

    fun close() = http.close()

    /** Every document in a user's collection (e.g. "tasks", "lists") — soft-deleted
     *  (`deleted: true`) documents are included too, same as reminders-web's own
     *  `getDocs`/`onSnapshot` calls; filtering those out is the caller's job. */
    suspend fun listDocuments(uid: String, collection: String): List<FirestoreDocument> {
        val token = validToken()
        val documents = mutableListOf<FirestoreDocument>()
        var pageToken: String? = null
        do {
            val response = http.get("$baseUrl/users/$uid/$collection") {
                header(HttpHeaders.Authorization, "Bearer $token")
                parameter("pageSize", PAGE_SIZE)
                pageToken?.let { parameter("pageToken", it) }
            }
            val text = response.bodyAsText()
            if (!response.status.isSuccess()) throw FirestoreException(errorMessage(text, response.status.value))
            val body = json.parseToJsonElement(text).jsonObject
            val docs = body["documents"]?.jsonArray.orEmpty()
            documents += docs.map { toFirestoreDocument(it.jsonObject) }
            pageToken = body["nextPageToken"]?.jsonPrimitive?.contentOrNull
        } while (pageToken != null)
        return documents
    }

    /** The singleton settings document, or null if the user has never synced settings
     *  before (matches reminders-web's own "doesn't exist yet" handling). */
    suspend fun getDocument(uid: String, collection: String, docId: String): FirestoreDocument? {
        val token = validToken()
        val response = http.get("$baseUrl/users/$uid/$collection/$docId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (response.status.value == 404) return null
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw FirestoreException(errorMessage(text, response.status.value))
        return toFirestoreDocument(json.parseToJsonElement(text).jsonObject)
    }

    /**
     * Full replace-or-create of a document. Deliberately sends no `updateMask` — this
     * app always syncs whole objects (§4's whole-document last-write-wins design, not
     * per-field patches), and Firestore's PATCH without an updateMask replaces the
     * entire document with exactly the fields given, matching that.
     */
    suspend fun setDocument(uid: String, collection: String, docId: String, fields: JsonObject) {
        val token = validToken()
        val response = http.patch("$baseUrl/users/$uid/$collection/$docId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject { put("fields", FirestoreValue.encodeFields(fields)) }.toString(),
            )
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) throw FirestoreException(errorMessage(text, response.status.value))
    }

    private suspend fun validToken(): String =
        authRepo.validIdToken() ?: throw FirestoreException("Not signed in")

    private fun toFirestoreDocument(document: JsonObject): FirestoreDocument {
        val name = document.getValue("name").jsonPrimitive.content
        val id = name.substringAfterLast('/')
        val rawFields = document["fields"]?.jsonObject ?: JsonObject(emptyMap())
        return FirestoreDocument(id, FirestoreValue.decodeFields(rawFields))
    }

    private fun errorMessage(text: String, statusCode: Int): String = try {
        json.parseToJsonElement(text).jsonObject["error"]?.jsonObject
            ?.get("message")?.jsonPrimitive?.contentOrNull
            ?: "Firestore request failed ($statusCode)"
    } catch (e: Exception) {
        "Firestore request failed ($statusCode)"
    }

    private companion object {
        const val PAGE_SIZE = 100
    }
}

/** Shared Json config for converting Task/ReminderList/Settings to/from Firestore's
 *  plain-field JSON. encodeDefaults=true is deliberate: kotlinx.serialization omits any
 *  field left at its Kotlin default when false, which would silently drop fields like
 *  `completed = false` or `deleted = false` from what's sent to Firestore — the same
 *  class of bug that broke Phase 2's sign-in until it was found (see AuthClient.kt). */
val firestoreModelJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }
