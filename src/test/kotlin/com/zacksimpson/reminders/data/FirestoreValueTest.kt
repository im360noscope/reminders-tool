package com.zacksimpson.reminders.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/** Unit tests for [FirestoreValue]'s conversion between plain JSON and Firestore's typed
 *  Value wire format, and for round-tripping real Task/ReminderList/Settings shapes
 *  through it via [firestoreModelJson]. */
class FirestoreValueTest {

    private fun parse(json: String): JsonObject = firestoreModelJson.parseToJsonElement(json).jsonObject

    @Test
    fun `encodes primitives to their typed wrapper`() {
        val fields = parse(
            """{"title":"Milk","order":3,"rating":4.5,"completed":false,"note":null}""",
        )
        val encoded = FirestoreValue.encodeFields(fields)

        assertEquals("""{"stringValue":"Milk"}""", encoded.getValue("title").toString())
        assertEquals("""{"integerValue":"3"}""", encoded.getValue("order").toString())
        assertEquals("""{"doubleValue":4.5}""", encoded.getValue("rating").toString())
        assertEquals("""{"booleanValue":false}""", encoded.getValue("completed").toString())
        assertEquals("""{"nullValue":null}""", encoded.getValue("note").toString())
    }

    @Test
    fun `encodes nested arrays and maps`() {
        val fields = parse(
            """{"subtasks":[{"id":"s1","completed":true}]}""",
        )
        val encoded = FirestoreValue.encodeFields(fields)

        val expected = parse(
            """
            {"subtasks":{"arrayValue":{"values":[
                {"mapValue":{"fields":{
                    "id":{"stringValue":"s1"},
                    "completed":{"booleanValue":true}
                }}}
            ]}}}
            """.trimIndent(),
        )
        assertEquals(expected.toString(), encoded.toString())
    }

    @Test
    fun `decode is the inverse of encode for primitives, arrays, and maps`() {
        val original = parse(
            """{"title":"Milk","order":3,"rating":4.5,"completed":false,"note":null,
                |"subtasks":[{"id":"s1","completed":true}]}
            """.trimMargin(),
        )

        val roundTripped = FirestoreValue.decodeFields(FirestoreValue.encodeFields(original))

        assertEquals(original, roundTripped)
    }

    @Test
    fun `Task round-trips through Firestore's field encoding unchanged`() {
        val task = Task(
            id = "t1",
            title = "Buy milk",
            listId = "inbox",
            date = "2026-07-24",
            recurrence = Recurrence(interval = 2, unit = RecurrenceUnit.WEEK),
            subtasks = listOf(Subtask(id = "s1", title = "2%", createdAt = 100L)),
            completed = false,
            createdAt = 100L,
            order = 0,
        )
        val fields = firestoreModelJson.encodeToJsonElement(Task.serializer(), task).jsonObject

        val roundTripped = FirestoreValue.decodeFields(FirestoreValue.encodeFields(fields))
        val decodedTask = firestoreModelJson.decodeFromJsonElement(Task.serializer(), roundTripped)

        assertEquals(task, decodedTask)
    }

    @Test
    fun `encodeDefaults keeps false and empty-collection fields present`() {
        // The exact class of bug that broke Phase 2's auth: a field left at its Kotlin
        // default (completed = false, subtasks = emptyList()) must still be sent to
        // Firestore, not silently dropped.
        val task = Task(id = "t1", title = "x", listId = "inbox", createdAt = 0L, order = 0)
        val json = firestoreModelJson.encodeToString(Task.serializer(), task)

        assertEquals(true, json.contains("\"completed\":false"))
        assertEquals(true, json.contains("\"deleted\":false"))
        assertEquals(true, json.contains("\"subtasks\":[]"))
    }
}
