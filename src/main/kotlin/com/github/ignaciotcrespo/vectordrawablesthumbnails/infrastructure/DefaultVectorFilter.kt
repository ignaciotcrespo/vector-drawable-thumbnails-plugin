package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFilter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Default implementation of VectorFilter.
 * Follows the Single Responsibility Principle by focusing only on filtering logic.
 */
class DefaultVectorFilter : VectorFilter {
    
    override fun filter(items: List<VectorItem>, filterText: String?): List<VectorItem> {
        return when {
            filterText.isNullOrBlank() -> items
            else -> items.filter { item ->
                item.name.lowercase().contains(filterText.lowercase())
            }
        }
    }
} 