package com.example.data.local.converter

import android.net.Uri
import androidx.room.TypeConverter
import org.json.JSONArray
import java.util.Date

/**
 * Room TypeConverters for managing types used across Box and Item entities:
 * - Timestamps (Date <-> Long)
 * - Lists of strings (tags, hierarchical paths, etc.)
 * - Android Uri for photos (Uri <-> String)
 */
class Converters {

    // --- Date / Timestamp Converters ---
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- List<String> Converters (JSON based for safe delimiters) ---
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        val jsonArray = JSONArray()
        for (item in list) {
            jsonArray.put(item)
        }
        return jsonArray.toString()
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return null
        return try {
            val jsonArray = JSONArray(value)
            val result = ArrayList<String>(jsonArray.length())
            for (i in 0 until jsonArray.length()) {
                result.add(jsonArray.getString(i))
            }
            result
        } catch (_: Exception) {
            // Fallback for simple comma-separated strings if any
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    // --- Uri Converters for primary photo storage & resolution ---
    @TypeConverter
    fun fromUri(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun toUri(uriString: String?): Uri? {
        return if (uriString.isNullOrBlank()) null else Uri.parse(uriString)
    }
}
