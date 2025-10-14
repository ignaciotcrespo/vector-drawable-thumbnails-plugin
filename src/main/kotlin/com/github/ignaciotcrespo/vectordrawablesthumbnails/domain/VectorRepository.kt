package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable

/**
 * Repository interface for managing vector data operations.
 * Follows the Single Responsibility Principle by focusing only on data management.
 * Follows the Dependency Inversion Principle by providing an abstraction for data operations.
 */
interface VectorRepository {
    fun loadVectors(
        project: Project,
        includeVectorDrawable: Boolean = true,
        includeSvg: Boolean = false
    ): Observable<VectorItem>
    fun getVectors(): List<VectorItem>
    fun clearVectors()
    fun addVector(vectorItem: VectorItem)
    fun updateVectorAnalytics(vector: VectorItem, analytics: VectorAnalytics)
} 