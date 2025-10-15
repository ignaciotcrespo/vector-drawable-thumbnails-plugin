package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorAnalytics
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable

/**
 * Repository interface for managing vector data operations for a specific file format.
 * Each implementation handles one specific vector format (e.g., VectorDrawable, SVG).
 * Follows the Single Responsibility Principle by focusing only on one file format.
 * Follows the Dependency Inversion Principle by providing an abstraction for data operations.
 * Follows the Open/Closed Principle - new formats = new implementations.
 */
interface VectorRepository {
    /**
     * Loads vectors from the project for this repository's file format.
     * @param project The IntelliJ project to load vectors from
     * @return Observable stream of loaded vector items
     */
    fun loadVectors(project: Project): Observable<VectorItem>

    /**
     * Returns the file format this repository handles.
     */
    fun getFileType(): String
} 