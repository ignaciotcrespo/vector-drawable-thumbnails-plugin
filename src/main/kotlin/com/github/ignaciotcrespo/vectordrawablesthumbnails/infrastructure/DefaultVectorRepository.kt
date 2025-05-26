package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorRepository
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Default implementation of VectorRepository.
 * Follows the Single Responsibility Principle by focusing only on data management.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
class DefaultVectorRepository(
    private val fileSearcher: VectorFileSearcher,
    private val parser: VectorParser
) : VectorRepository {
    
    private val vectors = mutableListOf<VectorItem>()
    
    override fun loadVectors(project: Project): Observable<VectorItem> {
        return fileSearcher.searchVectorFiles(project)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .flatMap { validFile -> parser.parseVectorFile(validFile) }
            .doOnNext { vectorItem -> addVector(vectorItem) }
    }
    
    override fun getVectors(): List<VectorItem> {
        return vectors.toList()
    }
    
    override fun clearVectors() {
        vectors.clear()
    }
    
    override fun addVector(vectorItem: VectorItem) {
        vectors.add(vectorItem)
    }
} 