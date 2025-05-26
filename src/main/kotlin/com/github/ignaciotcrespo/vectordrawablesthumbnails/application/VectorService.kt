package com.github.ignaciotcrespo.vectordrawablesthumbnails.application

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Service layer that orchestrates vector operations.
 * Follows the Single Responsibility Principle by focusing on business logic coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 * Enhanced with caching for better performance.
 */
class VectorService(
    private val repository: VectorRepository,
    private val filter: VectorFilter,
    private val sorterFactory: VectorSorterFactory
) {
    
    private val stateSubject = PublishSubject.create<VectorServiceState>()
    private var currentSortCriteria = SortCriteria.BY_NAME
    private var currentSortDirection = SortDirection.ASC
    private var currentFilterText: String? = null
    private var currentAdvancedFilter: FilterCriteria = FilterCriteria()
    
    // Cache for filtered and sorted results
    private var cachedResults: List<VectorItem>? = null
    private var cacheKey: String = ""
    
    val stateObservable: Observable<VectorServiceState> = stateSubject
    
    fun loadVectors(project: Project): Observable<VectorItem> {
        stateSubject.onNext(VectorServiceState.Loading)
        repository.clearVectors()
        clearCache() // Clear cache when loading new vectors
        
        return repository.loadVectors(project)
            .doOnComplete { stateSubject.onNext(VectorServiceState.Loaded) }
            .doOnError { stateSubject.onNext(VectorServiceState.Error(it)) }
    }
    
    fun getFilteredAndSortedVectors(): List<VectorItem> {
        val newCacheKey = generateCacheKey()
        
        // Return cached results if nothing changed
        if (newCacheKey == cacheKey && cachedResults != null) {
            return cachedResults!!
        }
        
        val allVectors = repository.getVectors()
        
        // Apply both text filter and advanced filter
        val textFiltered = if (currentFilterText.isNullOrBlank()) {
            allVectors
        } else {
            filter.filter(allVectors, currentFilterText)
        }
        
        val advancedFiltered = filter.filter(textFiltered, currentAdvancedFilter)
        val sorter = sorterFactory.createSorter(currentSortCriteria, currentSortDirection)
        val result = sorter.sort(advancedFiltered)
        
        // Cache the result
        cachedResults = result
        cacheKey = newCacheKey
        
        return result
    }
    
    fun getAllVectors(): List<VectorItem> {
        return repository.getVectors()
    }
    
    fun updateFilter(filterText: String?) {
        if (currentFilterText != filterText) {
            currentFilterText = filterText
            clearCache()
        }
    }
    
    fun updateAdvancedFilter(criteria: FilterCriteria) {
        if (currentAdvancedFilter != criteria) {
            currentAdvancedFilter = criteria
            clearCache()
        }
    }
    
    fun updateSort(criteria: SortCriteria, direction: SortDirection) {
        if (currentSortCriteria != criteria || currentSortDirection != direction) {
            currentSortCriteria = criteria
            currentSortDirection = direction
            clearCache()
        }
    }
    
    fun updateVectorAnalytics(vector: VectorItem, analytics: VectorAnalytics) {
        repository.updateVectorAnalytics(vector, analytics)
        clearCache() // Clear cache since vector data changed
    }
    
    fun getCurrentSortCriteria(): SortCriteria = currentSortCriteria
    fun getCurrentSortDirection(): SortDirection = currentSortDirection
    
    private fun generateCacheKey(): String {
        return "${currentFilterText}:${currentAdvancedFilter.hashCode()}:${currentSortCriteria}:${currentSortDirection}:${repository.getVectors().size}"
    }
    
    private fun clearCache() {
        cachedResults = null
        cacheKey = ""
    }
}

/**
 * Represents the different states of the vector service.
 */
sealed class VectorServiceState {
    object Loading : VectorServiceState()
    object Loaded : VectorServiceState()
    data class Error(val throwable: Throwable) : VectorServiceState()
}

/**
 * Factory for creating VectorSorter instances.
 * Follows the Open/Closed Principle by allowing new sorters to be added.
 */
interface VectorSorterFactory {
    fun createSorter(criteria: SortCriteria, direction: SortDirection): VectorSorter
} 