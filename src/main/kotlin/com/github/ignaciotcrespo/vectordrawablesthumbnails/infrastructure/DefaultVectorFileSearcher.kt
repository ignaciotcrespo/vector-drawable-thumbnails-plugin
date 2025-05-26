package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.File

/**
 * Default implementation of VectorFileSearcher.
 * Follows the Single Responsibility Principle by focusing only on file searching logic.
 */
class DefaultVectorFileSearcher : VectorFileSearcher {
    
    override fun searchVectorFiles(project: Project): Observable<ValidFile> {
        return Observable.create { emitter: ObservableEmitter<ValidFile> ->
            try {
                println("Starting vector file search for project: ${project.name}")
                val modules = ModuleManager.getInstance(project).modules
                println("Found ${modules.size} modules")
                if (modules.isNotEmpty()) {
                    val allExcludedRoots: MutableList<VirtualFile> = ArrayList()
                    for (module in modules) {
                        val excludedRoots = ModuleRootManager.getInstance(module).excludeRoots
                        allExcludedRoots.addAll(listOf(*excludedRoots))
                    }
                    val projectRootFolder = modules[0].project.basePath
                    println("Project root folder: $projectRootFolder")
                    if (projectRootFolder != null) {
                        val file1 = File(projectRootFolder)
                        searchFiles(emitter, file1, projectRootFolder, allExcludedRoots)
                    }
                }
            } finally {
                println("Vector file search completed")
                emitter.onComplete()
            }
        }
    }

    private fun searchFiles(
        emitter: ObservableEmitter<ValidFile>,
        folder: File,
        projectRootFolder: String,
        excludedRoots: List<VirtualFile>
    ) {
        val files = folder.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    if (shouldSkipDirectory(f)) {
                        continue
                    }
                    val fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f)
                    var isExcluded = false
                    for (excluded in excludedRoots) {
                        if (excluded == fVirtual) {
                            isExcluded = true
                            break
                        }
                    }
                    if (!isExcluded) {
                        searchFiles(emitter, f, projectRootFolder, excludedRoots)
                    }
                } else if (f.toString().endsWith(".xml")) {
                    println("Found XML file: ${f.absolutePath}")
                    emitter.onNext(ValidFile(f, projectRootFolder))
                }
            }
        }
    }

    private fun shouldSkipDirectory(directory: File): Boolean {
        return when {
            ".gradle" == directory.name -> true
            ".idea" == directory.name -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("generated") -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("intermediates") -> true
            else -> false
        }
    }
} 