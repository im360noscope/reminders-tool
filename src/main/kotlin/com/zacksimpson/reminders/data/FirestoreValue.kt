package com.zacksimpson.reminders.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

/**
 * Converts between plain kotlinx.serialization JSON and Firestore REST API's typed
 * "Value" wire format (each field wrapped as `{"stringValue": "..."}`, etc — see
 * https://firebase.google.com/docs/firestore/reference/rest/v1/Value).
 *
 * Only handles the value types reminders-web actually uses (numbers/strings/bools/
 * arrays/maps) — no timestampValue, referenceValue, geoPointValue, bytesValue.
 */
object FirestoreValue {
    /** A plain JSON field map (e.g. from `Json.encodeToJsonElement(Task.serializer(), t)`)
     *  into Firestore's per-field typed wrapper, ready to send as a document's `fields`. */
    fun encodeFields(fields: JsonObject): JsonObject =
        JsonObject(fields.mapValues { (_, v) -> encode(v) })

    /** The `fields` map from a Firestore document response back into plain JSON,
     *  decodable by `Json.decodeFromJsonElement(Task.serializer(), ...)`. */
    fun decodeFields(fields: JsonObject): JsonObject =
        JsonObject(fields.mapValues { (_, v) -> decode(v.jsonObject) })

    private fun encode(element: JsonElement): JsonObject = when (element) {
        is JsonNull -> buildJsonObject { put("nullValue", JsonNull) }
        is JsonArray -> buildJsonObject {
            put(
                "arrayValue",
                buildJsonObject { put("values", JsonArray(element.map { encode(it) })) },
            )
        }
        is JsonObject -> buildJsonObject {
            put("mapValue", buildJsonObject { put("fields", encodeFields(element)) })
        }
        is JsonPrimitive -> when {
            !element.isString && element.booleanOrNull != null ->
                buildJsonObject { put("booleanValue", element.boolean) }
            // Firestore's wire format encodes integerValue as a numeric *string*, unlike
            // every other numeric field type.
            !element.isString && element.longOrNull != null ->
                buildJsonObject { put("integerValue", element.content) }
            !element.isString ->
                buildJsonObject { put("doubleValue", element.double) }
            else -> buildJsonObject { put("stringValue", element.content) }
        }
    }

    private fun decode(value: JsonObject): JsonElement = when {
        "stringValue" in value -> JsonPrimitive(value.getValue("stringValue").jsonPrimitive.content)
        "booleanValue" in value -> JsonPrimitive(value.getValue("booleanValue").jsonPrimitive.boolean)
        "integerValue" in value -> JsonPrimitive(value.getValue("integerValue").jsonPrimitive.content.toLong())
        "doubleValue" in value -> JsonPrimitive(value.getValue("doubleValue").jsonPrimitive.double)
        "arrayValue" in value -> {
            val values = value["arrayValue"]?.jsonObject?.get("values")?.let { it as? JsonArray } ?: JsonArray(emptyList())
            JsonArray(values.map { decode(it.jsonObject) })
        }
        "mapValue" in value -> {
            val innerFields = value["mapValue"]?.jsonObject?.get("fields")?.jsonObject ?: JsonObject(emptyMap())
            decodeFields(innerFields)
        }
        else -> JsonNull // nullValue, or an unrecognized/absent Value variant.
    }
}

private val JsonPrimitive.boolean: Boolean get() = content.toBoolean()
private val JsonPrimitive.double: Double get() = content.toDouble()
