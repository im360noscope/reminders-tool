package com.zacksimpson.reminders.data

import kotlinx.serialization.json.Json

/**
 * Shared JSON config. ignoreUnknownKeys tolerates fields from future versions;
 * encodeDefaults writes defaulted fields explicitly; coerceInputValues falls back to a
 * field's default when given null for a non-null field.
 */
internal val appJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    coerceInputValues = true
}
