package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

/**
 * Comprehensive filter criteria for vector items.
 * Supports multiple filtering dimensions for professional use.
 */
data class FilterCriteria(
    val text: String? = null,
    val sizeRange: IntRange? = null,
    val complexityLevel: ComplexityLevel? = null,
    val fileSizeRange: LongRange? = null,
    val tags: List<String> = emptyList(),
    val usageStatus: UsageStatus? = null,
    val hasAnimations: Boolean? = null,
    val hasOptimizationSuggestions: Boolean? = null,
    val colors: Set<String> = emptySet(),
    val colorMatchMode: ColorMatchMode = ColorMatchMode.ANY,
    val colorCountRange: IntRange? = null
)

/**
 * Represents the usage status of a vector in the project.
 */
enum class UsageStatus {
    USED,
    UNUSED,
    FREQUENTLY_USED,
    RARELY_USED
}

/**
 * Represents different complexity levels of vectors.
 */
enum class ComplexityLevel {
    SIMPLE,      // 1-5 paths
    MODERATE,    // 6-15 paths
    COMPLEX,     // 16-30 paths
    VERY_COMPLEX // 30+ paths
}

/**
 * Represents how colors should be matched when filtering.
 */
enum class ColorMatchMode {
    ANY,     // Match vectors containing any of the selected colors
    ALL      // Match vectors containing all of the selected colors
} 