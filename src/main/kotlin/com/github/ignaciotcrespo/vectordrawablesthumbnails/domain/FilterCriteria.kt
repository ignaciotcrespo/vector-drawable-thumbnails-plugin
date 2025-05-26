package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

/**
 * Comprehensive filter criteria for vector items.
 * Supports multiple filtering dimensions for professional use.
 */
data class FilterCriteria(
    val text: String? = null,
    val sizeRange: IntRange? = null,
    val complexityRange: IntRange? = null,
    val fileSizeRange: LongRange? = null,
    val tags: List<String> = emptyList(),
    val usageStatus: UsageStatus? = null,
    val hasAnimations: Boolean? = null
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