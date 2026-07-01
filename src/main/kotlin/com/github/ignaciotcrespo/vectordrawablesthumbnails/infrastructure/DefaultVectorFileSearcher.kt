package com.github.ignaciotcrespo.vectordrawablesthumbnails.infrastructure

import com.github.ignaciotcrespo.vectordrawablesthumbnails.domain.VectorFileSearcher
import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.FileType
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
import java.nio.file.Files

/**
 * Default implementation of VectorFileSearcher.
 * Follows the Single Responsibility Principle by focusing only on file searching logic.
 * Enhanced with progress reporting and cancellation support.
 */
class DefaultVectorFileSearcher : VectorFileSearcher {
    
    override fun searchVectorFiles(
        project: Project,
        fileTypes: Set<FileType>
    ): Observable<ValidFile> {
        return Observable.create { emitter: ObservableEmitter<ValidFile> ->
            try {
                val progressIndicator = ProgressManager.getInstance().progressIndicator
                val searchType = if (fileTypes.isEmpty()) {
                    "Scanning for files..."
                } else {
                    val typeNames = fileTypes.joinToString(" and ") { it.displayName }
                    "Scanning for $typeNames files..."
                }
                progressIndicator?.text = searchType

                val modules = ModuleManager.getInstance(project).modules
                val allExcludedRoots: MutableList<VirtualFile> = ArrayList()

                // Collect excluded roots from modules if they exist
                if (modules.isNotEmpty()) {
                    for (module in modules) {
                        val excludedRoots = ModuleRootManager.getInstance(module).excludeRoots
                        allExcludedRoots.addAll(listOf(*excludedRoots))
                    }
                }

                // Always search in project base path, even if there are no modules
                // This ensures compatibility with all JetBrains IDEs (WebStorm, PyCharm, etc.)
                val projectRootFolder = project.basePath
                if (projectRootFolder != null) {
                    val file1 = File(projectRootFolder)
                    searchFiles(
                        emitter,
                        file1,
                        projectRootFolder,
                        allExcludedRoots,
                        progressIndicator,
                        fileTypes
                    )
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
        fileTypes: Set<FileType>
    ) {
        // Check for cancellation
        progressIndicator?.checkCanceled()

        val files = folder.listFiles()
        if (files != null) {
            progressIndicator?.text2 = "Scanning: ${folder.name}"

            for (f in files) {
                // Check for cancellation frequently
                progressIndicator?.checkCanceled()

                // Skip symlinks entirely to avoid cycles and expensive traversal
                // (e.g. iOS xcframework directories full of symlinks).
                if (Files.isSymbolicLink(f.toPath())) {
                    continue
                }

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
                            fileTypes
                        )
                    }
                } else {
                    // Check if file matches any of the requested file types
                    if (fileTypes.any { it.matches(f.name) }) {
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
            "Pods" == directory.name -> true
            "build" == directory.name -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("generated") -> true
            directory.absolutePath.contains("build") && directory.absolutePath.contains("intermediates") -> true
            else -> false
        }
    }
} 