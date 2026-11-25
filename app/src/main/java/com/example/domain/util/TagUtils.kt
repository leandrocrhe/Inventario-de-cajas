package com.example.domain.util

import com.example.data.local.entity.ItemEntity

data class TagStat(
    val name: String,
    val itemCount: Int
)

object TagUtils {

    val DEFAULT_FALLBACK_TAGS = listOf(
        "Frágil",
        "Cables",
        "Herramienta",
        "Documento",
        "Repuesto"
    )

    private val MULTI_WHITESPACE_REGEX = Regex("\\s+")

    /**
     * Trims leading/trailing whitespace and collapses internal multiple spaces to a single space.
     */
    fun normalizeTag(raw: String): String {
        return raw.trim().replace(MULTI_WHITESPACE_REGEX, " ")
    }

    /**
     * Splits a comma-separated tag string into normalized, deduplicated (case-insensitive) tags.
     */
    fun parseTags(rawTags: String): List<String> {
        if (rawTags.isBlank()) return emptyList()
        val seenLower = HashSet<String>()
        val result = mutableListOf<String>()
        rawTags.split(",").forEach { part ->
            val normalized = normalizeTag(part)
            if (normalized.isNotBlank() && seenLower.add(normalized.lowercase())) {
                result.add(normalized)
            }
        }
        return result
    }

    /**
     * Formats a list of tags into a canonical comma-separated string, normalized and deduplicated.
     */
    fun formatTags(tags: List<String>): String {
        if (tags.isEmpty()) return ""
        val seenLower = HashSet<String>()
        val result = mutableListOf<String>()
        tags.forEach { raw ->
            val normalized = normalizeTag(raw)
            if (normalized.isNotBlank() && seenLower.add(normalized.lowercase())) {
                result.add(normalized)
            }
        }
        return result.joinToString(", ")
    }

    /**
     * Adds a single normalized tag if non-blank and not already present (case-insensitive).
     */
    fun addTag(existing: List<String>, newTag: String): List<String> {
        val normalized = normalizeTag(newTag)
        if (normalized.isBlank()) return existing
        if (existing.any { it.equals(normalized, ignoreCase = true) }) {
            return existing
        }
        return existing + normalized
    }

    /**
     * Splits comma-separated input and merges normalized tags without duplicates (case-insensitive).
     */
    fun addTagsFromInput(existing: List<String>, rawInput: String): List<String> {
        if (rawInput.isBlank()) return existing
        var current = existing
        rawInput.split(",").forEach { part ->
            current = addTag(current, part)
        }
        return current
    }

    /**
     * Removes a tag from the list (case-insensitive).
     */
    fun removeTag(existing: List<String>, tagToRemove: String): List<String> {
        val normalizedTarget = normalizeTag(tagToRemove)
        return existing.filterNot { normalizeTag(it).equals(normalizedTarget, ignoreCase = true) }
    }

    /**
     * Computes tag usage statistics across all inventory items, sorted by frequency descending.
     */
    fun computeTagStats(items: List<ItemEntity>): List<TagStat> {
        val countsByLower = LinkedHashMap<String, Int>()
        val displayCasingFreq = LinkedHashMap<String, MutableMap<String, Int>>()

        items.forEach { item ->
            val uniqueItemTags = parseTags(item.tags)
            uniqueItemTags.forEach { tag ->
                val lower = tag.lowercase()
                countsByLower[lower] = (countsByLower[lower] ?: 0) + 1
                val casingMap = displayCasingFreq.getOrPut(lower) { LinkedHashMap() }
                casingMap[tag] = (casingMap[tag] ?: 0) + 1
            }
        }

        return countsByLower.entries
            .map { (lower, count) ->
                val bestDisplay = displayCasingFreq[lower]
                    ?.entries
                    ?.maxByOrNull { it.value }
                    ?.key ?: lower
                TagStat(name = bestDisplay, itemCount = count)
            }
            .sortedWith(
                compareByDescending<TagStat> { it.itemCount }
                    .thenBy { it.name.lowercase() }
            )
    }

    /**
     * Generates dynamic suggested tags from Room items, ordered by usage frequency.
     * Falls back to a minimal base list only when no tags exist in the database.
     */
    fun computeSuggestedTags(items: List<ItemEntity>): List<String> {
        val stats = computeTagStats(items)
        return if (stats.isNotEmpty()) {
            stats.map { it.name }
        } else {
            DEFAULT_FALLBACK_TAGS
        }
    }
}
