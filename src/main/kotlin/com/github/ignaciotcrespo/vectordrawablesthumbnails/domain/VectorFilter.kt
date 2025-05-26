package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Interface for filtering vector items.
 * Follows the Single Responsibility Principle by focusing only on filtering logic.
 */
interface VectorFilter {
    fun filter(items: List<VectorItem>, filterText: String?): List<VectorItem>
} 