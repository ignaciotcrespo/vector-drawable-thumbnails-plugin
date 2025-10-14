package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
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
 * Enhanced with progress reporting and cancellation support.
 */
class DefaultVectorFileSearcher : VectorFileSearcher {
    
    override fun searchVectorFiles(
        project: Project,
        includeVectorDrawable: Boolean,
        includeSvg: Boolean
    ): Observable<ValidFile> {
        return Observable.create { emitter: ObservableEmitter<ValidFile> ->
            try {
                val progressIndicator = ProgressManager.getInstance().progressIndicator
                val searchType = buildString {
                    val types = mutableListOf<String>()
                    if (includeVectorDrawable) types.add("vector drawable")
                    if (includeSvg) types.add("SVG")
                    append("Scanning for ")
                    append(types.joinToString(" and "))
                    append(" files...")
                }
                progressIndicator?.text = searchType

                val modules = ModuleManager.getInstance(project).modules
                if (modules.isNotEmpty()) {
                    val allExcludedRoots: MutableList<VirtualFile> = ArrayList()
                    for (module in modules) {
                        val excludedRoots = ModuleRootManager.getInstance(module).excludeRoots
                        allExcludedRoots.addAll(listOf(*excludedRoots))
                    }
                    val projectRootFolder = modules[0].project.basePath
                    if (projectRootFolder != null) {
                        val file1 = File(projectRootFolder)
                        searchFiles(
                            emitter,
                            file1,
                            projectRootFolder,
                            allExcludedRoots,
                            progressIndicator,
                            includeVectorDrawable,
                            includeSvg
                        )
                    }
                }
            } finally {
                emitter.onComplete()
            }
        }
    }

    private fun searchFiles(
        emitter: ObservableEmitter<ValidFile>,
        folder: File,
        projectRootFolder: String,
        excludedRoots: List<VirtualFile>,
        progressIndicator: ProgressIndicator? = null,
        includeVectorDrawable: Boolean = true,
        includeSvg: Boolean = false
    ) {
        // Check for cancellation
        progressIndicator?.checkCanceled()

        val files = folder.listFiles()
        if (files != null) {
            progressIndicator?.text2 = "Scanning: ${folder.name}"

            for (f in files) {
                // Check for cancellation frequently
                progressIndicator?.checkCanceled()

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
                        searchFiles(
                            emitter,
                            f,
                            projectRootFolder,
                            excludedRoots,
                            progressIndicator,
                            includeVectorDrawable,
                            includeSvg
                        )
                    }
                } else {
                    val fileName = f.toString()
                    val isXml = includeVectorDrawable && fileName.endsWith(".xml")
                    val isSvg = includeSvg && fileName.endsWith(".svg")
                    if (isXml || isSvg) {
                        emitter.onNext(ValidFile(f, projectRootFolder))
                    }
                }
            }
        }
    }

    private fun shouldSkipDirectory(directory: File): Boolean {
        return when {
            ".gradle" == directory.name -> true
            ".idea" == directory.name -> true
            ".git" == directory.name -> true
            "node_modules" == directory.name -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("generated") -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("intermediates") -> true
            else -> false
        }
    }
} 