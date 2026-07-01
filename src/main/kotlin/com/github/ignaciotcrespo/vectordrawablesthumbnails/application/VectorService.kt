package com.github.ignaciotcrespo.vectordrawablesthumbnails.application

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.FileType
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Service layer that orchestrates vector operations across multiple repositories.
 * Follows the Single Responsibility Principle by focusing on business logic coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 * Follows the Open/Closed Principle - new formats require only adding new repositories.
 * Enhanced with caching for better performance.
 */
class VectorService(
    private val repositories: List<VectorRepository>,
    private val filter: VectorFilter,
    private val sorterFactory: VectorSorterFactory
) {
    
    private val stateSubject = PublishSubject.create<VectorServiceState>()
    private var currentSortCriteria = SortCriteria.BY_NAME
    private var currentSortDirection = SortDirection.ASC
    private var currentFilterText: String? = null
    private var currentAdvancedFilter: FilterCriteria = FilterCriteria()

    // Storage for all loaded vectors from all repositories
    private val allVectors = mutableListOf<VectorItem>()
    private val vectorsMap = mutableMapOf<String, VectorItem>()

    // Cache for filtered and sorted results
    private var cachedResults: List<VectorItem>? = null
    private var cacheKey: String = ""

    val stateObservable: Observable<VectorServiceState> = stateSubject
    
    /**
     * Loads vectors from all enabled repositories based on UI selection.
     * @param project The IntelliJ project to load vectors from
     * @param enabledRepositories List of repositories to load from (subset of all repositories)
     * @return Observable stream of loaded vector items from all enabled repositories
     */
    fun loadVectors(
        project: Project,
        enabledRepositories: List<VectorRepository>
    ): Observable<VectorItem> {
        stateSubject.onNext(VectorServiceState.Loading)
        clearVectors()
        clearCache() // Clear cache when loading new vectors

        // Merge observables from all enabled repositories
        val observables = enabledRepositories.map { repo ->
            repo.loadVectors(project)
                .doOnNext { vectorItem -> addVector(vectorItem) }
        }

        return Observable.merge(observables)
            .doOnComplete { stateSubject.onNext(VectorServiceState.Loaded) }
            .doOnError { stateSubject.onNext(VectorServiceState.Error(it)) }
    }
    
    fun getFilteredAndSortedVectors(): List<VectorItem> {
        val newCacheKey = generateCacheKey()

        // Return cached results if nothing changed
        if (newCacheKey == cacheKey && cachedResults != null) {
            return cachedResults!!
        }

        // Snapshot to avoid ConcurrentModificationException while
        // background loading is still adding items.
        val snapshot = synchronized(allVectors) { allVectors.toList() }

        // Apply both text filter and advanced filter
        val textFiltered = if (currentFilterText.isNullOrBlank()) {
            snapshot
        } else {
            filter.filter(snapshot, currentFilterText)
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
        return synchronized(allVectors) { allVectors.toList() }
    }

    fun getAvailableRepositories(): List<VectorRepository> {
        return repositories
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
        val key = generateVectorKey(vector)
        val existingVector = vectorsMap[key]

        if (existingVector != null) {
            val updatedVector = existingVector.copy(analytics = analytics)

            // Update in both storage structures
            synchronized(this) {
                val index = allVectors.indexOf(existingVector)
                if (index >= 0) {
                    allVectors[index] = updatedVector
                    vectorsMap[key] = updatedVector
                }
            }
        }
        clearCache() // Clear cache since vector data changed
    }
    
    fun getCurrentSortCriteria(): SortCriteria = currentSortCriteria
    fun getCurrentSortDirection(): SortDirection = currentSortDirection
    
    private fun clearVectors() {
        synchronized(allVectors) {
            allVectors.clear()
            vectorsMap.clear()
        }
    }

    private fun addVector(vectorItem: VectorItem) {
        val key = generateVectorKey(vectorItem)
        synchronized(allVectors) {
            allVectors.add(vectorItem)
            vectorsMap[key] = vectorItem
        }
    }

    private fun generateVectorKey(vector: VectorItem): String {
        return "${vector.name}:${vector.validFile.file.path}"
    }

    private fun generateCacheKey(): String {
        return "${currentFilterText}:${currentAdvancedFilter.hashCode()}:${currentSortCriteria}:${currentSortDirection}:${allVectors.size}"
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