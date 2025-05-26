package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortDirection
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorSorter
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Configurable implementation of VectorSorter.
 * Follows the Single Responsibility Principle by focusing only on sorting logic.
 * Follows the Open/Closed Principle by allowing different sorting strategies.
 */
class ConfigurableVectorSorter(
    private val criteria: SortCriteria,
    private val direction: SortDirection
) : VectorSorter {
    
    override fun sort(items: List<VectorItem>): List<VectorItem> {
        val sortedItems = when (criteria) {
            SortCriteria.BY_NAME -> items.sortedBy { it.name }
            SortCriteria.BY_WIDTH -> items.sortedBy { it.viewportW }
            SortCriteria.BY_HEIGHT -> items.sortedBy { it.viewportH }
            SortCriteria.BY_AREA -> items.sortedBy { it.viewportW * it.viewportH }
            SortCriteria.BY_FILE_SIZE -> items.sortedBy { it.fileSize }
        }
        
        return when (direction) {
            SortDirection.ASC -> sortedItems
            SortDirection.DESC -> sortedItems.reversed()
        }
    }
} 