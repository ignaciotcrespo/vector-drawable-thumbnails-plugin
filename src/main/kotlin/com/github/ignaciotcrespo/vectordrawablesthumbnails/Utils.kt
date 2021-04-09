package com.github.ignaciotcrespo.vectordrawablesthumbnails

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

object Utils {
    @Throws(IOException::class)
    fun read(input: InputStream?): String {
        BufferedReader(InputStreamReader(input)).use { buffer ->
            return buffer.lines().collect(Collectors.joining("\n"))
        }
    }

    fun openValidFile(project: Project, item: ValidFile) {
        val fileByIoFile = LocalFileSystem.getInstance().findFileByIoFile(item.file)
        if (fileByIoFile != null) {
            runInUiThread { FileEditorManager.getInstance(project).openFile(fileByIoFile, true, true) }
        }
    }

    fun runInUiThread(runnable: Runnable?) {
        ApplicationManager.getApplication().invokeLater(runnable!!, ModalityState.defaultModalityState())
    }
}