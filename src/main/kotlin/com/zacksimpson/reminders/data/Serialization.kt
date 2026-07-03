package com.zacksimpson.reminders.data

import kotlinx.serialization.json.Json

/**
 * Single serialization config, shared by [RemindersRepository] and its tests.
 *
 * - `ignoreUnknownKeys` — tolerate fields added by future app versions.
 * - `encodeDefaults` — write defaulted fields explicitly (stable, self-describing JSON).
 * - `coerceInputValues` — a `null` supplied for a non-null field falls back to that
 *   field's default instead of throwing, so minor malformed input degrades gracefully
 *   rather than being treated as full corruption.
 */
internal val appJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}
