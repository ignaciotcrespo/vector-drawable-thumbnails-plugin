package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorRepository
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Default implementation of VectorRepository.
 * Thread-safe implementation using concurrent collections.
 * Follows the Single Responsibility Principle by focusing only on data management.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
class DefaultVectorRepository(
    private val fileSearcher: VectorFileSearcher,
    private val parser: VectorParser
) : VectorRepository {
    
    // Use thread-safe collections to prevent ConcurrentModificationException
    private val vectors = CopyOnWriteArrayList<VectorItem>()
    private val vectorsMap = ConcurrentHashMap<String, VectorItem>()
    
    override fun loadVectors(
        project: Project,
        includeVectorDrawable: Boolean,
        includeSvg: Boolean
    ): Observable<VectorItem> {
        return fileSearcher.searchVectorFiles(project, includeVectorDrawable, includeSvg)
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
        vectorsMap.clear()
    }
    
    override fun addVector(vectorItem: VectorItem) {
        val key = generateVectorKey(vectorItem)
        vectors.add(vectorItem)
        vectorsMap[key] = vectorItem
    }
    
    override fun updateVectorAnalytics(vector: VectorItem, analytics: VectorAnalytics) {
        val key = generateVectorKey(vector)
        val existingVector = vectorsMap[key]
        
        if (existingVector != null) {
            val updatedVector = existingVector.copy(analytics = analytics)
            
            // Update both collections atomically
            synchronized(this) {
                val index = vectors.indexOf(existingVector)
                if (index >= 0) {
                    vectors[index] = updatedVector
                    vectorsMap[key] = updatedVector
                }
            }
        }
    }
    
    private fun generateVectorKey(vector: VectorItem): String {
        return "${vector.name}:${vector.validFile.file.path}"
    }
} 