package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.application.VectorSorterFactory
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortCriteria
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.SortDirection
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorSorter

/**
 * Default implementation of VectorSorterFactory.
 * Follows the Open/Closed Principle by allowing new sorters to be added without modification.
 */
class DefaultVectorSorterFactory : VectorSorterFactory {
    
    override fun createSorter(criteria: SortCriteria, direction: SortDirection): VectorSorter {
        return ConfigurableVectorSorter(criteria, direction)
    }
} 