package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.project.Project
import io.reactivex.Observable

/**
 * Interface for searching vector files in a project.
 * Follows the Single Responsibility Principle by focusing only on file searching.
 */
interface VectorFileSearcher {
    fun searchVectorFiles(
        project: Project,
        includeVectorDrawable: Boolean = true,
        includeSvg: Boolean = false
    ): Observable<ValidFile>
} 