package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.File

class ProjectVectorFileProvider : VectorFileProvider {

    override fun getValidFilesObservable(project: Project): Observable<ValidFile> {
        return Observable.create { emitter ->
            try {
                val files = searchFiles(project)
                for (file in files) {
                    emitter.onNext(file)
                }
                emitter.onComplete()
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    private fun searchFiles(project: Project): List<ValidFile> {
        val result = mutableListOf<ValidFile>()
        val modules = ModuleManager.getInstance(project).modules
        for (module in modules) {
            val roots = ModuleRootManager.getInstance(module).sourceRoots
            for (root in roots) {
                if (root.isDirectory && !isExcluded(root.path)) {
                    searchFilesRecursively(project, root, result)
                }
            }
        }
        return result
    }

    private fun searchFilesRecursively(project: Project, directory: VirtualFile, result: MutableList<ValidFile>) {
        val children = directory.children
        for (child in children) {
            if (child.isDirectory && !isExcluded(child.path)) {
                searchFilesRecursively(project, child, result)
            } else if (child.extension == "xml") {
                val file = File(child.path)
                if (file.readText().contains("<vector")) {
                    val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
                    if (virtualFile != null) {
                        result.add(ValidFile(project, virtualFile))
                    }
                }
            }
        }
    }

    private fun isExcluded(path: String): Boolean {
        return path.contains(".gradle") ||
                path.contains(".idea") ||
                path.contains("build/generated") ||
                path.contains("build/intermediates")
    }
}
