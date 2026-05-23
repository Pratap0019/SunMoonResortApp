package com.example.sunmoonresort.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * Provides a single, correctly-configured [Gson] instance for all booking serialization.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * WHY THIS IS NEEDED
 * ──────────────────────────────────────────────────────────────────────────────
 * GSON's default Map deserializer creates a [com.google.gson.internal.LinkedTreeMap]
 * (alphabetically sorted) instead of a [java.util.LinkedHashMap] (insertion-ordered).
 *
 * [com.example.sunmoonresort.model.Bill] stores its breakdown and calculationDetails
 * in [LinkedHashMap]s with a specific display order:
 *   Room Charge → Extras → Pet Fee → Service Charge → Subtotal → GST
 *
 * Without this factory, after a GSON round-trip (save → load from either
 * SharedPreferences or Firestore), the breakdown list is reordered alphabetically:
 *   GST → Pet Fee → Room Charge → Service Charge → ...  (WRONG)
 *
 * [LinkedHashMapAdapter] intercepts all [Map] deserialization and forces it into
 * a [LinkedHashMap], preserving the original insertion order from the stored JSON.
 *
 * ──────────────────────────────────────────────────────────────────────────────
 * KEY TYPE SAFETY
 * ──────────────────────────────────────────────────────────────────────────────
 * JSON object keys are always strings, but the bookings root map is typed
 * Map<Int, List<BookingDetails>>.  The adapter converts JSON string keys to the
 * declared key type (Int, Long, Double, or String) so the rest of the app
 * continues to work exactly as before.
 *
 * Both [BookingLocalStore] and [BookingFirebaseStore] share this instance.
 * ──────────────────────────────────────────────────────────────────────────────
 */
object GsonFactory {

    val instance: Gson by lazy {
        GsonBuilder()
            .registerTypeHierarchyAdapter(Map::class.java, LinkedHashMapAdapter())
            .create()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Adapter
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Serializes maps exactly as GSON's default (insertion order respected).
     * Deserializes maps as [LinkedHashMap] to preserve JSON key order, and converts
     * the string-typed JSON keys to the map's declared key type (Int, Long, etc.).
     */
    private class LinkedHashMapAdapter :
        JsonSerializer<Map<*, *>>, JsonDeserializer<Map<*, *>> {

        // ── Serialize ──────────────────────────────────────────────────────────
        override fun serialize(
            src: Map<*, *>,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val obj = JsonObject()
            src.forEach { (key, value) ->
                obj.add(
                    key.toString(),
                    if (value == null) JsonNull.INSTANCE else context.serialize(value)
                )
            }
            return obj
        }

        // ── Deserialize ────────────────────────────────────────────────────────
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Map<*, *> {
            val jsonObject = json as? JsonObject ?: return LinkedHashMap<Any, Any?>()

            val mapType        = typeOfT as? ParameterizedType
            // Declared key type: e.g. Int  for Map<Int, List<BookingDetails>>
            //                         String for Map<String, Double>
            val keyType: Type  = mapType?.actualTypeArguments?.getOrNull(0) ?: String::class.java
            // Declared value type: e.g. List<BookingDetails>, Double, String
            val valueType: Type = mapType?.actualTypeArguments?.getOrNull(1) ?: Any::class.java

            val result = LinkedHashMap<Any?, Any?>(jsonObject.size())
            jsonObject.entrySet().forEach { (keyStr, value) ->
                val key: Any? = convertKey(keyStr, keyType)
                result[key] = if (value.isJsonNull) null else context.deserialize(value, valueType)
            }
            return result
        }

        /**
         * Convert a JSON object key (always a [String]) to the map's declared key type.
         * Falls back to the raw string if conversion is not possible.
         */
        private fun convertKey(keyStr: String, keyType: Type): Any? = when (keyType) {
            Int::class.java,
            Integer::class.java         -> keyStr.toIntOrNull() ?: keyStr

            Long::class.java,
            java.lang.Long::class.java  -> keyStr.toLongOrNull() ?: keyStr

            Double::class.java,
            java.lang.Double::class.java -> keyStr.toDoubleOrNull() ?: keyStr

            Float::class.java,
            java.lang.Float::class.java  -> keyStr.toFloatOrNull() ?: keyStr

            else                         -> keyStr   // String or any other type: keep as-is
        }
    }
}
