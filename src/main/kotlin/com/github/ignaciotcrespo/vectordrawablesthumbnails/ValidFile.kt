package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

class ValidFile(val file: File, val projectRootFolder: String) {
    val virtualFile: VirtualFile?
    val relativePath: String
        get() = file.toString().substring(projectRootFolder.length)

    init {
        virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
    }
}