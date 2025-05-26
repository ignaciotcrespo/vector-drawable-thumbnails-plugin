package com.github.ignaciotcrespo.vectordrawablesthumbnails.model

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.ComplexityLevel
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.UsageStatus

/**
 * Analytics data for a vector drawable.
 * Provides insights into performance, usage, and optimization opportunities.
 */
data class VectorAnalytics(
    val complexityScore: Int,
    val complexityLevel: ComplexityLevel,
    val pathCount: Int,
    val estimatedRenderTime: Long, // in microseconds
    val optimizationSuggestions: List<OptimizationSuggestion>,
    val usageCount: Int,
    val usageStatus: UsageStatus,
    val tags: List<String> = emptyList(),
    val hasAnimations: Boolean = false,
    val colorCount: Int = 1,
    val aspectRatio: Double
) {
    /**
     * Computed property that returns true if there are optimization suggestions available.
     */
    val hasOptimizationSuggestions: Boolean
        get() = optimizationSuggestions.isNotEmpty()
}

/**
 * Represents an optimization suggestion for a vector.
 */
data class OptimizationSuggestion(
    val type: OptimizationType,
    val description: String,
    val potentialSavings: String, // e.g., "15% file size reduction"
    val priority: Priority
)

/**
 * Types of optimizations that can be applied to vectors.
 */
enum class OptimizationType {
    REMOVE_UNUSED_PATHS,
    SIMPLIFY_CURVES,
    MERGE_PATHS,
    REDUCE_PRECISION,
    REMOVE_REDUNDANT_GROUPS,
    OPTIMIZE_COLORS
}

/**
 * Priority levels for optimization suggestions.
 */
enum class Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
} 