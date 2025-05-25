package com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.project.Project
import io.reactivex.Observable

interface IProjectFileScanner {
    fun findXmlFiles(project: Project): Observable<ValidFile>
}
