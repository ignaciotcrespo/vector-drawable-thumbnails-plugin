package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.project.Project
import io.reactivex.Observable

interface VectorFileProvider {
    fun getValidFilesObservable(project: Project): Observable<ValidFile>
}
