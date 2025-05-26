package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem

/**
 * Interface for sorting vector items.
 * Follows the Single Responsibility Principle by focusing only on sorting logic.
 * Follows the Open/Closed Principle by allowing new sorting strategies to be added.
 */
interface VectorSorter {
    fun sort(items: List<VectorItem>): List<VectorItem>
}

/**
 * Enum representing different sorting criteria.
 */
enum class SortCriteria {
    BY_NAME,
    BY_WIDTH,
    BY_HEIGHT,
    BY_AREA,
    BY_FILE_SIZE
}

/**
 * Enum representing sorting direction.
 */
enum class SortDirection {
    ASC,
    DESC
} 