package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project

/**
 * Service for analyzing vector drawables and providing insights.
 * Follows the Single Responsibility Principle by focusing only on analytics.
 */
interface VectorAnalyticsService {
    
    /**
     * Analyzes a vector drawable and returns comprehensive analytics.
     */
    fun analyzeVector(vectorItem: VectorItem): VectorAnalytics
    
    /**
     * Analyzes usage of vectors across the project.
     */
    fun analyzeUsage(project: Project, vectors: List<VectorItem>): Map<VectorItem, UsageStatus>
    
    /**
     * Generates optimization suggestions for a vector.
     */
    fun generateOptimizationSuggestions(vectorItem: VectorItem): List<com.github.ignaciotcrespo.vectordrawablesthumbnails.model.OptimizationSuggestion>
    
    /**
     * Calculates complexity score based on vector structure.
     */
    fun calculateComplexityScore(vectorItem: VectorItem): Int
    
    /**
     * Estimates render time for a vector.
     */
    fun estimateRenderTime(vectorItem: VectorItem): Long
    
    /**
     * Extracts semantic tags from vector content.
     */
    fun extractTags(vectorItem: VectorItem): List<String>
} 