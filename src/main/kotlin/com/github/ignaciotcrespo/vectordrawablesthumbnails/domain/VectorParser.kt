package com.github.ignaciotcrespo.vectordrawablesthumbnails.domain

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import io.reactivex.Observable

/**
 * Interface for parsing vector files into VectorItem objects.
 * Follows the Single Responsibility Principle by focusing only on vector parsing.
 */
interface VectorParser {
    fun parseVectorFile(validFile: ValidFile): Observable<VectorItem>
} 