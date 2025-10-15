package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorParser
import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorRepository
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.FileType
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import com.intellij.openapi.project.Project
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Repository implementation for Android Vector Drawable XML files.
 * Follows the Single Responsibility Principle by handling only Vector Drawable format.
 * Follows the Dependency Inversion Principle by depending on abstractions.
 */
class VectorDrawableRepository(
    private val fileSearcher: VectorFileSearcher,
    private val parser: VectorParser
) : VectorRepository {

    override fun loadVectors(project: Project): Observable<VectorItem> {
        return fileSearcher.searchVectorFiles(project, setOf(FileType.VECTOR_DRAWABLE))
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .flatMap { validFile -> parser.parseVectorFile(validFile) }
    }

    override fun getFileType(): String = "Vector Drawable"
}
