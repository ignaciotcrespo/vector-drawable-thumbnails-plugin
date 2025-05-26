package com.github.ignaciotcrespo.vectordrawablesthumbnails.application

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.*
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Service layer that orchestrates vector operations.
 * Follows the Single Responsibility Principle by focusing on business logic coordination.
 * Follows the Dependency Inversion Principle by depending on abstractions.
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
    
    val stateObservable: Observable<VectorServiceState> = stateSubject
    
    fun loadVectors(project: Project): Observable<VectorItem> {
        stateSubject.onNext(VectorServiceState.Loading)
        repository.clearVectors()
        
        return repository.loadVectors(project)
            .doOnComplete { stateSubject.onNext(VectorServiceState.Loaded) }
            .doOnError { stateSubject.onNext(VectorServiceState.Error(it)) }
    }
    
    fun getFilteredAndSortedVectors(): List<VectorItem> {
        val allVectors = repository.getVectors()
        val filteredVectors = filter.filter(allVectors, currentFilterText)
        val sorter = sorterFactory.createSorter(currentSortCriteria, currentSortDirection)
        return sorter.sort(filteredVectors)
    }
    
    fun updateFilter(filterText: String?) {
        currentFilterText = filterText
    }
    
    fun updateSort(criteria: SortCriteria, direction: SortDirection) {
        currentSortCriteria = criteria
        currentSortDirection = direction
    }
    
    fun getCurrentSortCriteria(): SortCriteria = currentSortCriteria
    fun getCurrentSortDirection(): SortDirection = currentSortDirection
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