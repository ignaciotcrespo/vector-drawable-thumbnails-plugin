package com.github.ignaciotcrespo.vectordrawablesthumbnails.scanners

import com.github.ignaciotcrespo.vectordrawablesthumbnails.model.ValidFile
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.File

class ProjectFileScanner : IProjectFileScanner {

    override fun findXmlFiles(project: Project): Observable<ValidFile> {
        return Observable.create { emitter: ObservableEmitter<ValidFile> ->
            try {
                val modules = ModuleManager.getInstance(project).modules
                if (modules.isNotEmpty()) {
                    val allExcludedRoots: MutableList<VirtualFile> = ArrayList()
                    for (module in modules) {
                        val excludedRoots = ModuleRootManager.getInstance(module).excludeRoots
                        allExcludedRoots.addAll(excludedRoots)
                    }
                    val projectRootFolder = project.basePath
                    if (projectRootFolder != null) {
                        val file1 = File(projectRootFolder)
                        searchFiles(emitter, file1, projectRootFolder, allExcludedRoots)
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
        excludedRoots: List<VirtualFile>
    ) {
        val files = folder.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    if (".gradle" == f.name || ".idea" == f.name ||
                        (f.absolutePath.contains("build") && (f.absolutePath.contains("generated") || f.absolutePath.contains("intermediates")))
                    ) {
                        continue
                    }

                    val fVirtual = LocalFileSystem.getInstance().findFileByIoFile(f)
                    var isExcluded = false
                    if (fVirtual != null) { // Ensure fVirtual is not null before checking exclusion
                        for (excluded in excludedRoots) {
                            if (excluded == fVirtual) {
                                isExcluded = true
                                break
                            }
                        }
                    } else {
                        // Optionally handle cases where VirtualFile cannot be found, though for directory scanning this might be rare
                        // For now, if we can't find a virtual file, we'll assume it's not excluded by this mechanism.
                    }

                    if (!isExcluded) {
                        searchFiles(emitter, f, projectRootFolder, excludedRoots)
                    }
                } else if (f.toString().endsWith(".xml")) {
                    emitter.onNext(ValidFile(f, projectRootFolder))
                }
            }
        }
    }
}
