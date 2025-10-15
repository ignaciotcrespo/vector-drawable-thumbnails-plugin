package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.FileType
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.project.Project
import io.reactivex.Observable

/**
 * Interface for searching vector files in a project.
 * Follows the Single Responsibility Principle by focusing only on file searching.
 * Follows the Open/Closed Principle by using FileType enum for extensibility.
 */
interface VectorFileSearcher {
    /**
     * Searches for vector files of the specified types in the project.
     * @param project The IntelliJ project to search in
     * @param fileTypes The set of file types to include in the search
     * @return Observable stream of valid files found
     */
    fun searchVectorFiles(
        project: Project,
        fileTypes: Set<FileType>
    ): Observable<ValidFile>
} 