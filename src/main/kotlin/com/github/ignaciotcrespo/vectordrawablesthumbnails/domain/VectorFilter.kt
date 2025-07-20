package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Interface for filtering vector items.
 * Follows the Single Responsibility Principle by focusing only on filtering logic.
 * Enhanced to support comprehensive filtering criteria.
 */
interface VectorFilter {
    
    /**
     * Filters vectors using comprehensive criteria.
     */
    fun filter(items: List<VectorItem>, criteria: FilterCriteria): List<VectorItem>
    
    /**
     * Simple text-based filtering for backward compatibility.
     */
    fun filter(items: List<VectorItem>, filterText: String?): List<VectorItem> {
        return filter(items, FilterCriteria(text = filterText))
    }
} 