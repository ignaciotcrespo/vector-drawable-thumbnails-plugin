package com.github.ignaciotcrespo.vectordrawablesthumbnails.parser

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.VectorItem
import io.reactivex.Observable

interface IVectorDrawableParser {
    fun parseVector(validFile: ValidFile): Observable<VectorItem?>
}
