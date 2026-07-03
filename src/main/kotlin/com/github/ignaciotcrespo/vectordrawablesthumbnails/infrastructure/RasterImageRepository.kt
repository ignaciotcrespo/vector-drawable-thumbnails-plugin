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
 * Repository for raster/bitmap image formats (PNG, JPG, JPEG, WEBP, GIF, BMP).
 * Follows the same pattern as VectorDrawableRepository and SvgRepository.
 */
class RasterImageRepository(
    private val fileSearcher: VectorFileSearcher,
    private val parser: VectorParser
) : VectorRepository {

    override fun loadVectors(project: Project): Observable<VectorItem> {
        return fileSearcher.searchVectorFiles(project, FileType.RASTER_TYPES)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.newThread())
            .flatMap { validFile -> parser.parseVectorFile(validFile) }
    }

    override fun getFileType(): String = "Raster Image"
}
